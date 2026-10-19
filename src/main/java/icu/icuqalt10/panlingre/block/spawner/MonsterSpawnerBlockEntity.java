package icu.icuqalt10.panlingre.block.spawner;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModBlockEntities;
import icu.icuqalt10.panlingre.item.common.SpawnerBoostItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class MonsterSpawnerBlockEntity extends BlockEntity {
    // Skip the one-block-high maintenance space directly above buried spawners.
    public static final int SPAWN_SEARCH_START_OFFSET = 2;

    private Tag spawnerData = new CompoundTag();
    private SpawnerConfig config;
    private int configGeneration = -1;
    private long lastSpawnTick = -1;
    private int spawnDelay = -1;
    private long retryTick;

    public MonsterSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPAWNER_BE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spawnerData = tag.contains("spawner_data") ? tag.get("spawner_data").copy() : new CompoundTag();
        lastSpawnTick = tag.contains("LastSpawnTick", Tag.TAG_ANY_NUMERIC) ? tag.getLong("LastSpawnTick") : -1;
        spawnDelay = tag.contains("SpawnDelay", Tag.TAG_ANY_NUMERIC) ? tag.getInt("SpawnDelay") : -1;
        config = null;
        configGeneration = -1;
        retryTick = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("spawner_data", spawnerData.copy());
        tag.putLong("LastSpawnTick", lastSpawnTick);
        tag.putInt("SpawnDelay", spawnDelay);
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MonsterSpawnerBlockEntity spawner) {
        spawner.serverTick((ServerLevel) level);
    }

    private void serverTick(ServerLevel level) {
        long now = level.getGameTime();
        if (configGeneration != SpawnerDataLoader.generation()) {
            SpawnerConfig previous = config;
            configGeneration = SpawnerDataLoader.generation();
            try {
                config = SpawnerDataLoader.resolve(spawnerData);
                // A changed speed applies after /reload, without resetting the last spawn time.
                if (spawnDelay < 0 || previous != null && !previous.speed().equals(config.speed())
                        || spawnDelay < config.speed().min() || spawnDelay > config.speed().max()) {
                    spawnDelay = config.speed().sample(level.random);
                    setChanged();
                }
            } catch (RuntimeException exception) {
                config = null;
                PanlingRE.LOGGER.error("Invalid spawner at {} in {}: {}", worldPosition, level.dimension().location(), exception.getMessage());
            }
        }
        if (config == null || config.monsters().isEmpty() || config.count().max() == 0) return;
        if (lastSpawnTick < 0 || lastSpawnTick > now) {
            lastSpawnTick = now;
            setChanged();
        }
        double frequencyMultiplier = 1;
        double countMultiplier = 1;
        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator()
                    || !(player.getOffhandItem().getItem() instanceof SpawnerBoostItem boost)) continue;
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                    > (double) boost.getRadius() * boost.getRadius()) continue;
            frequencyMultiplier = Math.max(frequencyMultiplier, boost.getFrequencyMultiplier());
            countMultiplier = Math.max(countMultiplier, boost.getCountMultiplier());
        }
        if (now - lastSpawnTick < Math.max(1, spawnDelay / frequencyMultiplier) || now < retryTick) return;
        retryTick = now + 20;
        if (!level.hasNearbyAlivePlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 18)) return;

        int count = (int) (config.count().sample(level.random) * countMultiplier);
        boolean spawned = false;
        for (int i = 0; i < count; i++) {
            SpawnerConfig.Monster monster = config.choose(level.random);
            CompoundTag nbt = monster.nbt().copy();
            nbt.putString("id", monster.id().toString());
            var entity = EntityType.loadEntityRecursive(nbt, level, loaded -> loaded);
            if (entity == null || !findSpawnPosition(level, entity)) continue;
            var location = entity.position();
            var cap = config.maxCount();
            int nearby = level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(location)).inflate(cap.distance()),
                    other -> other.isAlive() && other.getTags().containsAll(cap.tags())
                            && other.distanceToSqr(location) <= cap.distance() * cap.distance()).size();
            if (nearby >= cap.limit()) continue;
            if (level.tryAddFreshEntityWithPassengers(entity)) {
                spawned = true;
            }
        }
        if (spawned || count == 0) {
            lastSpawnTick = now;
            spawnDelay = config.speed().sample(level.random);
            retryTick = 0;
            setChanged();
        }
    }

    private boolean findSpawnPosition(ServerLevel level, Entity entity) {
        var group = entity.getSelfAndPassengers().toList();
        // Apply NBT age/scale before checking any candidate, including mounted passengers.
        group.forEach(Entity::refreshDimensions);
        for (int y = worldPosition.getY() + SPAWN_SEARCH_START_OFFSET; y < level.getMaxBuildHeight(); y++) {
            entity.moveTo(worldPosition.getX() + 0.5, y, worldPosition.getZ() + 0.5, entity.getYRot(), entity.getXRot());
            boolean fits = true;
            for (Entity member : group) {
                if (member.isPassenger()) member.getVehicle().positionRider(member);
                AABB bounds = member.getBoundingBox();
                if (bounds.minY < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight()
                        || !level.getWorldBorder().isWithinBounds(bounds)) return false;
                if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                        BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) return false;
                if (!level.noBlockCollision(member, bounds)) {
                    fits = false;
                    break;
                }
            }
            if (fits) return true;
        }
        return false;
    }
}
