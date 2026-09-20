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
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
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
                helper.assertTrue(dragon.animationSeconds(0.5f) > 0.2, "Test did not advance animation time");
                helper.assertTrue(dragon.animationSeconds(0.5f) == dragonObserver.animationSeconds(0.5f), "Dragon observer phase differs");
                CompoundTag saved = new CompoundTag();
                dragon.addAdditionalSaveData(saved);
                dragonObserver.readAdditionalSaveData(saved);
                helper.assertTrue(dragon.animationSeconds(0.5f) == dragonObserver.animationSeconds(0.5f), "Reload restarts dragon animation");
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
     * 俯仰必须同时作用于渲染和碰撞箱。渲染与 OBB 的逐点一致性由 {@code multipartPoseTest}
     * 用真实 GeckoLib 参考实现验证；这里验证状态链路：服务端按实际速度方向算出的俯仰，
     * 确实传到了 {@code modelToEntity} 并让碰撞箱跟着倾斜。
     *
     * <p>几何上要注意：龙首几乎在锚点正上方（局部 Z ≈ -2），而尾巴伸到 -40 格，所以"低头"
     * 的主要表现是**尾巴抬起**，而不是龙首下沉。
     */
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void bodyPitchTiltsTheCollisionBoxes(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 40, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        Vec3 base = helper.absoluteVec(new Vec3(2, 40, 2));
        try {
            int tail = 20; // tail_tip
            for (int i = 0; i < 40; i++) {
                // 位置每 tick 复位：速度只用来驱动俯仰，不让位移混进测量里。
                dragon.setPos(base);
                dragon.setDeltaMovement(0, -1.0, 0);
                dragon.tick();
            }
            float divePitch = dragon.bodyPitch();
            double diveTailY = dragon.getWorldParts()[tail].getOrientedBox().center.y - dragon.getY();

            for (int i = 0; i < 40; i++) {
                dragon.setPos(base);
                dragon.setDeltaMovement(0, 1.0, 0);
                dragon.tick();
            }
            float climbPitch = dragon.bodyPitch();
            double climbTailY = dragon.getWorldParts()[tail].getOrientedBox().center.y - dragon.getY();

            helper.assertTrue(divePitch < -8.0F && climbPitch > 8.0F,
                    "Body pitch does not follow the velocity direction: dive=" + divePitch + " climb=" + climbPitch);
            helper.assertTrue(Math.abs(divePitch) <= 22.001F && Math.abs(climbPitch) <= 22.001F,
                    "Body pitch is not clamped: " + divePitch + " / " + climbPitch);
            // 俯冲（负俯仰）时尾巴抬起、爬升时尾巴压低。22 度对应约 15 格，远超姿态抖动。
            helper.assertTrue(diveTailY > climbTailY + 1.0,
                    "Pitch never reached the collision boxes: dive tail Y=" + diveTailY + " climb tail Y=" + climbTailY);
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * BossBar 必须显式订阅玩家才会显示。之前漏掉了 {@code startSeenByPlayer} /
     * {@code stopSeenByPlayer} 的覆盖，导致墓龙的 BossBar 从头到尾没有任何观众。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bossBarTracksViewers(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "BossBarTest"));
        try {
            helper.assertTrue(dragon.bossBarViewers().isEmpty(), "BossBar started with viewers already attached");
            dragon.startSeenByPlayer(player);
            helper.assertTrue(dragon.bossBarViewers().contains(player),
                    "BossBar did not pick up the player that started tracking the dragon");
            dragon.stopSeenByPlayer(player);
            helper.assertTrue(!dragon.bossBarViewers().contains(player),
                    "BossBar kept a player that stopped tracking the dragon");
            helper.succeed();
        } finally {
            dragon.discard();
            player.discard();
        }
    }

    /**
     * 世界内血条和原版名牌都挂在 {@link EntityAttachment#NAME_TAG} 上，而挂点由
     * {@code EntityDimensions} 的 attachments 决定。主体只有 1cm，挂点默认就贴在锚点上，
     * 于是名牌/血条出现在龙的身体根部而不是头上。
     *
     * <p>这里要求挂点被抬到头顶，同时宽高仍是锚点大小（否则会连带改变挤压、粒子散布等行为）。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nameTagHangsAboveTheHead(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            Vec3 attachment = dragon.getAttachments()
                    .getNullable(EntityAttachment.NAME_TAG, 0, dragon.getViewYRot(1.0F));
            helper.assertTrue(attachment != null, "Dragon has no name tag attachment");
            helper.assertTrue(attachment.y > 20.0,
                    "Name tag attachment is still at the anchor: " + attachment);
            helper.assertTrue(Math.abs(attachment.x) < 4.0 && Math.abs(attachment.z) < 8.0,
                    "Name tag attachment drifted off the head: " + attachment);
            // 换掉挂点不能顺手把尺寸也改了。
            helper.assertTrue(dragon.getBbWidth() < 0.1F && dragon.getBbHeight() < 0.1F,
                    "Root dimensions must stay the anchor, got "
                            + dragon.getBbWidth() + "x" + dragon.getBbHeight());
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 寻路必须按**当前这一刻的真实身体**判定，而不是主体那个 1cm 锚点盒子，也不是某张量好的表。
     *
     * <p>否则 AI 会给一个 1cm 的生物规划路线（往 1 格缝隙、往墙里走），每一步再被
     * {@code move()} 的 OBB 判定否掉，表现就是贴着墙反复磨、不会绕路。
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pathfindingUsesTheLiveBodyFootprint(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            // 尺寸查询本身必须留在锚点上：改了它会连带影响挤压、粒子散布、跳跃等行为。
            helper.assertTrue(dragon.getBbWidth() < 0.1F,
                    "Root size query must stay the anchor, got " + dragon.getBbWidth());
            helper.assertTrue(dragon.getNavigation() instanceof GraveDragonPathNavigation,
                    "Dragon is using the stock navigation again: " + dragon.getNavigation().getClass());
            helper.assertTrue(dragon.getNavigation().getNodeEvaluator()
                            instanceof GraveDragonBodyPathing.Ground,
                    "Dragon is using the stock node evaluator again");

            // 身体范围必须是"这一刻的 OBB 包络"：每个子碰撞箱的角点换算进自身坐标系后都要落在里面，
            // 而且不能退化成锚点大小。
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (int frame = 0; frame < 40; frame++) {
                dragon.tick();
                helper.assertTrue(dragon.bodyFootprintValid(), "Body footprint was never measured");
                double a = dragon.yBodyRot * Math.PI / 180.0;
                double cos = Math.cos(a), sin = Math.sin(a);
                for (GraveDragonPartEntity part : dragon.getWorldParts()) {
                    var box = part.getOrientedBox();
                    if (box == null) continue;
                    for (Vec3 c : box.corners()) {
                        double wx = c.x - dragon.getX(), wz = c.z - dragon.getZ();
                        minX = Math.min(minX, wx * cos + wz * sin);
                        maxX = Math.max(maxX, wx * cos + wz * sin);
                        minZ = Math.min(minZ, -wx * sin + wz * cos);
                        maxZ = Math.max(maxZ, -wx * sin + wz * cos);
                        maxY = Math.max(maxY, c.y - dragon.getY());
                    }
                }
            }
            helper.assertTrue(dragon.bodyMinX() <= minX + 1e-6 && dragon.bodyMaxX() >= maxX - 1e-6
                            && dragon.bodyMinZ() <= minZ + 1e-6 && dragon.bodyMaxZ() >= maxZ - 1e-6
                            && dragon.bodyMaxY() >= maxY - 1e-6,
                    "Reported footprint " + dragon.bodyMinX() + ".." + dragon.bodyMaxX() + " / "
                            + dragon.bodyMinZ() + ".." + dragon.bodyMaxZ() + " does not cover the body "
                            + minX + ".." + maxX + " / " + minZ + ".." + maxZ);
            // 这条龙是长条：向前伸出几十格，横向只有十几格。退化成锚点或变成正方形都是错的。
            helper.assertTrue(dragon.bodyMinZ() < -20.0, "Body no longer reaches forward: " + dragon.bodyMinZ());
            helper.assertTrue(dragon.bodyMaxY() > 20.0, "Body height collapsed: " + dragon.bodyMaxY());
            helper.assertTrue(dragon.bodyMaxX() - dragon.bodyMinX() < 20.0,
                    "Body width is implausibly wide: " + (dragon.bodyMaxX() - dragon.bodyMinX()));
            // WalkNodeEvaluator 的密集扫描对这条龙是 47*31*47 次查询/节点，必须保持稀疏采样。
            int samples = GraveDragonBodyPathing.sampleCount();
            helper.assertTrue(samples > 8 && samples < 200,
                    "Path node sampling must stay sparse and cover the body, got " + samples);
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 形态切换必须走过渡动画，而且**过渡播放期间就做垂直位移**：起飞播完刚好升到 10 格，
     * 落地播完刚好回到地面。召唤出来是地面形态，并立刻做一次起飞判断。
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void formSwitchesThroughTransitionAnimations(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setPos(helper.absoluteVec(new Vec3(2, 40, 2)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            helper.assertTrue(!dragon.flying(), "墓龙召唤出来应该是地面形态");
            helper.assertTrue("idle_ground".equals(dragon.animation()), "初始动画: " + dragon.animation());
            helper.assertTrue(!dragon.isNoGravity(), "地面形态应该有重力");

            // 召唤后应立刻判断起飞（这里头顶空旷，于是马上进起飞过渡）。
            dragon.tick();
            helper.assertTrue("takeoff".equals(dragon.animation()), "没有立刻判断起飞: " + dragon.animation());
            helper.assertTrue(!dragon.flying(), "过渡期间不该提前切形态");
            helper.assertTrue(dragon.isNoGravity(), "起飞过渡期间要临时关掉重力");

            double groundY = dragon.getY();
            // 过渡中途：应已抬升一部分。
            dragon.backdateAnimation(GraveDragonPose.duration("takeoff") * 0.5);
            dragon.tick();
            double halfway = dragon.getY() - groundY;
            helper.assertTrue(halfway > 2.0 && halfway < 8.0, "起飞中途抬升应在 2~8 格，实际 " + halfway);

            // 过渡播完：进空中形态，刚好升到 10 格。
            dragon.backdateAnimation(GraveDragonPose.duration("takeoff") + 0.5);
            dragon.tick();
            helper.assertTrue(dragon.flying(), "起飞过渡结束后没进空中形态");
            // 漫游可能立刻接手，于是动画会是 idle_air 或它的过渡；两者都算进空中形态。
            helper.assertTrue("idle_air".equals(dragon.animation()) || "idle_air_to_fly".equals(dragon.animation()),
                    "空中动画应该是 idle_air 或它的过渡: " + dragon.animation());
            helper.assertTrue(dragon.isNoGravity(), "空中形态应该无重力");
            double airborneY = dragon.getY();
            helper.assertTrue(Math.abs(airborneY - groundY - 10.0) < 0.2,
                    "起飞结束应刚好升到 10 格，实际 " + (airborneY - groundY));

            // 落地：同样在过渡期间下降，播完刚好到地面。
            // 降落的目标是**实际探测到的地面**，落差取决于当前离地高度，所以断言用相对关系。
            dragon.scheduleFormSwitchIn(0);
            dragon.tick();
            helper.assertTrue("land".equals(dragon.animation()), "落地没有过渡动画: " + dragon.animation());
            double landFrom = dragon.getY();
            dragon.backdateAnimation(GraveDragonPose.duration("land") * 0.5);
            dragon.tick();
            double midLandY = dragon.getY();
            helper.assertTrue(midLandY < landFrom - 1.0,
                    "落地过渡中途应该已经开始下降：" + landFrom + " -> " + midLandY);

            dragon.backdateAnimation(GraveDragonPose.duration("land") + 0.5);
            dragon.tick();
            double landedY = dragon.getY();
            helper.assertTrue(landedY < midLandY - 1.0,
                    "落地结束时应该比中途更低：" + midLandY + " -> " + landedY);
            helper.assertTrue(!dragon.flying(), "落地过渡结束后没进地面形态");
            helper.assertTrue("idle_ground".equals(dragon.animation()), "地面动画: " + dragon.animation());
            helper.assertTrue(!dragon.isNoGravity(), "地面形态不该无重力");
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /** 头顶垂直空间不足时不允许起飞，保持地面形态（空间 &lt; 10 格就一直待在地上）。 */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void takeoffNeedsVerticalClearance(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = helper.absoluteVec(new Vec3(4, 40, 4));
        // 天花板必须在实体加入世界**之前**放好：加入后世界会立刻 tick 一次，龙马上就会做
        // 第一次起飞判断，那时再放就晚了。
        BlockPos ceiling = BlockPos.containing(base).above(4);
        level.setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        helper.assertTrue(level.getBlockState(ceiling).is(Blocks.STONE), "天花板没有放上");

        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(base);
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            helper.assertTrue(!dragon.flying(), "墓龙召唤出来应该是地面形态");

            dragon.tick();
            helper.assertTrue(!dragon.flying(), "垂直空间不足却起飞了");
            helper.assertTrue("idle_ground".equals(dragon.animation()),
                    "空间不足时不该播起飞过渡: " + dragon.animation());

            // 拆掉天花板后应该能正常起飞（说明只是推迟、不是永久禁止）。
            level.setBlockAndUpdate(ceiling, Blocks.AIR.defaultBlockState());
            dragon.setPos(base);
            dragon.scheduleFormSwitchIn(0);
            dragon.tick();
            helper.assertTrue("takeoff".equals(dragon.animation()),
                    "拆掉天花板后仍不起飞: " + dragon.animation());
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 空中形态要换成原版的 3D 飞行寻路，而且**直线被挡时能绕出一条曲线**。
     *
     * <p>这正是避让的第 3 条：目标点本身可达，只是直线路径上有障碍，那就改路线、不动目标点。
     * 复用原版 A* 的好处是这条天然成立，不需要自己写避障，也就不会退化成"每 tick 换目标点"。
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void flightNavigationRoutesAroundObstacles(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        Vec3 base = helper.absoluteVec(new Vec3(4, 40, 4));
        dragon.setPos(base);
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            helper.assertTrue(dragon.getNavigation() instanceof GraveDragonPathNavigation,
                    "地面形态应该用地面寻路: " + dragon.getNavigation().getClass());

            // 升空 —— 形态切换会重建寻路器。
            dragon.scheduleFormSwitchIn(0);
            dragon.tick();
            dragon.backdateAnimation(GraveDragonPose.duration("takeoff") + 0.5);
            dragon.tick();
            helper.assertTrue(dragon.flying(), "没有进空中形态");
            helper.assertTrue(dragon.getNavigation() instanceof GraveDragonFlightNavigation,
                    "空中形态没有换成飞行寻路: " + dragon.getNavigation().getClass());

            // 在直线路径正中间挡一堵墙（横向 ±2、纵向 ±1），两侧留出绕行空间。
            BlockPos centre = BlockPos.containing(dragon.position());
            BlockPos wall = centre.offset(0, 0, 10);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
                }
            }

            BlockPos target = centre.offset(0, 0, 20);
            var path = dragon.getNavigation().createPath(target, 1);
            helper.assertTrue(path != null && path.canReach(),
                    "直线被挡时飞行寻路没有绕出路径（这正是第 3 条要保证的行为）");
            helper.assertTrue(path.getNodeCount() > 1, "路径节点太少，看起来没有真正规划");

            // 路径的每一个节点都不该落在障碍里——这才是"绕开"的直接证据。
            for (int i = 0; i < path.getNodeCount(); i++) {
                var node = path.getNode(i);
                helper.assertTrue(!level.getBlockState(new BlockPos(node.x, node.y, node.z)).is(Blocks.STONE),
                        "路径穿过了障碍物 @" + node.x + "," + node.y + "," + node.z);
            }
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 无仇恨漫游：四周随机挑一个**可达**点过去；到达后 30% 概率原地待机 10 秒，其余找下一个点。
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wanderFindsReachableTargetAndIdles(GameTestHelper helper) {
        var level = helper.getLevel();
        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(helper.absoluteVec(new Vec3(4, 60, 4)));
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            // 升空，验证空中形态的漫游（空气里 A* 总能找到路）。
            dragon.scheduleFormSwitchIn(0);
            dragon.tick();
            dragon.backdateAnimation(GraveDragonPose.duration("takeoff") + 0.5);
            dragon.tick();
            helper.assertTrue(dragon.flying(), "没有进空中形态");

            dragon.tick();
            helper.assertTrue(dragon.isMoving(), "漫游没有自己启动");

            int idleSeen = 0, retargetSeen = 0;
            for (int i = 0; i < 400 && (idleSeen == 0 || retargetSeen == 0); i++) {
                // 把目标点放到脚下就等于"已到达"（到达判定看的是距离）。
                dragon.setWanderTarget(dragon.position());
                dragon.tick();
                if (dragon.isWanderIdle()) {
                    idleSeen++;
                    helper.assertTrue(dragon.wanderIdleTicks() <= 200,
                            "原地待机不该超过 10 秒，实际 " + dragon.wanderIdleTicks() + " tick");
                    dragon.resetWanderIdle(); // 清掉计时，好继续验证另一个分支
                } else if (dragon.isMoving()) {
                    retargetSeen++;
                }
            }
            helper.assertTrue(idleSeen > 0, "到达后从来没有出现过原地待机");
            helper.assertTrue(retargetSeen > 0, "到达后从来没有换下一个目标点");
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }

    /**
     * 地面移动中转弯要播 turn_ground_left / turn_ground_right。
     * 空中暂时不接转向动画——turn_air_* 是留给后续攻击的（待机 → 转向目标 → 攻击）。
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void groundTurningPlaysTurnAnimations(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 base = helper.absoluteVec(new Vec3(4, 40, 4));
        // 头顶压一块天花板，让起飞净空检查失败，保证留在地面形态。
        level.setBlockAndUpdate(BlockPos.containing(base).above(1), Blocks.STONE.defaultBlockState());

        var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), level);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.noPhysics = true;
        dragon.setPos(base);
        helper.assertTrue(level.addFreshEntity(dragon), "Root failed to spawn");
        try {
            dragon.tick();
            helper.assertTrue(!dragon.flying(), "被天花板挡着却起飞了");

            // 转向判定看的是"目标点方向与当前朝向的偏角"。实体初始朝 +Z，而面朝 +Z 时**右方是 -X**
            // （左手系里右方 = 前方 × 上方 = (0,0,1)×(0,1,0) = (-1,0,0)），所以目标放在 -X 才是右转。
            dragon.setWanderTarget(new Vec3(base.x - 60, base.y, base.z));
            dragon.resetWanderIdle();
            for (int i = 0; i < 5; i++) dragon.tick();
            // 命名反直觉：美术按"观众在屏幕上看到的方向"命名，所以实体右转对应 turn_ground_left。
            helper.assertTrue("turn_ground_left".equals(dragon.animation()),
                    "地面移动右转时应该播 turn_ground_left（命名按观众视角），实际 " + dragon.animation());

            // 目标换到 +X（实体左转），应该切到另一个动作。
            dragon.setWanderTarget(new Vec3(base.x + 60, base.y, base.z));
            for (int i = 0; i < 5; i++) dragon.tick();
            helper.assertTrue("turn_ground_right".equals(dragon.animation()),
                    "地面移动左转时应该播 turn_ground_right，实际 " + dragon.animation());
            helper.succeed();
        } finally {
            dragon.discard();
        }
    }
}
