package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import icu.icuqalt10.panlingre.network.GraveDragonHitPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side record of what the server actually damaged on a multipart boss, used for
 * both the chat report and the F3+B highlight.
 *
 * <p>This is what makes a report like "I aimed at the leg and nothing happened" readable
 * in game. Either no line appears at all, meaning the server rejected the attack, or the
 * line and the highlight name a different part than the one under the crosshair, meaning
 * the two sides disagree about what was hit.
 */
public final class GraveDragonDamageLog {
    /** How long a hit stays highlighted in the F3+B overlay, in ticks. */
    private static final int HIGHLIGHT_TICKS = 100;

    private record Pending(int entityId, String partLabel, float healthBefore) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private static volatile int lastEntityId = -1;
    private static volatile int lastPart = -1;
    private static volatile long lastHitTime = Long.MIN_VALUE;

    private GraveDragonDamageLog() {
    }

    /** Called from the payload handler when the server reports a landed hit. */
    public static void record(GraveDragonHitPayload payload) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null) return;
        if (!(level.getEntity(payload.entityId()) instanceof GraveDragonEntity dragon)) return;

        lastEntityId = payload.entityId();
        lastPart = payload.partIndex();
        lastHitTime = level.getGameTime();

        mc.player.displayClientMessage(Component.empty()
                .append(Component.literal("◆ 命中 ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(payload.partLabel() + " [#" + payload.partIndex() + "]")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  倍率 ×" + trim(payload.multiplier()))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("  扣血 " + trim(payload.amount()))
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal("  剩余 " + trim(dragon.getHealth()))
                        .withStyle(ChatFormatting.GRAY)), false);

        // Health can keep moving for a tick after the event, so the settled line is
        // measured on the next client tick.
        PENDING.add(new Pending(payload.entityId(), payload.partLabel(), dragon.getHealth()));
    }

    /** Called once per client tick; flushes deferred actual-damage lines. */
    public static void tick() {
        if (PENDING.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null) {
            PENDING.clear();
            return;
        }
        for (Pending pending : PENDING) {
            if (!(level.getEntity(pending.entityId()) instanceof GraveDragonEntity dragon)) continue;
            float actual = pending.healthBefore() - dragon.getHealth();
            mc.player.displayClientMessage(Component.empty()
                    .append(Component.literal("   └ " + pending.partLabel() + " 结算 ")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(trim(actual)).withStyle(ChatFormatting.RED))
                    .append(Component.literal("  剩 " + trim(dragon.getHealth()))
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        PENDING.clear();
    }

    /**
     * Part to highlight for the given dragon, or -1. The highlight follows the server's
     * verdict, so a mismatch with the crosshair is immediately visible.
     */
    public static int highlightPartFor(GraveDragonEntity dragon) {
        var level = Minecraft.getInstance().level;
        if (level == null || lastPart < 0 || lastEntityId != dragon.getId()) return -1;
        return level.getGameTime() - lastHitTime < HIGHLIGHT_TICKS ? lastPart : -1;
    }

    private static String trim(float value) {
        if (Float.isNaN(value)) return "?";
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}
