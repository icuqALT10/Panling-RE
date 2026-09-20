import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
tmap={t["id"]:t for t in bb["textures"]}
use=collections.defaultdict(list)
for e in bb["elements"]:
    for f,v in e["faces"].items():
        use[str(v["texture"])].append((e["name"],f,v["uv"],e["from"],e["to"]))
for tid in ["19","8","24","16","89","90","91","20","21","23","17","18","25","0","22","1","2","3"]:
    t=tmap[tid]
    print(f'--- tex {tid} {t["name"]} {t["width"]}x{t["height"]} uv={t["uv_width"]}x{t["uv_height"]} uses={len(use[tid])}')
    for nm,f,uv,fr,to in use[tid][:4]:
        print("     ",repr(nm),f,uv,fr,to)
