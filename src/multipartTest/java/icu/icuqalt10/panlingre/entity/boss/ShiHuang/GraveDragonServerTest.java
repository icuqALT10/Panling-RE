package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.entity.XingHaiEntity;
import net.minecraft.nbt.CompoundTag;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import java.util.List;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public final class GraveDragonServerTest {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void closeRangeOverlappingObbPick(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true); dragon.setNoGravity(true); dragon.noPhysics = true;
        Vec3 eye = helper.absoluteVec(new Vec3(2, 30, 2));
        dragon.setPos(eye);
        level.addFreshEntity(dragon);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ObbInsideTest"));
        try {
            // Use the actual broad-phase iteration order. A body enclosing the camera
            // must remain the closest hit even if a farther claw is visited later.
            var ordered = level.getPartEntities().stream().filter(p -> p.getParent() == dragon).toList();
            for (var part : dragon.getWorldParts())
                part.setOrientedBox(OrientedBoundingBox.axisAligned(eye.add(100, 0, 0), 1, 1, 1));
            var body = (GraveDragonPartEntity)ordered.get(0);
            var claw = (GraveDragonPartEntity)ordered.get(1);
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            Vec3 end = eye.add(0, 0, 4.5);
            for (int order = 0; order < 2; order++) {
                var inside = order == 0 ? body : claw;
                var farther = order == 0 ? claw : body;
                inside.setOrientedBox(OrientedBoundingBox.axisAligned(eye, 4, 4, 4));
                farther.setOrientedBox(OrientedBoundingBox.axisAligned(eye.add(0, 0, 4), 1, 1, 1));
                var hit = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1),
                        e -> e == inside || e == farther, 4.5 * 4.5);
                helper.assertTrue(hit != null && hit.getEntity() == inside,
                        "Inside body hit was overwritten by a farther OBB, turning a reachable hit into a miss");
                helper.assertTrue(hit.getLocation().distanceTo(eye) < 3, "Close-range OBB rejected by vanilla 3-block filter");
                var excluded = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1),
                        e -> e == farther, 4.5 * 4.5);
                helper.assertTrue(excluded != null && excluded.getEntity() == farther, "OBB correction ignores caller filter");
            }
            float before = dragon.getHealth();
            player.attack(claw); // The containing OBB after the second iteration.
            helper.assertTrue(dragon.getHealth() < before, "Empty-hand attack caused no health loss");
            helper.assertTrue(dragon.hurtTime > 0, "Empty-hand damage did not start hurt overlay");
            helper.succeed();
        } finally { dragon.discard(); player.discard(); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void meleeAgainstMovingObb(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ObbMeleeTest"));
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(40);
        try {
            var jaw = dragon.getWorldParts()[12];
            Vec3 center = dragon.position().add(0, 2, 0);
            var clientBox = OrientedBoundingBox.axisAligned(center, 1, 1, 1);
            jaw.setOrientedBox(clientBox);
            player.setPos(center.x, center.y - player.getEyeHeight(), center.z - 2);
            player.setYRot(0); player.setXRot(0);
            Vec3 eye = player.getEyePosition(), rayEnd = eye.add(player.getViewVector(1).scale(3));
            var clientHit = ProjectileUtil.getEntityHitResult(player, eye, rayEnd,
                    new AABB(eye, rayEnd).inflate(1), entity -> entity == jaw, 9);
            helper.assertTrue(clientHit != null && clientHit.getEntity() == jaw, "Client did not hit OBB");
            var attack = ServerboundInteractPacket.createAttackPacket(jaw, false);
            // The animated jaw moves after the pick, before the attack packet is handled.
            jaw.setOrientedBox(clientBox.move(new Vec3(0.75, 0, 0)));
            helper.assertTrue(jaw.getOrientedBox().clip(eye, rayEnd).isEmpty(), "Test must exercise a stale aim ray");
            Entity target = attack.getTarget(level);
            helper.assertTrue(target == jaw && player.canInteractWithEntity(target, 1), "Valid part packet or OBB reach rejected");
            float before = dragon.getHealth();
            player.attack(target); // Full vanilla melee + mod events, not direct part.hurt.
            helper.assertTrue(dragon.getHealth() < before, "Ordinary melee was discarded when the picked OBB moved");
            helper.assertTrue(dragon.getLastHurtByMob() == player, "Damage source lost the attacking player");
            // The fix must not make a distant part attackable.
            player.setPos(player.position().add(0, 0, -20));
            helper.assertTrue(!player.canInteractWithEntity(jaw, 1), "Reach check accepts distant OBB");
            helper.assertTrue(!jaw.hurt(level.damageSources().playerAttack(player), 100), "Direct melee bypasses OBB reach");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lateTrackingAnimationMetadata(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        var pangu = new PanGuEntity(ModEntities.PAN_GU.get(), level);
        pangu.startAnimation("attack.heavy");
        var swords = new FeiXianJianZhenEntity(ModEntities.FEI_XIAN_JIAN_ZHEN.get(), level);
        var stars = new XingHaiEntity(ModEntities.XING_HAI.get(), level);
        long startedAt = level.getGameTime();
        helper.runAfterDelay(5, () -> {
            var dragonObserver = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
            var panguObserver = new PanGuEntity(ModEntities.PAN_GU.get(), level);
            var swordsObserver = new FeiXianJianZhenEntity(ModEntities.FEI_XIAN_JIAN_ZHEN.get(), level);
            var starsObserver = new XingHaiEntity(ModEntities.XING_HAI.get(), level);
            List<Entity> originals = List.of(dragon, pangu, swords, stars);
            List<Entity> observers = List.of(dragonObserver, panguObserver, swordsObserver, starsObserver);
            try {
                // This is the metadata used by ServerEntity.sendPairingData. The
                // observer has a new tickCount/cache and missed every earlier event.
                for (int i = 0; i < originals.size(); i++) {
                    var metadata = originals.get(i).getEntityData().getNonDefaultValues();
                    helper.assertTrue(metadata != null, "No initial animation metadata");
                    observers.get(i).getEntityData().assignValues(metadata);
                    if (i == 0) continue;
                    int clips = 0;
                    for (var value : observers.get(i).getEntityData().getNonDefaultValues()) {
                        if (value.value() instanceof CompoundTag tag && tag.contains("clip")) {
                            helper.assertTrue(tag.getLong("start") == startedAt, "Late observer restarts animation clock");
                            clips++;
                        }
                    }
                    helper.assertTrue(clips == (i == 1 ? 2 : 1), "Missing current clip for newly tracked entity");
                }
                helper.assertTrue(dragon.idleAirSeconds(0.5f) > 0.2, "Test did not advance animation time");
                helper.assertTrue(dragon.idleAirSeconds(0.5f) == dragonObserver.idleAirSeconds(0.5f), "Dragon observer phase differs");
                CompoundTag saved = new CompoundTag();
                dragon.addAdditionalSaveData(saved);
                dragonObserver.readAdditionalSaveData(saved);
                helper.assertTrue(dragon.idleAirSeconds(0.5f) == dragonObserver.idleAirSeconds(0.5f), "Reload restarts dragon animation");
                helper.succeed();
            } finally {
                originals.forEach(Entity::discard);
                observers.forEach(Entity::discard);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativePartsAndObbHits(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        try {
            var parts = dragon.getWorldParts();
            helper.assertTrue(parts.length == 79, "Missing upper jaw");
            helper.assertTrue(!dragon.isPickable(), "Logical anchor is an extra hit target");
            for (var part : parts) {
                helper.assertTrue(level.getEntity(part.getId()) == null, "Part was independently spawned");
                helper.assertTrue(level.getEntityOrPart(part.getId()) == part, "Network attack id cannot resolve part");
                helper.assertTrue(part.getId() == dragon.getId() + part.getPartIndex() + 1, "Part id is not deterministic");
                helper.assertTrue(!((Entity)part instanceof LivingEntity), "Part still owns independent health");
            }
            var head = parts[11];
            Vec3 center = head.getOrientedBox().center;
            double q = Math.sqrt(0.5);
            head.setOrientedBox(new OrientedBoundingBox(center, new Vec3(q, 0, q), new Vec3(0, 1, 0),
                    new Vec3(-q, 0, q), new Vec3(4, 0.5, 0.5)));
            var arrow = new Arrow(EntityType.ARROW, level);
            Vec3 empty = center.add(2.5, 0, -2.5), from = empty.add(0, 2, 0), to = empty.add(0, -2, 0);
            helper.assertTrue(ProjectileUtil.getEntityHitResult(level, arrow, from, to,
                    new AABB(from, to).inflate(1), e -> e == head) == null, "Vanilla projectile hits empty AABB corner");
            from = center.add(0, 2, 0); to = center.add(0, -2, 0);
            var hit = ProjectileUtil.getEntityHitResult(level, arrow, from, to, new AABB(from, to).inflate(1), e -> e == head);
            helper.assertTrue(hit != null && hit.getEntity() == head, "Vanilla projectile misses OBB");
            helper.assertTrue(ProjectileUtil.getEntityHitResult(arrow, empty.add(0, 2, 0), empty.add(0, -2, 0),
                    AABB.ofSize(empty, 2, 6, 2), e -> e == head, 16) == null, "Client picking overload hits empty corner");
            helper.assertTrue(ProjectileUtil.getEntityHitResult(arrow, from, to,
                    new AABB(from, to).inflate(1), e -> e == head, 16) != null, "Client picking overload misses OBB");
            helper.assertTrue(level.getEntities((Entity)null, AABB.ofSize(empty, 0.1, 0.1, 0.1), e -> e == head).isEmpty(),
                    "World area query still uses part envelope");
            helper.assertTrue(MultipartEntity.collectTargets(level, AABB.ofSize(center, 0.2, 0.2, 0.2), null).contains(dragon),
                    "Area skills cannot resolve native part to living root");
            float before = dragon.getHealth();
            arrow.setPos(from); arrow.setDeltaMovement(to.subtract(from));
            helper.assertTrue(head.hurt(level.damageSources().arrow(arrow, null), 10), "Part rejected a real projectile hit");
            helper.assertTrue(dragon.getHealth() < before, "Part damage did not reach root");
            dragon.discard();
            for (var part : parts) helper.assertTrue(level.getEntityOrPart(part.getId()) == null, "Orphan part after root removal");
            helper.succeed();
        } finally {
            if (!dragon.isRemoved()) dragon.discard();
        }
    }
}
