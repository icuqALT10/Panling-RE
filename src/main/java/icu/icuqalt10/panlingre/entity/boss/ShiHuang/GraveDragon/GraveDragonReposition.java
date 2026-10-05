package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A fixed destination for one ground attack, calculated once before its turn animation. */
final class GraveDragonReposition {
    record Plan(String action, Vec3 target, Vec3 from, Vec3 to, float fromYaw, float yaw,
                String animation, long startedAt, int ticks) { }

    static Plan plan(GraveDragonEntity dragon, GraveDragonActions.Action action, ServerPlayer player) {
        Vec3 target = player.position();
        Vec3 toward = target.subtract(dragon.position()).multiply(1, 0, 1);
        float yaw = toward.lengthSqr() < .001 ? dragon.yBodyRot
                : (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
        float difference = Mth.wrapDegrees(yaw - dragon.yBodyRot);
        String turn = difference > 0 ? "turn_ground_left" : "turn_ground_right";
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        var transform = GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale());
        for (var window : action.windows()) {
            int ticks = Math.max(1, (int) Math.ceil((window.to() - window.from()) * 20));
            for (int tick = 0; tick < ticks; tick += ticks <= 3 ? 1 : 3) {
                double seconds = window.from() + tick / 20.0;
                Vec3 destination;
                if (window.part().equals("breath")) {
                    destination = GraveDragonBreath.firingPosition(dragon, action.name(), seconds, yaw, player);
                } else if (window.part().equals("fan_breath")) {
                    destination = GraveDragonMythic.fanPosition(dragon, seconds, yaw, player);
                    if (!GraveDragonMythic.fanHits(dragon, GraveDragonMythic.fan(dragon, seconds, yaw, destination), player)) continue;
                } else if (window.part().startsWith("flank_")) {
                    var flank = GraveDragonMythic.flank(dragon, action.name(), yaw, dragon.position());
                    destination = dragon.position().add(target.subtract(flank.box().center).multiply(1, 0, 1));
                    if (!GraveDragonMythic.flankHits(dragon, GraveDragonMythic.flank(dragon, action.name(), yaw, destination), player)) continue;
                } else if (window.part().equals("tail_slam") || window.part().equals("rift")) {
                    Vec3 point = window.part().equals("rift")
                            ? GraveDragonMythic.riftOrigins(dragon, yaw, dragon.position()).get(1)
                                    .add(GraveDragonMythic.forward(yaw).scale(15))
                            : GraveDragonMythic.part(dragon, action.name(), seconds, 20, yaw, dragon.bodyPitch(), dragon.position());
                    destination = dragon.position().add(target.subtract(point).multiply(1, 0, 1));
                } else if (window.part().equals("landing") || window.part().equals("roar")
                        || window.part().equals("coil")) {
                    // Leave the target inside the area, with room between it and the root.
                    double distance = window.part().equals("roar") ? 30 : window.part().equals("coil") ? 8 : 5;
                    Vec3 facing = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
                    destination = target.subtract(facing.scale(distance));
                    destination = new Vec3(destination.x, dragon.getY(), destination.z);
                    if (window.part().equals("landing") && Math.abs(target.y - destination.y) >= 2.5) continue;
                } else {
                    int part = switch (window.part()) {
                        case "front_l" -> 26;
                        case "front_r" -> 39;
                        case "hind_l" -> 52;
                        case "hind_r" -> 65;
                        case "tail" -> dragon.partBroken("tail") ? 16 : 19;
                        case "bite" -> 78;
                        default -> 11;
                    };
                    var frame = GraveDragonPose.sample(action.name(), seconds, false);
                    var box = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[part],
                            GraveDragonEntity.PART_BOUNDS[part], transform, dragon.position());
                    Vec3 contact = box.center;
                    if (window.part().equals("charge")) {
                        // The head's centre passes above a standing player. Aim the
                        // low edge of a damaging OBB through the locked target instead.
                        for (int i = 0; i < GraveDragonEntity.PART_LABELS.length; i++) {
                            var hitbox = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[i],
                                    GraveDragonEntity.PART_BOUNDS[i], transform, dragon.position());
                            for (Vec3 corner : hitbox.corners()) {
                                if (corner.y >= player.getY() && corner.y < contact.y) { contact = corner; box = hitbox; }
                            }
                        }
                    }
                    Vec3 travel = Vec3.ZERO;
                    if (window.part().equals("charge") || window.part().equals("firezone") || window.part().equals("bite")) {
                        Vec3 facing = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
                        double distance = window.part().equals("bite") ? GraveDragonMythic.biteTravel(seconds)
                                : window.part().equals("charge") ? (seconds - window.from()) * 20 + 1 : 20;
                        if (window.part().equals("charge") && (distance < 8 || distance > 40)) continue;
                        travel = facing.scale(distance);
                        contact = contact.add(travel);
                    }
                    Vec3 shift = target.subtract(contact).multiply(1, 0, 1);
                    if (window.part().equals("charge") || window.part().equals("bite")) {
                        if (!box.move(shift.add(travel)).intersects(player.getBoundingBox())) continue;
                    }
                    if ((window.part().equals("charge") || window.part().equals("firezone"))
                            && dragon.level().clip(new ClipContext(box.center.add(shift), player.getEyePosition(),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS)
                        continue;
                    if (!window.part().equals("charge") && !window.part().equals("firezone") && !window.part().equals("bite")
                            && !box.move(shift).intersects(player.getBoundingBox())) continue;
                    destination = dragon.position().add(shift);
                }
                if (destination == null || destination.distanceToSqr(dragon.position()) >= bestDistance) continue;
                if (window.part().equals("firezone")
                        && !GraveDragonFireballEntity.canReach(dragon, action.name(), player, yaw, destination)) continue;
                if (window.part().equals("rift")) {
                    var origins = GraveDragonMythic.riftOrigins(dragon, yaw, destination);
                    boolean reaches = false;
                    for (int ray = 0; ray < 3; ray++)
                        if (GraveDragonRockEntity.canReach(dragon, origins.get(ray),
                                GraveDragonMythic.riftDirection(yaw, ray), player)) { reaches = true; break; }
                    if (!reaches) continue;
                }
                BlockPos floor = BlockPos.containing(destination.x, destination.y - .1, destination.z);
                if (!dragon.level().getBlockState(floor).isFaceSturdy(dragon.level(), floor, Direction.UP)) continue;
                if (!dragon.combatPoseFitsAt(turn, GraveDragonPose.duration(turn), yaw, dragon.bodyPitch(), destination, destination.y)
                        || !dragon.combatPoseFitsAt(action.name(), 0, yaw, dragon.bodyPitch(), destination, destination.y)) continue;
                if (window.part().equals("coil") && !dragon.combatPoseFitsAt(action.name(), seconds,
                        yaw, dragon.bodyPitch(), destination, destination.y)) continue;
                best = destination;
                bestDistance = destination.distanceToSqr(dragon.position());
            }
        }
        if (best == null) return null;
        double duration = GraveDragonPose.duration(turn);
        int steps = (int) Math.ceil(Math.max(duration * 20, Math.max(dragon.position().distanceTo(best) / .25, Math.abs(difference) / 2)));
        for (int step = 0; step <= steps; step++) {
            double progress = (double) step / steps;
            Vec3 position = dragon.position().lerp(best, progress);
            BlockPos floor = BlockPos.containing(position.x, position.y - .1, position.z);
            if (!dragon.level().getBlockState(floor).isFaceSturdy(dragon.level(), floor, Direction.UP)
                    || !dragon.combatPoseFitsAt(turn, duration * progress, dragon.yBodyRot + difference * (float) progress,
                    dragon.bodyPitch(), position, dragon.getY())) return null;
        }
        return new Plan(action.name(), target, dragon.position(), best, dragon.yBodyRot,
                yaw, turn, dragon.level().getGameTime(), 5);
    }

    private GraveDragonReposition() { }
}
