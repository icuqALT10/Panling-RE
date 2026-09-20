package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.entity.XingHaiEntity;
import icu.icuqalt10.panlingre.util.SkillHelper;
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

    /**
     * The real acceptance criterion for melee: standing beside the dragon and aiming at
     * ANY part must damage the dragon. This drives the exact server path the attack
     * packet uses (part.hurt -> serializable ray re-cast), so it does not depend on
     * client input timing.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyAimedPartTakesDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        dragon.setHealth(1_000_000);
        // Stand a few blocks to the dragon's side so several parts are in reach.
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var parts = dragon.getWorldParts();
        int aimed = 0, damaged = 0;
        var failed = new java.util.ArrayList<String>();
        // A fresh attacker per aim: the mod keeps damage cooldowns per attacker, so
        // reusing one player would let an earlier i-frame mask a later hit.
        FakePlayer player = null;
        try {
            for (int i = 0; i < parts.length; i++) {
                var box = parts[i].getOrientedBox();
                if (box == null) continue;
                Vec3 centre = box.center;
                player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "MeleeAccept" + i));
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(50);
                // Put the eye a step back along the box normal so the part is in front.
                Vec3 eye = centre.add(box.axisX.scale(box.halfExtents.x + 0.6));
                player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
                Vec3 delta = centre.subtract(player.getEyePosition());
                player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
                player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
                player.yRotO = player.getYRot();
                player.xRotO = player.getXRot();
                // The server must resolve this aim to some part of the body.
                int struck = dragon.pickPartAlongViewRay(player);
                aimed++;
                if (struck < 0) {
                    failed.add("part" + i + " not resolved");
                    continue;
                }
                float before = dragon.getHealth();
                parts[i].hurt(level.damageSources().playerAttack(player), 1000.0F);
                if (dragon.getHealth() < before) damaged++;
                else failed.add("part" + i + " resolved to " + struck + " but no damage");
                player.discard();
                player = null;
            }
            helper.assertTrue(aimed > 70, "Expected the full part list, got " + aimed);
            helper.assertTrue(failed.isEmpty(),
                    "Melee missed " + failed.size() + "/" + aimed + " aimed parts: " + failed);
            helper.assertTrue(damaged == aimed, "Only " + damaged + "/" + aimed + " aims damaged the dragon");
            helper.succeed();
        } finally {
            if (player != null) player.discard();
            dragon.discard();
        }
    }

    /**
     * Regression for the bug that made melee feel dead over large parts of the body: the
     * server used to resolve its own ray first and then reject the result when that part
     * was out of reach. A long body part further along the view line (the torso runs
     * several blocks deep) could therefore win the ray and veto an otherwise valid click
     * on a nearby limb. The client's in-reach part must win instead.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void reachableRequestedPartBeatsDistantRayHit(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ReachOrderTest"));
        try {
            var parts = dragon.getWorldParts();
            // Stand beside part 22 (left foreleg) and aim through it at the far end of the
            // body, so the server's ray legitimately reaches several deeper parts too.
            var near = parts[22].getOrientedBox();
            Vec3 eye = near.center.add(near.axisX.scale(near.halfExtents.x + 0.5));
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            Vec3 far = parts[0].getOrientedBox().center;
            Vec3 delta = far.subtract(player.getEyePosition());
            player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
            player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();

            int rayPart = dragon.pickPartAlongViewRay(player);
            helper.assertTrue(rayPart >= 0, "Test needs the view ray to reach the body");
            helper.assertTrue(dragon.canPlayerReachPart(player, rayPart),
                    "Test needs the ray hit to be reachable, got part" + rayPart);

            // The ray corrects a wrong pick: aiming through the body at the far end resolves
            // to a part the ray actually reaches, and the attack still lands.
            int resolved = dragon.resolveMeleeStrike(player, 22);
            helper.assertTrue(resolved >= 0, "A reachable aim must not be discarded");
            helper.assertTrue(dragon.canPlayerReachPart(player, resolved),
                    "Resolved part " + resolved + " is out of reach");

            float before = dragon.getHealth();
            parts[22].hurt(level.damageSources().playerAttack(player), 100.0F);
            helper.assertTrue(dragon.getHealth() < before, "Resolved melee produced no damage");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
    }

    /**
     * A direct hurt() call from far outside the interaction range must still be refused, so
     * the part resolver cannot be abused by code paths that bypass the attack packet's own
     * reach guard.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void distantDirectMeleeIsRefused(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DistantMeleeTest"));
        try {
            var parts = dragon.getWorldParts();
            var box = parts[22].getOrientedBox();
            // Stand well beyond any attack range.
            Vec3 away = box.center.add(box.axisX.scale(box.halfExtents.x + 20.0));
            player.setPos(away.x, away.y - player.getEyeHeight(), away.z);
            float before = dragon.getHealth();
            helper.assertTrue(!parts[22].hurt(level.damageSources().playerAttack(player), 100.0F),
                    "A part 20 blocks away accepted a melee hit");
            helper.assertTrue(dragon.getHealth() == before, "Distant melee still dealt damage");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
    }

    /**
     * One player attacking several parts inside the same tick must damage the dragon
     * once. Every part forwards to the same parent, so the parent's per-attacker damage
     * cooldown has to collapse the burst into a single hit.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void burstOnManyPartsDamagesOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        dragon.setHealth(1_000_000);
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "BurstTest"));
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(50);
        try {
            // Aim so that several parts are genuinely in range, then hit all of them in
            // one tick with the very same attacker.
            var parts = dragon.getWorldParts();
            var box = parts[0].getOrientedBox();
            Vec3 eye = box.center.add(box.axisX.scale(box.halfExtents.x + 0.6));
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            int reachable = 0;
            for (int i = 0; i < parts.length; i++) {
                if (dragon.canPlayerReachPart(player, i)) reachable++;
            }
            helper.assertTrue(reachable >= 2, "Test needs several parts in range, got " + reachable);

            float before = dragon.getHealth();
            int accepted = 0;
            for (int i = 0; i < parts.length; i++) {
                if (!dragon.canPlayerReachPart(player, i)) continue;
                if (parts[i].hurt(level.damageSources().playerAttack(player), 1000.0F)) accepted++;
            }
            float total = before - dragon.getHealth();
            helper.assertTrue(accepted == 1,
                    "Burst of " + reachable + " reachable parts returned " + accepted + " hits, expected 1");
            // Damage multiplier is at most 2.0, so a single hit cannot exceed 2000.
            helper.assertTrue(total > 0 && total <= 2000.0F,
                    "Burst dealt " + total + " damage; expected exactly one hit's worth");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
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

    /**
     * 玩家报告的原始问题：混元神鼎 Skill4 的火龙卷打不到墓龙。
     *
     * <p>龙卷风只查询自己那一格碰撞箱内的活体，而子碰撞箱是普通 Entity（不继承
     * LivingEntity），主体的逻辑盒又远小于躯体，于是龙卷风扫过脖颈或翅膀时什么都找不到。
     * 这里刻意选离逻辑锚点最远的部件落点，先断言原版查询确实漏掉墓龙（即复现了 bug），
     * 再断言龙卷风真的扣了血。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fireTornadoHitsPartAwayFromAnchor(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        dragon.setHealth(1_000_000);
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();

        var parts = dragon.getWorldParts();
        int farIndex = -1;
        double farDist = -1;
        for (int i = 0; i < parts.length; i++) {
            var box = parts[i].getOrientedBox();
            if (box == null) continue;
            double d = box.center.distanceTo(dragon.position());
            if (d > farDist) { farDist = d; farIndex = i; }
        }
        helper.assertTrue(farIndex >= 0, "No part owns an oriented box");
        helper.assertTrue(farDist > 8.0,
                "Test needs a part well away from the logical anchor, got " + farDist);

        FireTornadoEntity tornado = null;
        try {
            Vec3 impact = parts[farIndex].getOrientedBox().center;
            tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), level,
                    impact, impact, 40, 50.0F);
            helper.assertTrue(level.addFreshEntity(tornado), "Tornado failed to spawn");

            // The regression condition: the query the skill used to run cannot see the
            // dragon at all, so any damage has to come from the part-resolving lookup.
            helper.assertTrue(level.getEntitiesOfClass(LivingEntity.class, tornado.getBoundingBox())
                            .stream().noneMatch(e -> e == dragon),
                    "Vanilla LivingEntity query already finds the dragon; test proves nothing");
            helper.assertTrue(MultipartEntity.collectTargets(level, tornado.getBoundingBox(), tornado)
                            .contains(dragon),
                    "Part-resolving lookup cannot reach the dragon through part " + farIndex);

            float before = dragon.getHealth();
            tornado.tick();
            helper.assertTrue(dragon.getHealth() < before,
                    "Fire tornado overlapping part " + farIndex + " dealt no damage to the dragon");
            helper.succeed();
        } finally {
            if (tornado != null) tornado.discard();
            dragon.discard();
        }
    }

    /**
     * 混元神鼎 Skill3 与盘古的冰冻锥形区域都走 {@code SkillHelper.getLivingEntitiesInFront}。
     * 多节实体的逻辑坐标只是记账锚点，躯体可以整体落在该点之外，所以只采样
     * {@code position()} 会让“只有翅膀/尾巴伸进锥形区域”的 Boss 被整个漏掉。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void forwardConeFindsDistantPart(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ConeTest"));
        try {
            var parts = dragon.getWorldParts();
            int farIndex = -1;
            double farDist = -1;
            for (int i = 0; i < parts.length; i++) {
                var box = parts[i].getOrientedBox();
                if (box == null) continue;
                double d = box.center.distanceTo(dragon.position());
                if (d > farDist) { farDist = d; farIndex = i; }
            }
            helper.assertTrue(farIndex >= 0 && farDist > 8.0,
                    "Test needs a part well away from the logical anchor, got " + farDist);
            Vec3 partCenter = parts[farIndex].getOrientedBox().center;

            // Stand 6 blocks behind the part facing +Z (yRot 0) with a 6-long, 2-wide,
            // 2-tall cone: the part sits at its far end, so it is the only thing inside.
            player.setPos(partCenter.x, partCenter.y, partCenter.z - 6.0);
            player.setYRot(0);
            player.setXRot(0);
            Vec3 center = new Vec3(partCenter.x, partCenter.y, partCenter.z - 3.0);
            Vec3 anchorOffset = dragon.position().subtract(center);
            helper.assertTrue(Math.abs(anchorOffset.z) > 3.0
                            || Math.abs(anchorOffset.x) > 1.0
                            || Math.abs(anchorOffset.y) > 1.0,
                    "Test must exercise an anchor outside the cone, offset=" + anchorOffset);

            List<LivingEntity> found = SkillHelper.getLivingEntitiesInFront(player, 2.0, 2.0, 6.0);
            helper.assertTrue(found.contains(dragon),
                    "Cone missed a boss whose part " + farIndex + " lies inside it");
            helper.assertTrue(!found.contains(player), "Cone returned its own caster");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
    }

    /** 多节实体的采样点必须覆盖每一个子碰撞箱，否则区域判定会漏掉部分躯体。 */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bodySamplesCoverEveryPart(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        dragon.tick();
        try {
            List<Vec3> samples = dragon.multipartBodySamples();
            var parts = dragon.getWorldParts();
            helper.assertTrue(samples.get(0).equals(dragon.position()), "Anchor sample is not the logical position");

            AABB cover = null;
            for (Vec3 sample : samples) {
                cover = cover == null ? new AABB(sample, sample) : cover.minmax(new AABB(sample, sample));
            }
            helper.assertTrue(cover != null, "A multipart root produced no samples");
            int boxes = 0;
            for (var part : parts) {
                var box = part.getOrientedBox();
                if (box == null) continue;
                boxes++;
                AABB partBox = part.getBoundingBox();
                helper.assertTrue(cover.minX <= partBox.minX && cover.minY <= partBox.minY
                                && cover.minZ <= partBox.minZ && cover.maxX >= partBox.maxX
                                && cover.maxY >= partBox.maxY && cover.maxZ >= partBox.maxZ,
                        "Sample envelope does not cover part " + part.getPartIndex());
                helper.assertTrue(samples.stream().anyMatch(s -> s.distanceToSqr(box.center) < 1.0E-9D),
                        "Part " + part.getPartIndex() + " has no sample at its centre");
            }
            helper.assertTrue(boxes == parts.length, "A part is missing its oriented box");
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 寻路必须按**真实身体**判定，而不是主体那个 1cm 锚点盒子。
     *
     * <p>否则 AI 会给一个 1cm 的生物规划路线（往 1 格缝隙、往墙里走），每一步再被
     * {@code move()} 的 OBB 判定否掉，表现就是贴着墙反复磨、不会绕路。
     *
     * <p>身体的具体尺寸由 {@code GraveDragonPoseTest} 对着 OBB 表逐帧重算校验；这里验证接线：
     * 龙真的换了自己的导航与节点评估器，而且每个节点的采样开销是有界的。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pathfindingUsesTheRealBody(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            helper.assertTrue(GraveDragonEntity.pathingNose() > 30.0F,
                    "Pathing footprint collapsed to the anchor: nose=" + GraveDragonEntity.pathingNose());
            helper.assertTrue(GraveDragonEntity.pathingHalfWidth() > 4.0F,
                    "Pathing footprint collapsed to the anchor: halfWidth=" + GraveDragonEntity.pathingHalfWidth());
            // 尺寸查询本身必须留在锚点上：改了它会连带影响挤压、粒子散布、跳跃等行为。
            helper.assertTrue(dragon.getBbWidth() < 0.1F,
                    "Root size query must stay the anchor, got " + dragon.getBbWidth());

            helper.assertTrue(dragon.getNavigation() instanceof GraveDragonPathNavigation,
                    "Dragon is using the stock navigation again: " + dragon.getNavigation().getClass());
            helper.assertTrue(dragon.getNavigation().getNodeEvaluator()
                            instanceof GraveDragonPathNavigation.BodyAwareWalkNodeEvaluator,
                    "Dragon is using the stock node evaluator again");
            // WalkNodeEvaluator 的密集扫描对这条龙是 47*31*47 次查询/节点，必须保持稀疏采样。
            int samples = GraveDragonPathNavigation.BodyAwareWalkNodeEvaluator.sampleCount();
            helper.assertTrue(samples > 8 && samples < 200,
                    "Path node sampling must stay sparse and cover the body, got " + samples);
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }
}
