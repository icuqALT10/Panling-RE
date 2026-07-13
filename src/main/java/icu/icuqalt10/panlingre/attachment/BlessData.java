package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.other.bless_shengshou;
import icu.icuqalt10.panlingre.network.SyncBlessPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

public record BlessData(boolean qinglong, boolean zhuque, boolean baihu, boolean xuanwu) {

    public BlessData() {
        this(false, false, false, false);
    }

    public static final Codec<BlessData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("qinglong", false).forGetter(BlessData::qinglong),
            Codec.BOOL.optionalFieldOf("zhuque", false).forGetter(BlessData::zhuque),
            Codec.BOOL.optionalFieldOf("baihu", false).forGetter(BlessData::baihu),
            Codec.BOOL.optionalFieldOf("xuanwu", false).forGetter(BlessData::xuanwu)
    ).apply(instance, BlessData::new));

    public BlessData with(String type, boolean value) {
        return switch (type.toLowerCase()) {
            case "qinglong" -> new BlessData(value, zhuque, baihu, xuanwu);
            case "zhuque" -> new BlessData(qinglong, value, baihu, xuanwu);
            case "baihu" -> new BlessData(qinglong, zhuque, value, xuanwu);
            case "xuanwu" -> new BlessData(qinglong, zhuque, baihu, value);
            default -> this;
        };
    }

    public boolean get(String type) {
        return switch (type.toLowerCase()) {
            case "qinglong" -> qinglong;
            case "zhuque" -> zhuque;
            case "baihu" -> baihu;
            case "xuanwu" -> xuanwu;
            default -> false;
        };
    }

    //静态工具
    public static boolean hasBless(Player player, String blessType) {
        return player.getData(ModAttachments.BLESS.get()).get(blessType);
    }

    public static boolean addBless(Player player, String blessType) {
        BlessData current = player.getData(ModAttachments.BLESS.get());
        if (current.get(blessType)) {
            return false;
        }
        BlessData newData = current.with(blessType, true);
        player.setData(ModAttachments.BLESS.get(), newData);

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncBlessPayload(newData));
        }
        return true;
    }

    public static boolean removeBless(Player player, String blessType) {
        BlessData current = player.getData(ModAttachments.BLESS.get());
        if (!current.get(blessType)) {
            return false;
        }
        BlessData newData = current.with(blessType, false);
        player.setData(ModAttachments.BLESS.get(), newData);

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncBlessPayload(newData));
        }
        return true;
    }
}