package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.CompletableFuture;

/**
 * End-to-end acceptance probe for left-click melee. Stands next to a temporary
 * dragon, aims at each part's oriented-box centre, fires a real attack key press
 * (which goes through client picking and the vanilla attack packet), and reports
 * whether the server actually applied damage. Run with -Dpanlingre.meleeAccept=true.
 */
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class GraveDragonMeleeAccept {
    private static int ticks, menuTicks, stage, rootId, part, cooldown;
    private static boolean opened, finishing;
    private static Vec3 savedPos;
    private static float savedYaw, savedPitch;
    private static boolean savedFlying;
    private static CompletableFuture<?> pending;
    private static float healthBefore;
    private static int hits, misses, wrongTarget;
    private static String currentAim = "";
    private static int printed;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("panlingre.meleeAccept")) return;
        var mc = Minecraft.getInstance();
        if (!opened && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen && ++menuTicks > 60) {
            opened = true;
            var menu = mc.screen;
            mc.createWorldOpenFlows().openWorld("world", () -> mc.setScreen(menu));
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || finishing) return;
        if (pending != null && !pending.isDone()) return;
        try {
            if (pending != null) { pending.join(); pending = null; }
            if (++ticks > 30000) { finish(mc); return; }
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();

            if (stage == 0) {
                savedPos = mc.player.position();
                savedYaw = mc.player.getYRot();
                savedPitch = mc.player.getXRot();
                savedFlying = mc.player.getAbilities().flying;
                mc.player.getAbilities().flying = true;
                mc.player.onUpdateAbilities();
                pending = server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var dragon = new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(), player.serverLevel());
                    dragon.setNoAi(true);
                    dragon.setNoGravity(true);
                    dragon.noPhysics = true;
                    dragon.setPos(savedPos.x + 4.0, savedPos.y, savedPos.z);
                    player.serverLevel().addFreshEntity(dragon);
                    rootId = dragon.getId();
                    // Generous health so every probe can be observed independently.
                    dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
                    dragon.setHealth(1_000_000);
                    System.out.println("MELEEACCEPT spawned root=" + rootId);
                });
                stage = 1; ticks = 0;
                return;
            }

            if (stage == 1 && ticks > 80) {
                part = 0;
                stage = 2;
                ticks = 0;
                return;
            }

            if (stage == 2) {
                if (part >= 79) { finish(mc); return; }
                if (cooldown > 0) { cooldown--; return; }
                var dragon = (GraveDragonEntity) mc.level.getEntity(rootId);
                if (dragon == null) return;
                var box = dragon.getWorldParts()[part].getOrientedBox();
                Vec3 eye = mc.player.getEyePosition();
                Vec3 delta = box.center.subtract(eye);
                mc.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
                mc.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
                mc.player.yRotO = mc.player.getYRot();
                mc.player.xRotO = mc.player.getXRot();
                mc.gameRenderer.pick(1.0F);
                currentAim = mc.hitResult instanceof EntityHitResult hit
                        && hit.getEntity() instanceof GraveDragonPartEntity p
                        ? "part" + p.getPartIndex() : String.valueOf(mc.hitResult.getType());
                int targetPart = part;
                pending = server.submit(() -> {
                    var root = (GraveDragonEntity) server.overworld().getEntity(rootId);
                    if (root != null) healthBefore = root.getHealth();
                    System.out.println("MELEEACCEPT aimPart=" + targetPart + " clientPicked=" + currentAim);
                });
                KeyMapping.click(mc.options.keyAttack.getKey());
                stage = 3; ticks = 0;
                return;
            }

            // Give the server time to process the attack packet and its damage.
            if (stage == 3) {
                if (ticks < 6) return;
                int targetPart = part;
                pending = server.submit(() -> {
                    var root = (GraveDragonEntity) server.overworld().getEntity(rootId);
                    if (root == null) return;
                    float damage = healthBefore - root.getHealth();
                    if (damage > 0) hits++; else misses++;
                    boolean aimMatched = currentAim.equals("part" + targetPart);
                    if (!aimMatched && damage > 0) wrongTarget++;
                    System.out.println("MELEEACCEPT aimPart=" + targetPart + " picked=" + currentAim
                            + " damage=" + String.format("%.2f", damage)
                            + (damage > 0 ? " OK" : " NO-DAMAGE"));
                });
                part++;
                cooldown = 12;
                stage = 2;
                ticks = 0;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            finish(mc);
        }
    }

    private static void finish(Minecraft mc) {
        if (finishing) return;
        finishing = true;
        System.out.println("MELEEACCEPT SUMMARY hits=" + hits + " noDamage=" + misses
                + " damageOnWrongTarget=" + wrongTarget + " parts=" + part);
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player.serverLevel().getEntity(rootId) instanceof GraveDragonEntity dragon) dragon.discard();
            player.connection.teleport(savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
            mc.execute(() -> {
                mc.player.getAbilities().flying = savedFlying;
                mc.player.onUpdateAbilities();
                System.out.println("MELEEACCEPT STOP");
                mc.stop();
            });
        });
    }
}
