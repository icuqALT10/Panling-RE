package icu.icuqalt10.panlingre.instance;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Copies FULL, lit chunks without loading or placing the building in the live level. */
final class InstanceRegionTask {
    private record Chunk(ChunkPos pos, CompoundTag blocks, CompoundTag entities, CompoundTag poi) {}
    private final ServerLevel level;
    private final InstanceDefinition definition;
    private final int slot;
    private final CompletableFuture<List<Chunk>> prepared;
    private CompletableFuture<Void> written;

    InstanceRegionTask(ServerLevel level, InstanceDefinition definition, int slot) {
        this.level = level;
        this.definition = definition;
        this.slot = slot;
        ResourceLocation resource = definition.template().withPath(path -> "instance_regions/" + path.substring(path.lastIndexOf('/') + 1) + ".nbt");
        var source = level.getServer().getResourceManager().getResource(resource).orElseThrow(
                () -> new IllegalStateException("Missing baked instance " + resource));
        prepared = CompletableFuture.supplyAsync(() -> {
            try (var input = source.open()) {
                return relocate(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()), definition, slot);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }, Util.ioPool());
    }

    static boolean unloaded(ServerLevel level, InstanceDefinition definition, int slot) {
        int minX = ((int) definition.centerForSlot(slot).x - 256) >> 4;
        int minZ = ((int) definition.centerPos().z - definition.regionDepth() * 256) >> 4;
        var map = level.getChunkSource().chunkMap;
        for (int z = minZ; z < minZ + definition.regionDepth() * 32; z++) {
            for (int x = minX; x < minX + 32; x++) {
                long key = ChunkPos.asLong(x, z);
                if (map.updatingChunkMap.containsKey(key) || map.pendingUnloads.containsKey(key)
                        || level.entityManager.chunkLoadStatuses.containsKey(key)) return false;
            }
        }
        return true;
    }

    boolean blocked() {
        return written == null && prepared.isDone() && !unloaded(level, definition, slot);
    }

    boolean tick() {
        if (written != null) {
            if (!written.isDone()) return false;
            written.join(); // Propagate IO failure: an incomplete copy must never become READY.
            return true;
        }
        if (!prepared.isDone()) return false;
        List<Chunk> chunks = prepared.join();
        if (!unloaded(level, definition, slot)) return false;
        var map = level.getChunkSource().chunkMap;
        var entities = (EntityStorage) level.entityManager.permanentStorage;
        var poi = level.getPoiManager();
        List<CompletableFuture<?>> writes = new ArrayList<>(chunks.size() * 3);
        // Enqueue the whole copy on the server thread before any subsequent chunk load can read it.
        // IOWorker reads pending writes, so loads also see the new data before disk flushing completes.
        for (Chunk chunk : chunks) {
            entities.emptyChunks.remove(chunk.pos.toLong());
            for (int y = level.getMinSection(); y < level.getMaxSection(); y++) {
                long section = SectionPos.asLong(chunk.pos.x, y, chunk.pos.z);
                poi.storage.remove(section);
                poi.dirty.remove(section);
            }
            writes.add(map.write(chunk.pos, chunk.blocks));
            writes.add(entities.simpleRegionStorage.write(chunk.pos, chunk.entities));
            writes.add(poi.simpleRegionStorage.write(chunk.pos, chunk.poi));
        }
        written = CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenRunAsync(() -> {
            map.flushWorker();
            entities.simpleRegionStorage.synchronize(true).join();
            poi.simpleRegionStorage.synchronize(true).join();
        }, Util.ioPool());
        return false;
    }

    void finishIO() {
        if (written != null) written.join();
    }

    private static List<Chunk> relocate(CompoundTag root, InstanceDefinition definition, int slot) {
        int[] center = root.getIntArray("Center");
        // arena_version invalidates saved scenes; it is independent of the baked resource's version.
        if (root.getInt("Depth") != definition.regionDepth()
                || center.length != 3 || center[0] != definition.centerPos().x || center[2] != definition.centerPos().z) {
            throw new IllegalStateException("Baked instance layout does not match " + definition.template());
        }
        int dx = slot * definition.spacing();
        int minX = (center[0] - 256) >> 4;
        int minZ = (center[2] - definition.regionDepth() * 256) >> 4;
        Map<Long, CompoundTag> blocks = index(root.getList("Chunks", Tag.TAG_COMPOUND), "xPos", "zPos");
        Map<Long, CompoundTag> entities = index(root.getList("Entities", Tag.TAG_COMPOUND), "X", "Z");
        Map<Long, CompoundTag> pois = index(root.getList("Poi", Tag.TAG_COMPOUND), "X", "Z");
        BlockPos oldOrigin = BlockPos.of(root.getLong("Origin"));
        String oldTag = "panlingre.tomb." + oldOrigin.asLong() + ".";
        String newTag = "panlingre.tomb." + oldOrigin.offset(dx, 0, 0).asLong() + ".";
        List<Chunk> result = new ArrayList<>(1024 * definition.regionDepth());
        // Replace every chunk in the reserved regions, including air and absent entity/POI records.
        // Copying only the building footprint would leave data from the previous scene behind.
        for (int z = minZ; z < minZ + definition.regionDepth() * 32; z++) {
            for (int x = minX; x < minX + 32; x++) {
                long key = ChunkPos.asLong(x, z);
                CompoundTag chunk = blocks.get(key);
                if (chunk == null) chunk = root.getCompound("Empty").copy();
                var pos = new ChunkPos(x + (dx >> 4), z);
                chunk.putInt("xPos", pos.x);
                chunk.putInt("zPos", pos.z);
                chunk.putLong("LastUpdate", 0);
                chunk.putLong("InhabitedTime", 0);
                for (String field : List.of("block_entities", "block_ticks", "fluid_ticks"))
                    for (Tag tag : chunk.getList(field, Tag.TAG_COMPOUND)) shiftBlock((CompoundTag) tag, dx);
                CompoundTag entityChunk = entities.get(key);
                if (entityChunk != null) {
                    entityChunk = entityChunk.getCompound("Data");
                    entityChunk.putIntArray("Position", new int[]{pos.x, pos.z});
                    for (Tag tag : entityChunk.getList("Entities", Tag.TAG_COMPOUND)) shiftEntity((CompoundTag) tag, dx, oldTag, newTag);
                }
                CompoundTag poi = pois.get(key);
                if (poi != null) {
                    poi = poi.getCompound("Data");
                    var sections = poi.getCompound("Sections");
                    for (String section : sections.getAllKeys())
                        for (Tag tag : sections.getCompound(section).getList("Records", Tag.TAG_COMPOUND)) {
                            CompoundTag record = (CompoundTag) tag;
                            int[] p = record.getIntArray("pos");
                            if (p.length == 3) { p[0] += dx; record.putIntArray("pos", p); }
                        }
                }
                result.add(new Chunk(pos, chunk, entityChunk, poi));
            }
        }
        return result;
    }

    private static Map<Long, CompoundTag> index(ListTag entries, String x, String z) {
        Map<Long, CompoundTag> result = new HashMap<>();
        for (Tag entry : entries) {
            CompoundTag tag = (CompoundTag) entry;
            result.put(ChunkPos.asLong(tag.getInt(x), tag.getInt(z)), tag);
        }
        return result;
    }

    private static void shiftBlock(CompoundTag tag, int dx) {
        if (tag.contains("x")) tag.putInt("x", tag.getInt("x") + dx);
    }

    private static void shiftEntity(CompoundTag entity, int dx, String oldTag, String newTag) {
        ListTag pos = entity.getList("Pos", Tag.TAG_DOUBLE);
        pos.set(0, DoubleTag.valueOf(pos.getDouble(0) + dx));
        entity.putUUID("UUID", UUID.randomUUID());
        ListTag tags = entity.getList("Tags", Tag.TAG_STRING);
        for (int i = 0; i < tags.size(); i++) {
            String value = tags.getString(i);
            if (value.startsWith(oldTag)) tags.set(i, StringTag.valueOf(newTag + value.substring(oldTag.length())));
        }
        for (Tag passenger : entity.getList("Passengers", Tag.TAG_COMPOUND))
            shiftEntity((CompoundTag) passenger, dx, oldTag, newTag);
    }
}
