package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PanlingRE.MODID);

    private static ItemStack makeLootKey(String keyType, String keyId) {
        ItemStack stack = new ItemStack(ModItems.loot_key.get());
        stack.set(ModComponents.KEY_TYPE.get(), keyType);
        stack.set(ModComponents.KEY_ID.get(), keyId);
        return stack;
    }

    private static ItemStack makeLootChest(String chestType, String chestId, String lootTableId) {
        ItemStack stack = new ItemStack(ModItems.loot_chest.get());
        CompoundTag beData = new CompoundTag();
        beData.putString("id", "panlingre:loot_chest_be");
        beData.putString("chestType", chestType);
        beData.putString("chestId", chestId);
        beData.putString("lootTableId", lootTableId);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beData));
        return stack;
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> other =
            CREATIVE_MODE_TABS.register("other", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.PanlingRE.item_tab.other"))
                    .icon(() -> new ItemStack(ModItems.race_ren.get()))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.tong_qian.get());
                        output.accept(ModItems.yuan_bao.get());
                        output.accept(ModItems.yin_piao.get());
                        output.accept(ModItems.ys_dhq.get());

                        output.accept(ModItems.ldl.get());
                        output.accept(ModItems.zft.get());
                        output.accept(ModItems.dzt.get());

                        output.accept(ModItems.suspicious_dirt.get());
                        output.accept(ModItems.zhu_sha_ore.get());
                        output.accept(ModItems.jing_tie_ore.get());
                        output.accept(ModItems.chi_tong_ore.get());
                        output.accept(ModItems.jin_jing_ore.get());
                        output.accept(ModItems.xuan_tie_ore.get());
                        output.accept(ModItems.bai_yu_ore.get());
                        output.accept(ModItems.ling_yu_ore.get());
                        output.accept(ModItems.bi_hai_ore.get());
                        output.accept(ModItems.bamboo.get());

                        // 默认钥匙（空key_type，空key_id）
                        output.accept(ModItems.loot_key.get());
                        // 圣山金钥匙示例
                        output.accept(makeLootKey("golden", "sheng_shan"));

                        // 默认宝箱
                        output.accept(ModItems.loot_chest.get());
                        // 圣山金宝箱示例
                        output.accept(makeLootChest("golden", "sheng_shan", "test"));
                    })
                    .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> material =
            CREATIVE_MODE_TABS.register("material", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.PanlingRE.item_tab.material"))
                    .icon(() -> new ItemStack(ModItems.rare2_yuanhe.get()))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.ys1_jin.get());
                        output.accept(ModItems.ys1_mu.get());
                        output.accept(ModItems.ys1_shui.get());
                        output.accept(ModItems.ys1_huo.get());
                        output.accept(ModItems.ys1_tu.get());
                        output.accept(ModItems.ys2_jin.get());
                        output.accept(ModItems.ys2_mu.get());
                        output.accept(ModItems.ys2_shui.get());
                        output.accept(ModItems.ys2_huo.get());
                        output.accept(ModItems.ys2_tu.get());
                        output.accept(ModItems.ys3_jin.get());
                        output.accept(ModItems.ys3_mu.get());
                        output.accept(ModItems.ys3_shui.get());
                        output.accept(ModItems.ys3_huo.get());
                        output.accept(ModItems.ys3_tu.get());

                        output.accept(ModItems.yao_yin_1.get());
                        output.accept(ModItems.yao_yin_2.get());
                        output.accept(ModItems.yao_yin_3.get());
                        output.accept(ModItems.yao_yin_4.get());

                        output.accept(ModItems.he_shou_wu.get());
                        output.accept(ModItems.ling_zhi.get());
                        output.accept(ModItems.ren_shen.get());
                        output.accept(ModItems.lan_cao.get());
                        output.accept(ModItems.zhen_zhu.get());
                        output.accept(ModItems.pu_ti_zi.get());

                        output.accept(ModItems.zhu_sha_1.get());
                        output.accept(ModItems.zhu_sha_2.get());
                        output.accept(ModItems.zhu_sha_3.get());
                        output.accept(ModItems.zhu_sha_4.get());
                        output.accept(ModItems.fu_zhi.get());

                        output.accept(ModItems.rare2_yuanhe_fra.get());
                        output.accept(ModItems.rare2_yuanhe.get());
                        output.accept(ModItems.jing_shou_pi.get());
                        output.accept(ModItems.jing_tie.get());

                        output.accept(ModItems.rare3_yuanhe_fra.get());
                        output.accept(ModItems.rare3_yuanhe.get());
                        output.accept(ModItems.chi_tong_ding.get());
                        output.accept(ModItems.jin_jing.get());

                        output.accept(ModItems.rare4_yuanhe_fra.get());
                        output.accept(ModItems.rare4_yuanhe.get());
                        output.accept(ModItems.xuan_tie.get());
                        output.accept(ModItems.bai_yu_jing.get());

                        output.accept(ModItems.rare5_yuanhe_fra.get());
                        output.accept(ModItems.rare5_yuanhe.get());
                        output.accept(ModItems.ling_yu_jian.get());
                        output.accept(ModItems.bi_hai_zhu.get());
                    })
                    .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> weapon =
            CREATIVE_MODE_TABS.register("weapon", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.PanlingRE.item_tab.weapon"))
                    .icon(() -> new ItemStack(ModItems.hun_yuan_shen_din.get()))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.warrior.get());
                        output.accept(ModItems.archer.get());
                        output.accept(ModItems.warlock.get());

                        output.accept(ModItems.ba_fang_yi.get());

                        output.accept(ModItems.race_ren.get());
                        output.accept(ModItems.race_shen.get());
                        output.accept(ModItems.race_xian.get());
                        output.accept(ModItems.race_yao.get());
                        output.accept(ModItems.race_zhan.get());
                        output.accept(ModItems.bless_shengshou.get());

                        output.accept(ModItems.qing_ying_feather.get());
                        output.accept(ModItems.shen_su_feather.get());
                        output.accept(ModItems.ye_ming_zhu.get());
                        output.accept(ModItems.qian_jin_suo.get());
                        output.accept(ModItems.kong_ming_shi.get());
                        output.accept(ModItems.shen_su_fu.get());
                        output.accept(ModItems.ling_shi.get());

                        output.accept(ModItems.tao_mu_jian.get());
                        output.accept(ModItems.kai_shan_dao.get());
                        output.accept(ModItems.jing_tie_jian.get());
                        output.accept(ModItems.chi_tong_jian.get());
                        output.accept(ModItems.ju_tong_chui.get());
                        output.accept(ModItems.po_kong_fu.get());
                        output.accept(ModItems.tai_ji_jian.get());
                        output.accept(ModItems.yu_ru_yi.get());
                        output.accept(ModItems.fei_xian_jian.get());
                        output.accept(ModItems.ding_hai_shen_zhen.get());

                        output.accept(ModItems.teng_mu_gong.get());
                        output.accept(ModItems.jing_tie_gong.get());
                        output.accept(ModItems.hei_tie_nu.get());
                        output.accept(ModItems.yan_tie_gong.get());
                        output.accept(ModItems.hong_ling_nu.get());
                        output.accept(ModItems.zhong_chui_gong.get());
                        output.accept(ModItems.jiao_long_nu.get());
                        output.accept(ModItems.bei_dou_gong.get());
                        output.accept(ModItems.liu_xing_nu.get());
                        output.accept(ModItems.zhu_ri.get());

                        output.accept(ModItems.huang_tong_lu.get());
                        output.accept(ModItems.jing_tie_lu.get());
                        output.accept(ModItems.chi_tong_lu.get());
                        output.accept(ModItems.suo_hun_lu.get());
                        output.accept(ModItems.qi_sha_din.get());
                        output.accept(ModItems.hun_yuan_shen_din.get());
                    })
                    .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> armor =
            CREATIVE_MODE_TABS.register("armor", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.PanlingRE.item_tab.armor"))
                    .icon(() -> new ItemStack(ModItems.chu_xin_helmet.get()))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.chu_xin_helmet.get());
                        output.accept(ModItems.chu_xin_chestplate.get());
                        output.accept(ModItems.chu_xin_leggings.get());
                        output.accept(ModItems.chu_xin_boots.get());

                        output.accept(ModItems.zhan_lang_helmet.get());
                        output.accept(ModItems.zhan_lang_chestplate.get());
                        output.accept(ModItems.zhan_lang_leggings.get());
                        output.accept(ModItems.zhan_lang_boots.get());

                        output.accept(ModItems.xiong_shou_helmet.get());
                        output.accept(ModItems.xiong_shou_chestplate.get());
                        output.accept(ModItems.xiong_shou_leggings.get());
                        output.accept(ModItems.xiong_shou_boots.get());

                        output.accept(ModItems.ling_gui_helmet.get());
                        output.accept(ModItems.ling_gui_chestplate.get());
                        output.accept(ModItems.ling_gui_leggings.get());
                        output.accept(ModItems.ling_gui_boots.get());

                        output.accept(ModItems.long_lin_helmet.get());
                        output.accept(ModItems.long_lin_chestplate.get());
                        output.accept(ModItems.long_lin_leggings.get());
                        output.accept(ModItems.long_lin_boots.get());

                        output.accept(ModItems.feng_xing_helmet.get());
                        output.accept(ModItems.feng_xing_chestplate.get());
                        output.accept(ModItems.feng_xing_leggings.get());
                        output.accept(ModItems.feng_xing_boots.get());

                        output.accept(ModItems.hu_pi_helmet.get());
                        output.accept(ModItems.hu_pi_chestplate.get());
                        output.accept(ModItems.hu_pi_leggings.get());
                        output.accept(ModItems.hu_pi_boots.get());

                        output.accept(ModItems.she_lin_helmet.get());
                        output.accept(ModItems.she_lin_chestplate.get());
                        output.accept(ModItems.she_lin_leggings.get());
                        output.accept(ModItems.she_lin_boots.get());

                        output.accept(ModItems.chi_ying_helmet.get());
                        output.accept(ModItems.chi_ying_chestplate.get());
                        output.accept(ModItems.chi_ying_leggings.get());
                        output.accept(ModItems.chi_ying_boots.get());

                        output.accept(ModItems.gong_fu_helmet.get());
                        output.accept(ModItems.gong_fu_chestplate.get());
                        output.accept(ModItems.gong_fu_leggings.get());
                        output.accept(ModItems.gong_fu_boots.get());

                        output.accept(ModItems.hua_tuo_helmet.get());
                        output.accept(ModItems.hua_tuo_chestplate.get());
                        output.accept(ModItems.hua_tuo_leggings.get());
                        output.accept(ModItems.hua_tuo_boots.get());

                        output.accept(ModItems.cao_yao_helmet.get());
                        output.accept(ModItems.cao_yao_chestplate.get());
                        output.accept(ModItems.cao_yao_leggings.get());
                        output.accept(ModItems.cao_yao_boots.get());

                        output.accept(ModItems.yun_xing_helmet.get());
                        output.accept(ModItems.yun_xing_chestplate.get());
                        output.accept(ModItems.yun_xing_leggings.get());
                        output.accept(ModItems.yun_xing_boots.get());
                    })
                    .build()
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> potion =
            CREATIVE_MODE_TABS.register("potion", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.PanlingRE.item_tab.potion"))
                    .icon(() -> new ItemStack(Items.POTION))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.he_ding_dan.get());
                        output.accept(ModItems.wan_ling.get());
                        output.accept(ModItems.luo_shen.get());
                        output.accept(ModItems.hun_yuan_1.get());
                        output.accept(ModItems.hun_yuan_2.get());
                        output.accept(ModItems.hun_yuan_3.get());
                        output.accept(ModItems.pi_gu_1.get());
                        output.accept(ModItems.pi_gu_2.get());
                        output.accept(ModItems.pi_gu_3.get());
                        output.accept(ModItems.hui_chun_1.get());
                        output.accept(ModItems.hui_chun_2.get());
                        output.accept(ModItems.hui_chun_3.get());
                        output.accept(ModItems.tian_shen_1.get());
                        output.accept(ModItems.tian_shen_2.get());
                        output.accept(ModItems.tian_shen_3.get());
                        output.accept(ModItems.jiu_zhuan_1.get());
                        output.accept(ModItems.jiu_zhuan_2.get());
                        output.accept(ModItems.jiu_zhuan_3.get());
                        output.accept(ModItems.qi_si.get());
                        output.accept(ModItems.feng_hou_1.get());
                        output.accept(ModItems.feng_hou_2.get());
                        output.accept(ModItems.feng_hou_3.get());
                        output.accept(ModItems.jian_xue.get());

                        output.accept(ModItems.po_di_fu.get());
                        output.accept(ModItems.shou_yu_fu.get());
                        output.accept(ModItems.tui_huo_fu.get());
                        output.accept(ModItems.ju_li_fu.get());
                    })
                    .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
