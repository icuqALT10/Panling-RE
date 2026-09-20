package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Reports which part the production pick actually selects when the crosshair is aimed at a
 * given part.
 *
 * <p>The resolver in {@link GraveDragonEntity#pickPartAlongViewRay} is the one that decides
 * which part a melee swing damages, so a chain of parts that selects its successor means the
 * aim is consistently resolved one link too far along, and the last links become unreachable.
 * This test prints that mapping so the pattern is visible instead of guessed at.
 */
@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public final class GraveDragonPickMappingTest {
    private static final int[] NECK_CHAIN = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 21, 11};
    private static final int[] FRONT_LEFT_CHAIN = {22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34};

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void reportPickMapping(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();

        var player = new net.neoforged.neoforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "PickMap"));
        try {
            System.out.println("PICKMAP --- neck chain (aim -> resolved) ---");
            for (int index : NECK_CHAIN) report(level, player, dragon, index, true);
            System.out.println("PICKMAP --- front left leg chain ---");
            for (int index : FRONT_LEFT_CHAIN) report(level, player, dragon, index, false);

            // Also report, for each part, whether the ray that STARTS at its own centre
            // resolves back to it. A part that fails this is genuinely unreachable.
            System.out.println("PICKMAP --- self-resolution from own surface ---");
            var broken = new java.util.ArrayList<String>();
            for (int index = 0; index < dragon.getWorldParts().length; index++) {
                var box = dragon.getWorldParts()[index].getOrientedBox();
                if (box == null) continue;
                int hits = 0;
                for (int face = 0; face < 6; face++) {
                    Vec3 axis = switch (face) {
                        case 0 -> box.axisX;
                        case 1 -> box.axisX.scale(-1);
                        case 2 -> box.axisY;
                        case 3 -> box.axisY.scale(-1);
                        case 4 -> box.axisZ;
                        default -> box.axisZ.scale(-1);
                    };
                    double half = switch (face) {
                        case 0, 1 -> box.halfExtents.x;
                        case 2, 3 -> box.halfExtents.y;
                        default -> box.halfExtents.z;
                    };
                    Vec3 eye = box.center.add(axis.scale(half + 0.5));
                    player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
                    Vec3 delta = box.center.subtract(player.getEyePosition());
                    player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
                    player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
                    player.yRotO = player.getYRot();
                    player.xRotO = player.getXRot();
                    if (dragon.pickPartAlongViewRay(player) == index) hits++;
                }
                if (hits == 0) broken.add(index + ":" + GraveDragonEntity.PART_LABELS[index]);
            }
            System.out.println("PICKMAP never resolves to itself (" + broken.size() + "): " + broken);
            helper.succeed();
        } finally {
            player.discard();
            dragon.discard();
        }
    }

    /** Aims the crosshair at a part's centre from just outside it and reports the resolution. */
    private static void report(net.minecraft.world.level.Level level, Player player,
                               GraveDragonEntity dragon, int index, boolean fromLeft) {
        var box = dragon.getWorldParts()[index].getOrientedBox();
        if (box == null) return;
        Vec3 away = box.axisX.scale(fromLeft ? box.halfExtents.x + 0.8 : -(box.halfExtents.x + 0.8));
        Vec3 eye = box.center.add(away);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        Vec3 delta = box.center.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();

        int resolved = dragon.pickPartAlongViewRay(player);
        Vec3 look = player.getLookAngle();
        Vec3 end = player.getEyePosition().add(look.scale(player.entityInteractionRange() + 1.0));
        EntityHitResult vanilla = ProjectileUtil.getEntityHitResult(player, player.getEyePosition(), end,
                new AABB(player.getEyePosition(), end).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable(), 16.0);
        String vanillaName = vanilla != null && vanilla.getEntity() instanceof GraveDragonPartEntity part
                ? "part" + part.getPartIndex() : (vanilla == null ? "MISS" : "other");
        System.out.println("PICKMAP aim=" + index + ":" + GraveDragonEntity.PART_LABELS[index]
                + " -> resolved=" + (resolved < 0 ? "NONE"
                        : resolved + ":" + GraveDragonEntity.PART_LABELS[resolved])
                + "   clientPick=" + vanillaName
                + (resolved == index ? "" : "   <-- MISMATCH"));
    }
}
