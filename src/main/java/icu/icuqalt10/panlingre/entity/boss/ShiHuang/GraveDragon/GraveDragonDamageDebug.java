package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

/**
 * 部件伤害链路的调试追踪开关。
 *
 * <h2>这是哪个开关</h2>
 * 就是<b>部件伤害调试</b>那个：它追踪一次伤害事件走了多远——部件的 hurt 钩子有没有被调用、
 * 服务端把命中解析到了哪个部件、命中报告有没有真的发出去。开启后会在日志里打
 * {@code DRAGONDMG ...} 行，并在客户端把命中的部件高亮出来。
 *
 * <h2>怎么开</h2>
 * 先把这个类的 {@link #ENABLED} 改成 {@code true}，再用
 * {@code gradlew runClient -PdamageDebug} 启动（后者会设置系统属性
 * {@code -Dpanlingre.damageDebug=true}）。两个条件同时满足才会输出，
 * 所以默认状态下即使误带了 gradle 参数也不会刷日志。
 */
public final class GraveDragonDamageDebug {
    /**
     * 总开关。**默认 false**：日常游玩/演出时不需要这些日志，
     * 之前它会在每次命中时往日志里写行，干扰观感。
     */
    public static final boolean ENABLED = false;

    private GraveDragonDamageDebug() {
    }

    /**
     * 每次调用点都重新判断，而不是缓存成 static final 常量：
     * JIT 会把编译期常量当死代码，连带把整段追踪代码擦掉，那样调试起来反而失真。
     */
    public static boolean enabled() {
        return ENABLED && Boolean.getBoolean("panlingre.damageDebug");
    }

    public static void log(String message) {
        if (enabled()) System.out.println("DRAGONDMG " + message);
    }
}
