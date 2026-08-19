package icu.icuqalt10.panlingre.worldtemplate;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Installs the bundled Panling world files before Minecraft reads a selected save.
 */
public final class WorldTemplateUpdater {
    public static final long TARGET_SEED = 202961L;

    private static final String TEMPLATE_DIRECTORY = "panlingre/world";
    private static final String VERSION_FILE = "panlingre_world_version.dat";
    private static final String MAP_INDEX_FILE = "idcounts.dat";

    private WorldTemplateUpdater() {
    }

    public static void updateIfNeeded(Path worldDirectory) throws IOException {
        Path world = worldDirectory.toAbsolutePath().normalize();
        Path oldLevelDat = world.resolve("level.dat");
        if (!Files.isRegularFile(oldLevelDat) || readSeed(oldLevelDat) != TARGET_SEED) {
            return;
        }

        Path template = FMLPaths.GAMEDIR.get().resolve(TEMPLATE_DIRECTORY).toAbsolutePath().normalize();
        if (!Files.isDirectory(template)) {
            PanlingRE.LOGGER.warn("Panling world template is missing: {}", template);
            return;
        }
        if (world.equals(template)) {
            PanlingRE.LOGGER.warn("Refusing to install the Panling world template into itself: {}", world);
            return;
        }

        updateIfNeeded(world, template, currentModVersion());
    }

    static void updateIfNeeded(Path worldDirectory, Path templateDirectory, String modVersion) throws IOException {
        Path world = worldDirectory.toAbsolutePath().normalize();
        Path template = templateDirectory.toAbsolutePath().normalize();
        Path oldLevelDat = world.resolve("level.dat");
        if (!Files.isRegularFile(oldLevelDat) || readSeed(oldLevelDat) != TARGET_SEED) {
            return;
        }

        Path templateLevelDat = template.resolve("level.dat");
        if (!Files.isRegularFile(templateLevelDat)) {
            throw new IOException("Panling world template has no level.dat: " + templateLevelDat);
        }

        Path targetData = world.resolve("data");
        Path versionFile = targetData.resolve(VERSION_FILE);
        if (modVersion.equals(readInstalledVersion(versionFile))) {
            return;
        }

        PanlingRE.LOGGER.info("Updating Panling world {} to mod version {}", world, modVersion);
        Files.createDirectories(world);

        int existingMapIndex = readMapIndex(targetData.resolve(MAP_INDEX_FILE));
        int templateMapIndex = readMapIndex(template.resolve("data").resolve(MAP_INDEX_FILE));
        int greatestTemplateMap = findGreatestMapId(template.resolve("data"));

        updateRegionFiles(template.resolve("region"), world.resolve("region"), world.resolve("poi"), true);
        updateRegionFiles(template.resolve("entities"), world.resolve("entities"), null, false);
        replaceTemplateDatapacks(template.resolve("datapacks"), world.resolve("datapacks"));
        mergeDataDirectory(template.resolve("data"), targetData);
        updateMapIndex(targetData.resolve(MAP_INDEX_FILE),
                Math.max(Math.max(existingMapIndex, templateMapIndex), greatestTemplateMap));

        copyRegularFile(templateLevelDat, world.resolve("level.dat"));
        writeInstalledVersion(versionFile, modVersion);
        PanlingRE.LOGGER.info("Finished updating Panling world {} to mod version {}", world, modVersion);
    }

    private static long readSeed(Path levelDat) throws IOException {
        CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
        CompoundTag data = root.getCompound("Data");
        CompoundTag worldGenSettings = data.getCompound("WorldGenSettings");
        if (worldGenSettings.contains("seed", 99)) {
            return worldGenSettings.getLong("seed");
        }
        // Worlds created before 1.16 stored their seed directly in Data.RandomSeed.
        return data.getLong("RandomSeed");
    }

    private static String currentModVersion() throws IOException {
        return ModList.get().getModContainerById(PanlingRE.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElseThrow(() -> new IOException("Cannot determine PanlingRE mod version"));
    }

    private static String readInstalledVersion(Path versionFile) {
        if (!Files.isRegularFile(versionFile)) {
            return "";
        }
        try {
            CompoundTag root = NbtIo.readCompressed(versionFile, NbtAccounter.unlimitedHeap());
            return root.getCompound("data").getString("Version");
        } catch (IOException exception) {
            PanlingRE.LOGGER.warn("Cannot read Panling world version marker {}; the template will be reinstalled",
                    versionFile, exception);
            return "";
        }
    }

    private static void writeInstalledVersion(Path versionFile, String version) throws IOException {
        CompoundTag data = new CompoundTag();
        data.putString("Version", version);
        data.putLong("Seed", TARGET_SEED);

        CompoundTag root = new CompoundTag();
        root.put("data", data);
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        writeCompressedAtomically(root, versionFile);
    }

    private static void updateRegionFiles(Path sourceDirectory, Path targetDirectory, Path poiDirectory,
                                          boolean clearMatchingPoi) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            return;
        }
        Files.createDirectories(targetDirectory);
        try (Stream<Path> files = Files.list(sourceDirectory)) {
            for (Path source : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mca"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList()) {
                Path target = targetDirectory.resolve(source.getFileName().toString());
                if (Files.size(source) == 0L) {
                    Files.deleteIfExists(target);
                } else {
                    copyRegularFile(source, target);
                }
                if (clearMatchingPoi && poiDirectory != null) {
                    Files.deleteIfExists(poiDirectory.resolve(source.getFileName().toString()));
                }
            }
        }
    }

    private static void replaceTemplateDatapacks(Path sourceDirectory, Path targetDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            return;
        }
        Files.createDirectories(targetDirectory);
        try (Stream<Path> entries = Files.list(sourceDirectory)) {
            for (Path source : entries.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList()) {
                rejectSymbolicLink(source);
                Path target = targetDirectory.resolve(source.getFileName().toString());
                deleteTree(target);
                if (Files.isDirectory(source)) {
                    copyTree(source, target);
                } else if (Files.isRegularFile(source)) {
                    copyRegularFile(source, target);
                }
            }
        }
    }

    private static void mergeDataDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            return;
        }
        Files.createDirectories(targetDirectory);
        Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                rejectSymbolicLink(directory);
                Files.createDirectories(targetDirectory.resolve(sourceDirectory.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                rejectSymbolicLink(file);
                String name = file.getFileName().toString();
                if (!name.equals(VERSION_FILE) && !name.equals(MAP_INDEX_FILE)) {
                    copyRegularFile(file, targetDirectory.resolve(sourceDirectory.relativize(file).toString()));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static int findGreatestMapId(Path dataDirectory) throws IOException {
        if (!Files.isDirectory(dataDirectory)) {
            return -1;
        }
        int greatest = -1;
        try (Stream<Path> files = Files.list(dataDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.startsWith("map_") && name.endsWith(".dat")) {
                    try {
                        greatest = Math.max(greatest, Integer.parseInt(name.substring(4, name.length() - 4)));
                    } catch (NumberFormatException ignored) {
                        // Not a vanilla map data filename.
                    }
                }
            }
        }
        return greatest;
    }

    private static int readMapIndex(Path indexFile) {
        if (!Files.isRegularFile(indexFile)) {
            return -1;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(indexFile, NbtAccounter.unlimitedHeap());
            return root.getCompound("data").getInt("map");
        } catch (IOException exception) {
            PanlingRE.LOGGER.warn("Cannot read map index {}; it will be rebuilt from the template map files",
                    indexFile, exception);
            return -1;
        }
    }

    private static void updateMapIndex(Path indexFile, int requiredIndex) throws IOException {
        if (requiredIndex < 0) {
            return;
        }
        CompoundTag root;
        if (Files.isRegularFile(indexFile)) {
            try {
                root = NbtIo.readCompressed(indexFile, NbtAccounter.unlimitedHeap());
            } catch (IOException exception) {
                root = new CompoundTag();
            }
        } else {
            root = new CompoundTag();
        }
        CompoundTag data = root.getCompound("data");
        data.putInt("map", Math.max(data.getInt("map"), requiredIndex));
        root.put("data", data);
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        writeCompressedAtomically(root, indexFile);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                rejectSymbolicLink(directory);
                Files.createDirectories(target.resolve(source.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                rejectSymbolicLink(file);
                copyRegularFile(file, target.resolve(source.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyRegularFile(Path source, Path target) throws IOException {
        rejectSymbolicLink(source);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void rejectSymbolicLink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Symbolic links are not allowed in the Panling world template: " + path);
        }
    }

    private static void writeCompressedAtomically(CompoundTag root, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            NbtIo.writeCompressed(root, temporary);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
