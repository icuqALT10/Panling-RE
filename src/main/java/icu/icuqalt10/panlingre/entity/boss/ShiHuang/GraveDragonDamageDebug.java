package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

/**
 * Opt-in server-side tracing for the multipart melee chain.
 *
 * <p>Enabled with {@code -Dpanlingre.damageDebug=true}. The point is to see how far a
 * damage event travels — whether the part's hurt hook runs at all, which part the server
 * resolves, and whether the hit report actually leaves the server — because a silent chat
 * (no line for melee, projectiles or skills alike) can mean either "the event never
 * reached the funnel" or "the packet never left".
 */
public final class GraveDragonDamageDebug {
    private GraveDragonDamageDebug() {
    }

    /**
     * Checked at each call site rather than cached in a static final: the JIT would treat a
     * compile-time constant as dead code and erase the tracing entirely.
     */
    public static boolean enabled() {
        return Boolean.getBoolean("panlingre.damageDebug");
    }

    public static void log(String message) {
        if (enabled()) System.out.println("DRAGONDMG " + message);
    }
}
