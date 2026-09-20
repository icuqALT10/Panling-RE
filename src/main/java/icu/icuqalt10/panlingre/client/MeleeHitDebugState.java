package icu.icuqalt10.panlingre.client;

/**
 * Latest server-side melee resolution, kept only for the F3+B debug overlay.
 *
 * <p>Seeing which part the server actually used is what makes a "I clearly aimed at the
 * leg but nothing happened" report diagnosable: the overlay draws the server's part in a
 * distinct colour, so client picking errors and server resolution errors look different.
 */
public final class MeleeHitDebugState {
    private static volatile int requested = -1;
    private static volatile int struck = -1;
    private static volatile boolean fromRay;
    private static volatile long gameTime = Long.MIN_VALUE;

    private MeleeHitDebugState() {
    }

    public static void record(int requestedIndex, int struckIndex, boolean rayHit) {
        requested = requestedIndex;
        struck = struckIndex;
        fromRay = rayHit;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        gameTime = level == null ? Long.MIN_VALUE : level.getGameTime();
    }

    /** True while the most recent report is recent enough to be worth drawing. */
    public static boolean isFresh() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null && gameTime != Long.MIN_VALUE
                && level.getGameTime() - gameTime < 100;
    }

    public static int requested() {
        return requested;
    }

    public static int struck() {
        return struck;
    }

    public static boolean fromRay() {
        return fromRay;
    }
}
