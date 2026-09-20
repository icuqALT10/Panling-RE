package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 脊柱链的**同步载荷**：把 21 个节点的世界坐标打包成一个 {@link CompoundTag}。
 *
 * <p>为什么要同步链、而不是两侧各自跑物理：碰撞箱与渲染必须逐位一致（这是"所见即所得"的前提），
 * 而链是有状态的 verlet 积分——服务端的 {@code position()} 与客户端插值出来的位置并不相同，
 * 各自跑链一定会分歧。所以链只在服务端推进，把结果同步出去，客户端直接复用同一组坐标。
 *
 * <p>每节点 3 个 float，共 63 个。只按 {@link #SYNC_INTERVAL_TICKS} 的节奏发送（这条龙一秒才移动
 * 一两个方块，链的形状变化很慢），所以带宽可以忽略。
 */
public final class GraveDragonSpineSync {
    /** 每隔多少 tick 同步一次链（5Hz）。 */
    public static final int SYNC_INTERVAL_TICKS = 4;
    private static final String KEY = "Spine";

    private GraveDragonSpineSync() {
    }

    public static CompoundTag write(Vec3[] nodes) {
        ListTag list = new ListTag();
        for (Vec3 node : nodes) {
            list.add(FloatTag.valueOf((float) node.x));
            list.add(FloatTag.valueOf((float) node.y));
            list.add(FloatTag.valueOf((float) node.z));
        }
        CompoundTag tag = new CompoundTag();
        tag.put(KEY, list);
        return tag;
    }

    /** 读回节点；数据不完整（长度不是 3 的倍数）时返回 null，调用方应退回纯动画姿态。 */
    public static Vec3[] read(CompoundTag tag) {
        if (!tag.contains(KEY)) return null;
        ListTag list = tag.getList(KEY, 5); // 5 = float
        if (list.size() % 3 != 0 || list.size() == 0) return null;
        Vec3[] nodes = new Vec3[list.size() / 3];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Vec3(list.getFloat(i * 3), list.getFloat(i * 3 + 1), list.getFloat(i * 3 + 2));
        }
        return nodes;
    }

    public static List<Vec3> toList(Vec3[] nodes) {
        List<Vec3> list = new ArrayList<>(nodes.length);
        for (Vec3 node : nodes) list.add(node);
        return list;
    }
}
