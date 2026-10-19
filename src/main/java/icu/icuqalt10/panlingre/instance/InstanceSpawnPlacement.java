package icu.icuqalt10.panlingre.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class InstanceSpawnPlacement {
    private InstanceSpawnPlacement() {}

    static List<Vec3> find(ServerLevel level, InstanceDefinition definition, int slot, List<ServerPlayer> players) {
        Vec3 center = definition.playerSpawnForSlot(slot);
        double radius = definition.safeDistance();
        if (radius == 0) return Collections.nCopies(players.size(), center);

        // Sample the horizontal disk at the configured spawn height, including its centre.
        List<Vec3> candidates = new ArrayList<>();
        int extent = (int) Math.ceil(radius / .75);
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                double dx = x * .75, dz = z * .75;
                if (dx * dx + dz * dz <= radius * radius) {
                    Vec3 candidate = center.add(dx, 0, dz);
                    if (definition.contains(slot, candidate)) candidates.add(candidate);
                }
            }
        }
        List<Vec3> selected = new ArrayList<>();
        List<AABB> occupied = new ArrayList<>();
        for (ServerPlayer player : players) {
            Vec3 best = null;
            double bestScore = -Double.MAX_VALUE;
            for (Vec3 candidate : candidates) {
                AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(candidate);
                double nearest = selected.stream().mapToDouble(candidate::distanceToSqr).min().orElse(Double.MAX_VALUE);
                if (nearest < 1 || occupied.stream().anyMatch(box::intersects) || !safe(level, player, box)) continue;
                // Solo/first player stays nearest the centre; later players spread away from those already placed.
                double score = selected.isEmpty() ? -candidate.distanceToSqr(center) : nearest;
                if (score > bestScore) { best = candidate; bestScore = score; }
            }
            if (best == null) return List.of();
            selected.add(best);
            occupied.add(player.getDimensions(Pose.STANDING).makeBoundingBox(best));
        }
        return selected;
    }

    private static boolean safe(ServerLevel level, ServerPlayer player, AABB box) {
        if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(player, box)
                || level.containsAnyLiquid(box)
                || !level.getBlockCollisions(player, box.move(0, -.05, 0)).iterator().hasNext()) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - .05, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.POWDER_SNOW)) return false;
        }
        return true;
    }
}
