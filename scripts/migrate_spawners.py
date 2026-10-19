"""Export four-direction monsters and migrate their placed spawners in run/saves/world.

Requires nbtlib (pip install --target tmp/spawner-tools nbtlib).
Run --export first; --apply backs up all affected files and verifies the result.
Only marker spawners with the exact spawn_entity/direction/tier/monster mapping qualify.
"""
import argparse
import collections
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import struct
import sys
from datetime import datetime
import zlib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tmp/spawner-tools'))
import nbtlib as nbt

WORLD = ROOT / 'run/saves/world'
FUNCTIONS = WORLD / 'datapacks/盘灵：RE数据包/data/plre/function/monsters'
RESOURCES = ROOT / 'src/main/resources/data/panlingre/spawner'
WORLD_RESOURCES = FUNCTIONS.parents[2] / 'panlingre/spawner'
DIRECTIONS = ('east', 'south', 'west', 'north')


def plain(value):
    if isinstance(value, dict):
        return {str(k): plain(v) for k, v in value.items()}
    if isinstance(value, (nbt.List, nbt.ByteArray, nbt.IntArray, nbt.LongArray)):
        return [plain(v) for v in value]
    if isinstance(value, (nbt.Float, nbt.Double)):
        return float(value)
    if isinstance(value, str):
        return str(value)
    return int(value)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def parse_snbt(text):
    # Mojang accepts trailing commas; nbtlib does not. Leave quoted text untouched.
    text = re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|,\s*(?=[\]}])',
                  lambda match: '' if match.group().startswith(',') else match.group(), text)
    return nbt.parse_nbt(text)


def definitions():
    monsters = {}
    blocks = {}
    for direction in DIRECTIONS:
        for path in (FUNCTIONS / direction).glob('*/*.mcfunction'):
            text = path.read_text(encoding='utf-8-sig').replace('\\\n', '')
            summons = re.findall(r'^summon (\S+) ~ ~ ~ (\{.*\})$', text, re.M)
            if not summons:
                continue
            if len(summons) != 1:
                raise ValueError(f'Expected exactly one summon: {path}')
            entity_id, tag = summons[0]
            key = path.relative_to(FUNCTIONS).with_suffix('').as_posix()
            monsters[key] = [{'id': entity_id if ':' in entity_id else 'minecraft:' + entity_id,
                              'nbt': plain(parse_snbt(tag)), 'weight': 1}]
        for line in (FUNCTIONS / direction / 'spawner.mcfunction').read_text(encoding='utf8').splitlines():
            if not line.startswith('give '):
                continue
            tag = parse_snbt(line.split('minecraft:block_entity_data=', 1)[1].split(',minecraft:custom_name=', 1)[0])
            if 'spawner_data' in tag:
                key = str(tag['spawner_data']).removeprefix('panlingre:')
                blocks[key] = json.loads((RESOURCES / 'block' / (key + '.json')).read_text(encoding='utf8'))
                continue
            key = marker_key(tag['SpawnPotentials'][0]['data']['entity'])
            if key not in monsters:
                raise ValueError(f'Missing summon for {key}')
            blocks[key] = block_config(tag, key)
    assert len(monsters) == len(blocks) == 38
    return monsters, blocks


def marker_key(entity):
    tags = list(map(str, entity.get('Tags', [])))
    if str(entity.get('id', '')) != 'minecraft:marker' or 'spawn_entity' not in tags:
        return None
    if len(tags) != 4 or tags[0] != 'spawn_entity' or tags[1] not in DIRECTIONS:
        raise ValueError(f'Unrecognized marker: {entity}')
    return '/'.join(tags[1:])


def block_config(be, key):
    lo, hi = int(be['MinSpawnDelay']), int(be['MaxSpawnDelay'])
    return {'spawn_count': int(be['SpawnCount']),
            'spawn_speed': lo if lo == hi else {'min': lo, 'max': hi},
            'spawn_monster': 'panlingre:' + key}


def target_key(be):
    if str(be.get('id', '')) not in ('minecraft:mob_spawner', 'minecraft:spawner'):
        return None
    entities = [p['data']['entity'] for p in be.get('SpawnPotentials', [])]
    current = be.get('SpawnData', {}).get('entity')
    if not entities and current is not None:
        entities = [current]
    keys = [marker_key(e) for e in entities]
    if not any(keys):
        return None
    if len(keys) != 1 or keys[0] is None or current is not None and marker_key(current) != keys[0]:
        raise ValueError(f'Mixed or unrecognized spawn potentials: {be}')
    assert int(be.get('RequiredPlayerRange', 18)) == 18
    return keys[0]


def chunks(raw):
    for slot in range(1024):
        location = int.from_bytes(raw[slot * 4:slot * 4 + 4], 'big')
        if not location:
            continue
        offset = (location >> 8) * 4096
        length = int.from_bytes(raw[offset:offset + 4], 'big')
        kind = raw[offset + 4]
        compressed = raw[offset + 5:offset + 4 + length]
        if kind == 2:
            data = zlib.decompress(compressed)
        elif kind == 1:
            data = gzip.decompress(compressed)
        elif kind == 3:
            data = compressed
        else:
            raise ValueError(f'Unsupported region compression: {kind}')
        yield slot, location, data


def replace_block(chunk, be):
    x, y, z = (int(be[k]) for k in ('x', 'y', 'z'))
    section = next(s for s in chunk['sections'] if int(s['Y']) == y // 16)
    states = section['block_states']
    palette = states['palette']
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    packed = [int(v) & ((1 << 64) - 1) for v in states.get('data', [])]
    indices = [(packed[i // per_long] >> ((i % per_long) * bits)) & ((1 << bits) - 1)
               for i in range(4096)] if packed else [0] * 4096
    index = (y & 15) * 256 + (z & 15) * 16 + (x & 15)
    assert str(palette[indices[index]]['Name']) == 'minecraft:spawner', (x, y, z)
    new_index = next((i for i, p in enumerate(palette) if str(p['Name']) == 'panlingre:spawner'), None)
    if new_index is None:
        new_index = len(palette)
        palette.append(nbt.Compound({'Name': nbt.String('panlingre:spawner')}))
    indices[index] = new_index
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    packed = [0] * ((4096 + per_long - 1) // per_long)
    for i, value in enumerate(indices):
        packed[i // per_long] |= value << ((i % per_long) * bits)
    states['data'] = nbt.LongArray([v if v < (1 << 63) else v - (1 << 64) for v in packed])


def expanded_states(section):
    states = section.get('block_states')
    if states is None:
        return None
    palette = [entry.snbt() for entry in states['palette']]
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    packed = states.get('data')
    if packed is None:
        return [palette[0]] * 4096
    return [palette[(int(packed[i // per_long]) >> ((i % per_long) * bits)) & ((1 << bits) - 1)]
            for i in range(4096)]


def verify_chunk(before, after, rows):
    old = nbt.File.parse(io.BytesIO(before))
    new = nbt.File.parse(io.BytesIO(after))
    old_entities = old.pop('block_entities')
    new_entities = new.pop('block_entities')
    expected_positions = {tuple(row['position']): row for row in rows}
    assert len(old_entities) == len(new_entities)
    for a, b in zip(old_entities, new_entities):
        position = tuple(int(a[k]) for k in ('x', 'y', 'z'))
        if position not in expected_positions:
            assert a.snbt() == b.snbt(), ('unrelated block entity changed', position)
        else:
            assert str(b['id']) == 'panlingre:spawner'
            assert str(b['spawner_data']) == 'panlingre:' + expected_positions[position]['block']
            for k in ('x', 'y', 'z', 'components', 'NeoForgeData', 'neoforge:attachments'):
                assert a.get(k, nbt.Compound()).snbt() == b.get(k, nbt.Compound()).snbt()
    assert len(old['sections']) == len(new['sections'])
    changed = set()
    for a, b in zip(old['sections'], new['sections']):
        left, right = expanded_states(a), expanded_states(b)
        if left is not None:
            assert right is not None
            for i, (old_state, new_state) in enumerate(zip(left, right)):
                if old_state == new_state:
                    continue
                pos = (int(old['xPos']) * 16 + (i & 15), int(a['Y']) * 16 + (i >> 8),
                       int(old['zPos']) * 16 + ((i >> 4) & 15))
                assert pos in expected_positions, ('unrelated block changed', pos)
                assert old_state == nbt.Compound({'Name':nbt.String('minecraft:spawner')}).snbt()
                assert new_state == nbt.Compound({'Name':nbt.String('panlingre:spawner')}).snbt()
                changed.add(pos)
        a.pop('block_states', None)
        b.pop('block_states', None)
    assert changed == set(expected_positions)
    assert old.snbt() == new.snbt(), 'Other chunk data changed'


def scan(blocks):
    plan = []
    for path in WORLD.rglob('r.*.*.mca'):
        if path.parent.name != 'region':
            continue
        raw = path.read_bytes()
        for slot, location, data in chunks(raw):
            if b'spawn_entity' not in data:
                continue
            chunk = nbt.File.parse(io.BytesIO(data))
            for be in chunk.get('block_entities', []):
                key = target_key(be)
                if key is None:
                    continue
                if key not in blocks:
                    raise ValueError(f'Unknown monster: {key}')
                config = block_config(be, key)
                block_key = key
                if blocks[key] != config:
                    block_key += f"_{int(be['SpawnCount'])}_{int(be['MinSpawnDelay'])}_{int(be['MaxSpawnDelay'])}"
                    blocks[block_key] = config
                plan.append({'file': path.relative_to(WORLD).as_posix(), 'slot': slot,
                             'position': [int(be[k]) for k in ('x', 'y', 'z')],
                             'block': block_key, 'old_nbt': be.snbt(),
                             'old_spawn_range': int(be.get('SpawnRange', 0))})
    return plan


def apply(plan, blocks, resume=None):
    # Hold the same session.lock Minecraft locks, so an open world is never edited offline.
    import msvcrt
    with (WORLD / 'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        backup = resume or ROOT / 'tmp' / ('spawner-backup-' + datetime.now().strftime('%Y%m%d-%H%M%S'))
        backup.mkdir(parents=True, exist_ok=resume is not None)
        targets = sorted({row['file'] for row in plan})
        functions = [FUNCTIONS / d / 'spawner.mcfunction' for d in DIRECTIONS]
        json_files = list(RESOURCES.rglob('*.json'))
        json_destinations = [WORLD_RESOURCES / path.relative_to(RESOURCES) for path in json_files]
        hashes = json.loads((backup / 'manifest.json').read_text(encoding='utf8'))['sha256'] if resume else {}
        for path in [] if resume else [WORLD / t for t in targets] + functions + [p for p in json_destinations if p.exists()]:
            rel = path.relative_to(WORLD)
            dest = backup / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, dest)
            hashes[rel.as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
            assert hashlib.sha256(dest.read_bytes()).hexdigest() == hashes[rel.as_posix()]
        if not resume:
            write_json(backup / 'manifest.json', {'world': str(WORLD), 'sha256': hashes, 'spawners': plan,
                        'added_files': [p.relative_to(WORLD).as_posix() for p in json_destinations if not p.exists()],
                        'game_time': int(nbt.load(WORLD / 'level.dat')['Data']['Time'])})
        now = json.loads((backup / 'manifest.json').read_text(encoding='utf8')).get(
            'game_time', int(nbt.load(WORLD / 'level.dat')['Data']['Time']))
        for relative in targets:
            path = WORLD / relative
            original = (backup / relative).read_bytes()
            assert hashlib.sha256(original).hexdigest() == hashes[relative]
            updated = bytearray(original)
            rows = [r for r in plan if r['file'] == relative]
            by_chunk = collections.defaultdict(list)
            for row in rows:
                by_chunk[row['slot']].append(row)
            expected = {}
            for slot, location, data in chunks(original):
                if slot not in by_chunk:
                    continue
                chunk = nbt.File.parse(io.BytesIO(data))
                for row in by_chunk[slot]:
                    be = next(b for b in chunk['block_entities'] if [int(b[k]) for k in ('x','y','z')] == row['position'])
                    assert be.snbt() == row['old_nbt']
                    replace_block(chunk, be)
                    delay = max(0, int(be.get('Delay', 0)))
                    speed = blocks[row['block']]['spawn_speed']
                    minimum = speed['min'] if isinstance(speed, dict) else speed
                    interval = max(minimum, delay)
                    for key in ('SpawnData', 'SpawnPotentials', 'SpawnCount', 'MinSpawnDelay', 'MaxSpawnDelay',
                                'SpawnRange', 'RequiredPlayerRange', 'MaxNearbyEntities', 'Delay'):
                        be.pop(key, None)
                    be['id'] = nbt.String('panlingre:spawner')
                    be['spawner_data'] = nbt.String('panlingre:' + row['block'])
                    be['LastSpawnTick'] = nbt.Long(now - (interval - delay))
                    be['SpawnDelay'] = nbt.Int(interval)
                stream = io.BytesIO()
                chunk.write(stream)
                expected[slot] = stream.getvalue()
                verify_chunk(data, expected[slot], by_chunk[slot])
                payload = zlib.compress(expected[slot])
                record = struct.pack('>I', len(payload) + 1) + b'\x02' + payload
                sectors = (len(record) + 4095) // 4096
                assert sectors <= 255
                if sectors <= (location & 255):
                    offset = (location >> 8) * 4096
                    updated[offset:offset+len(record)] = record
                else:
                    offset = len(updated)
                    assert offset % 4096 == 0
                    updated.extend(record + bytes(sectors * 4096 - len(record)))
                    updated[slot*4:slot*4+4] = ((offset // 4096 << 8) | sectors).to_bytes(4,'big')
            # Verify every chunk before replacing the region: untouched payloads must be byte-identical.
            old_chunks = {slot: data for slot, _, data in chunks(original)}
            for slot, _, data in chunks(updated):
                assert data == expected.get(slot, old_chunks[slot]), (relative, slot)
            staging = path.with_suffix('.mca.spawner-tmp')
            current = path.read_bytes()
            assert current == original or current == updated, ('World changed after backup', relative)
            if current != updated:
                staging.write_bytes(updated)
                os.replace(staging, path)
            assert path.read_bytes() == updated
        for source, destination in zip(json_files, json_destinations):
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
            assert destination.read_bytes() == source.read_bytes()
        for path in functions:
            original_text = (backup / path.relative_to(WORLD)).read_text(encoding='utf8')
            def replace(match):
                be = nbt.parse_nbt(match.group(1))
                key = marker_key(be['SpawnPotentials'][0]['data']['entity'])
                return 'give @s panlingre:spawner[minecraft:block_entity_data={id:"panlingre:spawner",spawner_data:"panlingre:' + key + '"},minecraft:custom_name='
            text, count = re.subn(r'give @s spawner\[minecraft:block_entity_data=(.*?),minecraft:custom_name=', replace, original_text)
            assert count > 0
            assert path.read_text(encoding='utf8') in (original_text, text), ('Function changed after backup', path)
            path.write_text(text, encoding='utf8', newline='\n')
        remaining = len(scan(blocks))
        write_json(backup / 'result.json', {'replaced': len(plan), 'regions': len(targets),
                    'by_definition': dict(collections.Counter(row['block'] for row in plan)),
                    'remaining_legacy_targets': remaining})
        assert remaining == 0, 'Legacy target spawners remain'
        print(json.dumps({'backup':str(backup), 'replaced':len(plan), 'regions':len(targets)}, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--export', action='store_true')
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--resume', type=Path, help='Resume a partially completed migration from its original backup')
    args = parser.parse_args()
    monsters, blocks = definitions()
    if args.resume:
        manifest = json.loads((args.resume / 'manifest.json').read_text(encoding='utf8'))
        plan = manifest['spawners']
        blocks.update({p.relative_to(RESOURCES / 'block').with_suffix('').as_posix(): json.loads(p.read_text(encoding='utf8'))
                       for p in (RESOURCES / 'block').rglob('*.json')})
    else:
        plan = scan(blocks)
    if args.export or args.apply:
        for folder, values in [('monster', monsters), ('block', blocks)]:
            for key, value in values.items():
                write_json(RESOURCES / folder / (key + '.json'), value)
    write_json(ROOT / 'tmp/spawner-migration-plan.json', plan)
    print(json.dumps({'monsters':len(monsters), 'block_definitions':len(blocks), 'targets':len(plan),
                      'regions':len(set(r['file'] for r in plan))}))
    if args.apply or args.resume:
        if not plan:
            raise ValueError('No matching legacy spawners to migrate')
        apply(plan, blocks, args.resume)


if __name__ == '__main__':
    main()
