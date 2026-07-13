package icu.icuqalt10.panlingre.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface skill_1_key {
     boolean skill_1_trigger(Level level, Player player, ItemStack stack);

    default long getCD_11(String skillId) {
        return switch (skillId) {
            case "cd.panlingre:teng_mu_gong.skill_1" -> 5000L;
            case "cd.panlingre:jing_tie_gong.skill_1" -> 5000L;
            case "cd.panlingre:hei_tie_nu.skill_1" -> 500L;
            case "cd.panlingre:yan_tie_gong.skill_1" -> 5000L;
            case "cd.panlingre:hong_ling_nu.skill_1" -> 500L;
            case "cd.panlingre:zhong_chui_gong.skill_1" -> 5000L;
            case "cd.panlingre:jiao_long_nu.skill_1" -> 500L;
            case "cd.panlingre:bei_dou_gong.skill_1" -> 5000L;
            case "cd.panlingre:liu_xing_nu.skill_1" -> 10000L;
            case "cd.panlingre:huang_tong_lu.skill_1" -> 2000L;
            case "cd.panlingre:jing_tie_lu.skill_1" -> 2000L;
            case "cd.panlingre:chi_tong_lu.skill_1" -> 2000L;
            case "cd.panlingre:suo_hun_lu.skill_1" -> 2000L;
            case "cd.panlingre:qi_sha_din.skill_1" -> 2000L;
            default -> 1000L;
        };
    }
}
