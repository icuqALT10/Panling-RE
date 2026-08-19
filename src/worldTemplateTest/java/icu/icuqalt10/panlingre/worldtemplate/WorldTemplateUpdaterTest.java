package icu.icuqalt10.panlingre.worldtemplate;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Plain-Java regression test; run with the Gradle worldTemplateTest task. */
public final class WorldTemplateUpdaterTest {
    private WorldTemplateUpdaterTest() {
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Path projectTemplate = Path.of("run", "panlingre", "world", "level.dat");
        if (Files.isRegularFile(projectTemplate)) {
            require(readSeed(projectTemplate) == WorldTemplateUpdater.TARGET_SEED,
                    "run/panlingre/world/level.dat does not use seed 202961");
        }
        Path root = Files.createTempDirectory("panlingre-world-template-test-");
        try {
            Path template = root.resolve("template");
            Path world = root.resolve("world");
            createLevelDat(template.resolve("level.dat"), WorldTemplateUpdater.TARGET_SEED);
            createLevelDat(world.resolve("level.dat"), WorldTemplateUpdater.TARGET_SEED);

            write(template.resolve("region/r.0.0.mca"), "new-region");
            write(template.resolve("region/r.1.0.mca"), "");
            write(world.resolve("region/r.0.0.mca"), "old-region");
            write(world.resolve("region/r.1.0.mca"), "old-cleared-region");
            write(world.resolve("region/r.9.9.mca"), "unrelated-region");
            write(world.resolve("poi/r.0.0.mca"), "stale-poi");
            write(world.resolve("poi/r.1.0.mca"), "stale-poi");

            write(template.resolve("entities/r.0.0.mca"), "new-entities");
            write(world.resolve("entities/r.0.0.mca"), "old-entities");
            write(template.resolve("datapacks/Panling/pack.mcmeta"), "new-pack");
            write(world.resolve("datapacks/Panling/stale.json"), "stale");
            write(world.resolve("datapacks/Other/keep.txt"), "keep-pack");

            write(template.resolve("data/map_300.dat"), "map-data");
            write(world.resolve("data/custom_mod.dat"), "keep-data");
            createMapIndex(world.resolve("data/idcounts.dat"), 450);

            WorldTemplateUpdater.updateIfNeeded(world, template, "test-1");
            require(read(world.resolve("region/r.0.0.mca")).equals("new-region"), "region was not replaced");
            require(!Files.exists(world.resolve("region/r.1.0.mca")), "zero-byte region did not clear target");
            require(read(world.resolve("region/r.9.9.mca")).equals("unrelated-region"), "unrelated region changed");
            require(!Files.exists(world.resolve("poi/r.0.0.mca")), "matching POI was not cleared");
            require(read(world.resolve("entities/r.0.0.mca")).equals("new-entities"), "entities were not replaced");
            require(!Files.exists(world.resolve("datapacks/Panling/stale.json")), "stale datapack file remained");
            require(read(world.resolve("datapacks/Other/keep.txt")).equals("keep-pack"), "other datapack changed");
            require(read(world.resolve("data/custom_mod.dat")).equals("keep-data"), "unrelated saved data changed");
            require(readMapIndex(world.resolve("data/idcounts.dat")) == 450, "higher target map index was lowered");
            require(readSeed(world.resolve("level.dat")) == WorldTemplateUpdater.TARGET_SEED, "level.dat was not copied");

            write(world.resolve("region/r.0.0.mca"), "changed-after-install");
            WorldTemplateUpdater.updateIfNeeded(world, template, "test-1");
            require(read(world.resolve("region/r.0.0.mca")).equals("changed-after-install"),
                    "matching version did not skip update");

            WorldTemplateUpdater.updateIfNeeded(world, template, "test-2");
            require(read(world.resolve("region/r.0.0.mca")).equals("new-region"),
                    "new version did not reinstall template");

            Path otherWorld = root.resolve("other-world");
            createLevelDat(otherWorld.resolve("level.dat"), 123L);
            WorldTemplateUpdater.updateIfNeeded(otherWorld, template, "test-1");
            require(!Files.exists(otherWorld.resolve("region")), "non-target seed was modified");

            Path legacyWorld = root.resolve("legacy-world");
            createLegacyLevelDat(legacyWorld.resolve("level.dat"), WorldTemplateUpdater.TARGET_SEED);
            WorldTemplateUpdater.updateIfNeeded(legacyWorld, template, "test-1");
            require(Files.isRegularFile(legacyWorld.resolve("region/r.0.0.mca")),
                    "legacy Data.RandomSeed world was not updated");

            String originalWorld = System.getenv("PANLINGRE_TEST_ORIGINAL_WORLD");
            if (originalWorld != null && !originalWorld.isBlank()) {
                Path importedWorld = root.resolve("imported-original-world");
                Files.createDirectories(importedWorld);
                Files.copy(Path.of(originalWorld).resolve("level.dat"), importedWorld.resolve("level.dat"));
                WorldTemplateUpdater.updateIfNeeded(importedWorld, template, "test-1");
                require(Files.isRegularFile(importedWorld.resolve("region/r.0.0.mca")),
                        "provided original world format was not recognized");
            }
            System.out.println("WorldTemplateUpdaterTest passed");
        } finally {
            deleteTree(root);
        }
    }

    private static void createLevelDat(Path path, long seed) throws IOException {
        CompoundTag worldGen = new CompoundTag();
        worldGen.putLong("seed", seed);
        CompoundTag data = new CompoundTag();
        data.put("WorldGenSettings", worldGen);
        CompoundTag root = new CompoundTag();
        root.put("Data", data);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(root, path);
    }

    private static long readSeed(Path path) throws IOException {
        CompoundTag data = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getCompound("Data");
        CompoundTag worldGenSettings = data.getCompound("WorldGenSettings");
        return worldGenSettings.contains("seed", 99)
                ? worldGenSettings.getLong("seed")
                : data.getLong("RandomSeed");
    }

    private static void createLegacyLevelDat(Path path, long seed) throws IOException {
        CompoundTag data = new CompoundTag();
        data.putLong("RandomSeed", seed);
        CompoundTag root = new CompoundTag();
        root.put("Data", data);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(root, path);
    }

    private static void createMapIndex(Path path, int value) throws IOException {
        CompoundTag data = new CompoundTag();
        data.putInt("map", value);
        CompoundTag root = new CompoundTag();
        root.put("data", data);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(root, path);
    }

    private static int readMapIndex(Path path) throws IOException {
        return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getCompound("data").getInt("map");
    }

    private static void write(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
