package icu.icuqalt10.panlingre.block.spawner;

import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.item.other.SpawnerBoostItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class SpawnerTest {
    @GameTest(template = "empty")
    public static void rangesAndWeights(GameTestHelper h) throws Exception {
        var defaults = SpawnerConfig.parse(new CompoundTag());
        h.assertTrue(defaults.count().min() == 1 && defaults.speed().min() == 20, "Wrong defaults");
        h.assertTrue(defaults.maxCount().equals(new SpawnerConfig.SpawnLimit(9, 10, List.of("monster"))), "Legacy cap default changed");
        var config = SpawnerConfig.parse(TagParser.parseTag("{spawn_count:{min:1,max:3},spawn_speed:{min:2,max:5},spawn_monster:[{id:'minecraft:pig',weight:1},{id:'minecraft:cow',weight:3},{id:'minecraft:sheep',weight:0}]}"));
        var random = RandomSource.create(1234);
        int pigs = 0;
        boolean countMin = false, countMax = false, speedMin = false, speedMax = false;
        for (int i = 0; i < 20000; i++) {
            int count = config.count().sample(random), speed = config.speed().sample(random);
            h.assertTrue(count >= 1 && count <= 3 && speed >= 2 && speed <= 5, "Out of range");
            countMin |= count == 1; countMax |= count == 3; speedMin |= speed == 2; speedMax |= speed == 5;
            String id = config.choose(random).id().getPath();
            h.assertTrue(!id.equals("sheep"), "Selected a zero weight entry");
            if (id.equals("pig")) pigs++;
        }
        h.assertTrue(countMin && countMax && speedMin && speedMax, "Range endpoints were excluded");
        h.assertTrue(pigs > 4700 && pigs < 5300, "Incorrect weighted sampling: " + pigs);
        for (String invalid : List.of("{spawn_speed:0}", "{spawn_count:{min:3,max:1}}", "{spawn_count:1.5}",
                "{spawn_monster:[{id:'minecraft:pig',weight:-1}]}", "{spawn_monster:[{id:'minecraft:pig',weight:0}]}",
                "{spawn_max_count:10}", "{spawn_max_count:{limit:-1,distance:12,tags:['monster']}}",
                "{spawn_max_count:{limit:1.5,distance:12,tags:['monster']}}", "{spawn_max_count:{limit:10,distance:-1,tags:[]}}",
                "{spawn_max_count:{limit:10,distance:12,tags:[1,2]}}", "{spawn_max_count:{limit:10,distance:12}}")) {
            boolean rejected = false;
            try { SpawnerConfig.parse(TagParser.parseTag(invalid)); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Accepted invalid input: " + invalid);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void exportedDefinitionsKeepMobData(GameTestHelper h) {
        var config = SpawnerDataLoader.resolve(StringTag.valueOf("panlingre:spawner/block/east/common/skeleton.json"));
        h.assertTrue(config.count().min() == 1 && config.speed().min() == 200 && config.speed().max() == 800, "Wrong exported configuration");
        h.assertTrue(config.maxCount().equals(new SpawnerConfig.SpawnLimit(10, 12, List.of("monster"))), "Map cap was not installed");
        CompoundTag data = config.monsters().getFirst().nbt().copy();
        data.putString("id", config.monsters().getFirst().id().toString());
        var mob = (Mob) EntityType.loadEntityRecursive(data, h.getLevel(), entity -> entity);
        h.assertTrue(mob != null && mob.getMaxHealth() == 12 && mob.getHealth() == 12, "Health data lost");
        h.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET), "Helmet lost");
        h.assertTrue(mob.getMainHandItem().is(Items.BOW), "Bow lost");
        h.assertTrue(mob.getTags().containsAll(List.of("monster", "east", "common", "skeleton")), "Tags lost");
        CompoundTag saved = new CompoundTag(); mob.saveWithoutId(saved);
        h.assertTrue(saved.getList("ArmorDropChances", Tag.TAG_FLOAT).size() == 4
                && saved.getList("ArmorDropChances", Tag.TAG_FLOAT).getFloat(3) == 0, "JSON drop chances lost their NBT type");
        h.assertTrue(saved.getString("DeathLootTable").equals("plre:monsters/east/common/skeleton"), "Loot table lost");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void buriedSpawnTimingPersistenceAndCap(GameTestHelper h) throws Exception {
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos position = h.absolutePos(relative);
        h.setBlock(relative, ModBlocks.spawner.get());
        h.setBlock(relative.above(), Blocks.STONE);
        h.setBlock(relative.above(2), Blocks.STONE);
        h.setBlock(relative.above(3), Blocks.AIR);
        h.setBlock(relative.above(4), Blocks.AIR);
        MonsterSpawnerBlockEntity spawner = h.getBlockEntity(relative);
        CompoundTag source = TagParser.parseTag("{spawner_data:{spawn_count:2,spawn_speed:5,spawn_monster:[{id:'minecraft:pig',nbt:{NoAI:1b,NoGravity:1b,Tags:['monster','spawner_test']},weight:1}]}}");
        spawner.loadWithComponents(source, h.getLevel().registryAccess());
        tick(h, position, spawner);
        long initial = spawner.saveWithoutMetadata(h.getLevel().registryAccess()).getLong("LastSpawnTick");
        ServerPlayer player = player(h);
        player.moveTo(position.getX() + 2, position.getY() + 1, position.getZ() + 2);
        h.runAfterDelay(3, () -> {
            h.assertTrue(mobs(h, position).isEmpty(), "Spawned before the interval elapsed");
            CompoundTag saved = spawner.saveWithoutMetadata(h.getLevel().registryAccess());
            spawner.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(saved.getLong("LastSpawnTick") == initial && saved.getInt("SpawnDelay") == 5, "Timer did not persist");
        });
        h.runAfterDelay(7, () -> {
            var mobs = mobs(h, position);
            h.assertTrue(mobs.size() == 2, "Expected one batch of two, got " + mobs.size());
            h.assertTrue(mobs.stream().allMatch(m -> m.getY() == position.getY() + 3), "Did not climb to the first empty space");
        });
        h.runAfterDelay(32, () -> {
            h.assertTrue(mobs(h, position).size() == 9, "Nearby monster cap was not preserved");
            h.getLevel().players().remove(player);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void noPlayerAndWorldCeiling(GameTestHelper h) throws Exception {
        // Place this spawner far from the player used by the other test.
        BlockPos position = h.absolutePos(new BlockPos(1, 1, 1)).offset(0, 80, 0);
        var level = h.getLevel();
        level.setBlock(position, ModBlocks.spawner.get().defaultBlockState(), 3);
        var spawner = (MonsterSpawnerBlockEntity) level.getBlockEntity(position);
        CompoundTag data = TagParser.parseTag("{spawner_data:{spawn_speed:1,spawn_monster:[{id:'minecraft:pig',nbt:{Tags:['spawner_test']}}]}}");
        data.putLong("LastSpawnTick", level.getGameTime() - 10);
        spawner.loadWithComponents(data, level.registryAccess());
        tick(h, position, spawner);
        h.assertTrue(mobs(h, position).isEmpty(), "Spawned without a nearby player");
        h.assertTrue(spawner.saveWithoutMetadata(level.registryAccess()).getLong("LastSpawnTick") == data.getLong("LastSpawnTick"), "Failed spawn changed LastSpawnTick");
        BlockPos ceiling = new BlockPos(position.getX(), level.getMaxBuildHeight() - 1, position.getZ());
        level.setBlock(ceiling, ModBlocks.spawner.get().defaultBlockState(), 3);
        var top = (MonsterSpawnerBlockEntity) level.getBlockEntity(ceiling);
        top.loadWithComponents(data, level.registryAccess());
        ServerPlayer player = player(h);
        player.moveTo(ceiling.getX() + 1, ceiling.getY(), ceiling.getZ());
        tick(h, ceiling, top);
        h.assertTrue(top.saveWithoutMetadata(level.registryAccess()).getLong("LastSpawnTick") == data.getLong("LastSpawnTick"), "Spawned above the build height");
        level.players().remove(player); level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ceiling, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spawnSpaceUsesLoadedBodySize(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1)).above(40);
        // Maintenance cavity at +1, then a valid low cavity, a narrow shaft and open sky.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 11; y++) {
                    boolean wall = y == 0 || y == 2 || y == 4 || (y == 5 || y == 6) && (x != 0 || z != 0);
                    level.setBlock(pos.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(pos, ModBlocks.spawner.get().defaultBlockState(), 3);
        var spawner = (MonsterSpawnerBlockEntity) level.getBlockEntity(pos);
        var player = player(h);
        player.moveTo(pos.getX() + 3, pos.getY() + 3, pos.getZ());
        String[][] cases = {
                {"baby", "minecraft:zombie", "IsBaby:1b", "3"},
                {"spider", "minecraft:spider", "", "3"},
                {"adult", "minecraft:zombie", "IsBaby:0b", "5"},
                {"ravager", "minecraft:ravager", "", "7"},
                {"scaled", "minecraft:zombie", "attributes:[{id:'minecraft:generic.scale',base:2d}]", "7"}
        };
        try {
            for (String[] entry : cases) {
                String extra = entry[2].isEmpty() ? "" : "," + entry[2];
                CompoundTag data = TagParser.parseTag("{spawner_data:{spawn_speed:1,spawn_monster:[{id:'" + entry[1]
                        + "',nbt:{NoAI:1b,NoGravity:1b,Tags:['spawner_size_" + entry[0] + "']" + extra + "}}]}}");
                data.putLong("LastSpawnTick", level.getGameTime() - 10);
                spawner.loadWithComponents(data, level.registryAccess());
                tick(h, pos, spawner);
                var entities = level.getEntities((Entity) null, new AABB(pos).inflate(12),
                        entity -> entity.getTags().contains("spawner_size_" + entry[0]));
                h.assertTrue(entities.size() == 1, "Expected one " + entry[0]);
                Entity entity = entities.getFirst();
                h.assertTrue(entity.getY() == pos.getY() + Integer.parseInt(entry[3]),
                        entry[0] + " chose wrong height: " + (entity.getY() - pos.getY()));
                h.assertTrue(level.noBlockCollision(entity, entity.getBoundingBox()), entry[0] + " intersects terrain");
                entity.discard();
            }
        } finally {
            level.players().remove(player);
            for (BlockPos block : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 11, 1))) {
                level.setBlock(block, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "spawner_reload", timeoutTicks = 100)
    public static void reloadRefreshesLoadedReferences(GameTestHelper h) throws Exception {
        var loader = new SpawnerDataLoader();
        var monsterId = ResourceLocation.parse("panlingre:monster/test/nested");
        var blockId = ResourceLocation.parse("panlingre:block/test/nested");
        var files = new HashMap<ResourceLocation, JsonElement>();
        var resources = h.getLevel().getServer().getResourceManager();
        for (var resource : resources.listResources("spawner", id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = resource.getKey().getPath();
            try (var reader = resource.getValue().openAsReader()) {
                files.put(ResourceLocation.fromNamespaceAndPath(resource.getKey().getNamespace(), path.substring(8, path.length() - 5)), JsonParser.parseReader(reader));
            }
        }
        files.putAll(Map.of(monsterId, JsonParser.parseString("[{\"id\":\"minecraft:pig\",\"nbt\":{\"ArmorDropChances\":[0,0.5,0,0]},\"weight\":1}]"),
                blockId, JsonParser.parseString("{\"spawn_count\":2,\"spawn_speed\":20,\"spawn_monster\":\"panlingre:spawner/monster/test/nested.json\"}")));
        loader.apply(files, h.getLevel().getServer().getResourceManager(), InactiveProfiler.INSTANCE);
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        var spawner = new MonsterSpawnerBlockEntity(pos, ModBlocks.spawner.get().defaultBlockState());
        spawner.setLevel(h.getLevel());
        var data = TagParser.parseTag("{spawner_data:'panlingre:test/nested'}");
        spawner.loadWithComponents(data, h.getLevel().registryAccess());
        tick(h, pos, spawner);
        CompoundTag before = spawner.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(before.getInt("SpawnDelay") == 20, "Nested JSON reference failed");
        files.get(blockId).getAsJsonObject().addProperty("spawn_speed", 60);
        files.get(blockId).getAsJsonObject().add("spawn_max_count", JsonParser.parseString("{\"limit\":3,\"distance\":12.5,\"tags\":[\"1\",\"2\"]}"));
        loader.apply(files, h.getLevel().getServer().getResourceManager(), InactiveProfiler.INSTANCE);
        h.assertTrue(SpawnerDataLoader.resolve(data.get("spawner_data")).maxCount()
                .equals(new SpawnerConfig.SpawnLimit(3, 12.5, List.of("1", "2"))), "Reload lost count limit or tag strings");
        tick(h, pos, spawner);
        CompoundTag after = spawner.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(after.getInt("SpawnDelay") == 60 && after.getLong("LastSpawnTick") == before.getLong("LastSpawnTick"), "Reload did not update speed while retaining timestamp");
        h.assertTrue(after.get("spawner_data") instanceof StringTag, "Reference was replaced by a snapshot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void configurableCapUsesFinalPointSphereAndAllTags(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1)).above(140);
        level.setBlock(pos, ModBlocks.spawner.get().defaultBlockState(), 3);
        for (int y = 2; y < 20; y++) level.setBlock(pos.above(y), Blocks.STONE.defaultBlockState(), 3);
        var spawner = (MonsterSpawnerBlockEntity) level.getBlockEntity(pos);
        Vec3 center = Vec3.atBottomCenterOf(pos.above(20));
        var player = player(h);
        player.moveTo(pos.getX() + 2, pos.getY(), pos.getZ());
        List<Entity> fixtures = new ArrayList<>();
        try {
            // The matching boundary mob is outside the radius around the buried block itself.
            Entity boundary = capFixture(h, fixtures, center.add(12, 0, 0), "minecraft:pig", "1", "2");
            capFixture(h, fixtures, center, "minecraft:pig", "1");
            capFixture(h, fixtures, center, "minecraft:pig", "2");
            capFixture(h, fixtures, center.add(12.01, 0, 0), "minecraft:pig", "1", "2");
            capFixture(h, fixtures, center.add(9, 0, 9), "minecraft:pig", "1", "2");
            capFixture(h, fixtures, center, "minecraft:marker", "1", "2");
            CompoundTag data = TagParser.parseTag("{spawner_data:{spawn_count:5,spawn_speed:1,spawn_max_count:{limit:2,distance:12,tags:['1','2']},spawn_monster:[{id:'minecraft:pig',nbt:{NoAI:1b,NoGravity:1b,Tags:['1','2','cap_spawned']}}]}}");
            for (int attempt = 0; attempt < 3; attempt++) {
                if (attempt == 2) boundary.discard();
                data.putLong("LastSpawnTick", level.getGameTime() - 10);
                spawner.loadWithComponents(data, level.registryAccess());
                tick(h, pos, spawner);
                var spawned = level.getEntities((Entity) null, new AABB(pos).inflate(30), entity -> entity.getTags().contains("cap_spawned"));
                h.assertTrue(spawned.size() == (attempt == 2 ? 2 : 1), "Wrong cap, sphere or tag matching: " + spawned.size());
                h.assertTrue(spawned.stream().allMatch(entity -> entity.position().equals(center)), "Cap checked before finding the final point");
                if (attempt == 1) h.assertTrue(spawner.saveWithoutMetadata(level.registryAccess()).getLong("LastSpawnTick")
                        == data.getLong("LastSpawnTick"), "Blocked attempt reset the timer");
            }
            level.getEntities((Entity) null, new AABB(pos).inflate(30), entity -> entity.getTags().contains("cap_spawned")).forEach(Entity::discard);
            // A batch larger than the requested map cap must stop at exactly ten.
            data = TagParser.parseTag("{spawner_data:{spawn_count:20,spawn_speed:1,spawn_max_count:{limit:10,distance:12,tags:['monster']},spawn_monster:[{id:'minecraft:pig',nbt:{NoAI:1b,NoGravity:1b,Tags:['monster','cap_spawned']}}]}}");
            data.putLong("LastSpawnTick", level.getGameTime() - 10);
            spawner.loadWithComponents(data, level.registryAccess());
            tick(h, pos, spawner);
            var spawned = level.getEntities((Entity) null, new AABB(pos).inflate(30), entity -> entity.getTags().contains("cap_spawned"));
            h.assertTrue(spawned.size() == 10, "Batch exceeded or failed to reach the configured cap: " + spawned.size());
            spawned.forEach(Entity::discard);
            // Radius zero includes an entity at the exact final point; an empty tag list matches it.
            var cap = data.getCompound("spawner_data").getCompound("spawn_max_count");
            cap.putInt("limit", 1); cap.putInt("distance", 0); cap.put("tags", new ListTag());
            spawner.loadWithComponents(data, level.registryAccess());
            tick(h, pos, spawner);
            h.assertTrue(level.getEntities((Entity) null, new AABB(pos).inflate(30), entity -> entity.getTags().contains("cap_spawned")).isEmpty(),
                    "Zero radius or empty tags failed to match existing mobs at the final point");
        } finally {
            fixtures.forEach(Entity::discard);
            level.getEntities((Entity) null, new AABB(pos).inflate(30), entity -> entity.getTags().contains("cap_spawned")).forEach(Entity::discard);
            level.players().remove(player);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (int y = 2; y < 20; y++) level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    private static Entity capFixture(GameTestHelper h, List<Entity> fixtures, Vec3 position, String id, String... tags) throws Exception {
        Entity entity = EntityType.loadEntityRecursive(TagParser.parseTag("{id:'" + id + "',NoAI:1b,NoGravity:1b}"), h.getLevel(), loaded -> loaded);
        entity.moveTo(position.x, position.y, position.z);
        for (String tag : tags) entity.addTag(tag);
        h.getLevel().addFreshEntity(entity);
        fixtures.add(entity);
        return entity;
    }

    @GameTest(template = "empty")
    public static void offhandBoostIsTemporaryAndDoesNotStack(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1)).above(100);
        level.setBlock(pos, ModBlocks.spawner.get().defaultBlockState(), 3);
        var spawner = (MonsterSpawnerBlockEntity) level.getBlockEntity(pos);
        var first = player(h);
        var second = player(h);
        first.moveTo(pos.getX() + 2.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        second.moveTo(pos.getX() + 3.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        int delay = 20;
        int boostedDelay = (int) Math.ceil(delay / SpawnerBoostItem.SPAWN_FREQUENCY_MULTIPLIER);
        int boostedCount = Math.min(9, (int) (2 * SpawnerBoostItem.SPAWN_COUNT_MULTIPLIER));
        try {
            h.assertTrue(new ItemStack(ModItems.spawner_boost.get()).getMaxStackSize() == 1, "Booster should not stack as an item");
            checkBoostBatch(h, pos, spawner, boostedDelay, 0);
            first.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.spawner_boost.get()));
            checkBoostBatch(h, pos, spawner, boostedDelay, boostedCount);

            // Item counts and multiple holders must never multiply the bonus again.
            first.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.spawner_boost.get(), 64));
            second.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.spawner_boost.get()));
            checkBoostBatch(h, pos, spawner, boostedDelay, boostedCount);

            second.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            first.moveTo(pos.getX() + 0.5 + SpawnerBoostItem.RADIUS, pos.getY() + 0.5, pos.getZ() + 0.5);
            checkBoostBatch(h, pos, spawner, boostedDelay, boostedCount);
            first.moveTo(pos.getX() + 1.5 + SpawnerBoostItem.RADIUS, pos.getY() + 0.5, pos.getZ() + 0.5);
            checkBoostBatch(h, pos, spawner, boostedDelay, 0);
            checkBoostBatch(h, pos, spawner, delay, 2);

            first.moveTo(pos.getX() + 2.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            first.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            first.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.spawner_boost.get()));
            checkBoostBatch(h, pos, spawner, boostedDelay, 0);
            checkBoostBatch(h, pos, spawner, delay, 2);
        } finally {
            level.players().remove(first);
            level.players().remove(second);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    private static void checkBoostBatch(GameTestHelper h, BlockPos pos, MonsterSpawnerBlockEntity spawner,
                                        int elapsed, int expectedCount) throws Exception {
        var level = h.getLevel();
        CompoundTag data = TagParser.parseTag("{spawner_data:{spawn_count:2,spawn_speed:20,spawn_monster:[{id:'minecraft:pig',nbt:{NoAI:1b,NoGravity:1b,Tags:['monster','spawner_boost_test']}}]}}");
        data.putLong("LastSpawnTick", level.getGameTime() - elapsed);
        spawner.loadWithComponents(data, level.registryAccess());
        tick(h, pos, spawner);
        var entities = level.getEntities((Entity) null, new AABB(pos).inflate(10), entity -> entity.getTags().contains("spawner_boost_test"));
        h.assertTrue(entities.size() == expectedCount, "Expected " + expectedCount + " spawns, got " + entities.size());
        CompoundTag saved = spawner.saveWithoutMetadata(level.registryAccess());
        h.assertTrue(saved.getInt("SpawnDelay") == 20 && saved.getCompound("spawner_data").equals(data.getCompound("spawner_data")),
                "Offhand aura permanently modified the spawner configuration or baseline delay");
        entities.forEach(Entity::discard);
    }

    private static void tick(GameTestHelper h, BlockPos pos, MonsterSpawnerBlockEntity spawner) {
        MonsterSpawnerBlockEntity.tick(h.getLevel(), pos, spawner.getBlockState(), spawner);
    }

    private static ServerPlayer player(GameTestHelper h) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "SpawnerTest"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) {}
            @Override public void send(Packet<?> packet, PacketSendListener listener) {}
        };
        // The spawner only needs the level's player list; avoid unrelated mod login/network handlers.
        h.getLevel().players().add(player);
        return player;
    }

    private static List<Entity> mobs(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntities((Entity) null, new AABB(pos).inflate(10), entity -> entity.getTags().contains("spawner_test"));
    }
}
