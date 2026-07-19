package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.item.archer.*;
import icu.icuqalt10.panlingre.item.archer.armor.chi_ying;
import icu.icuqalt10.panlingre.item.archer.armor.feng_xing;
import icu.icuqalt10.panlingre.item.archer.armor.hu_pi;
import icu.icuqalt10.panlingre.item.archer.armor.she_lin;
import icu.icuqalt10.panlingre.item.common.armor.*;
import icu.icuqalt10.panlingre.item.fuzhi.ju_li_fu;
import icu.icuqalt10.panlingre.item.fuzhi.po_di_fu;
import icu.icuqalt10.panlingre.item.fuzhi.shou_yu_fu;
import icu.icuqalt10.panlingre.item.fuzhi.tui_huo_fu;
import icu.icuqalt10.panlingre.item.other.*;
import icu.icuqalt10.panlingre.item.potions.*;
import icu.icuqalt10.panlingre.item.race.*;
import icu.icuqalt10.panlingre.item.warlock.*;
import icu.icuqalt10.panlingre.item.warlock.armor.cao_yao;
import icu.icuqalt10.panlingre.item.warlock.armor.gong_fu;
import icu.icuqalt10.panlingre.item.warlock.armor.hua_tuo;
import icu.icuqalt10.panlingre.item.warlock.armor.yun_xing;
import icu.icuqalt10.panlingre.item.warrior.*;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.item.warrior.armor.ling_gui;
import icu.icuqalt10.panlingre.item.warrior.armor.long_lin;
import icu.icuqalt10.panlingre.item.warrior.armor.xiong_shou;
import icu.icuqalt10.panlingre.item.warrior.armor.zhan_lang;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PanlingRE.MODID);

    //其他
    public static final DeferredItem<Item> tong_qian = ITEMS.register("tong_qian",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> yuan_bao = ITEMS.register("yuan_bao",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> yin_piao = ITEMS.register("yin_piao",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys_dhq = ITEMS.register("ys_dhq",() -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> qing_ying_feather = ITEMS.register("qing_ying_feather", qing_ying_feather::new);
    public static final DeferredItem<Item> shen_su_feather = ITEMS.register("shen_su_feather", shen_su_feather::new);
    public static final DeferredItem<Item> ye_ming_zhu = ITEMS.register("ye_ming_zhu", ye_ming_zhu::new);
    public static final DeferredItem<Item> qian_jin_suo = ITEMS.register("qian_jin_suo", qian_jin_suo::new);
    public static final DeferredItem<Item> kong_ming_shi = ITEMS.register("kong_ming_shi", kong_ming_shi::new);
    public static final DeferredItem<Item> shen_su_fu = ITEMS.register("shen_su_fu", shen_su_fu::new);
    public static final DeferredItem<Item> ling_shi = ITEMS.register("ling_shi", ling_shi::new);

    public static final DeferredItem<BlockItem> ldl = ITEMS.register("ldl",
            () -> new BlockItem(ModBlocks.ldl.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> zft = ITEMS.register("zft",
            () -> new BlockItem(ModBlocks.zft.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> dzt = ITEMS.register("dzt",
            () -> new BlockItem(ModBlocks.dzt.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> zhu_sha_ore = ITEMS.register("zhu_sha_ore",
            () -> new BlockItem(ModBlocks.zhu_sha_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> jing_tie_ore = ITEMS.register("jing_tie_ore",
            () -> new BlockItem(ModBlocks.jing_tie_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> chi_tong_ore = ITEMS.register("chi_tong_ore",
            () -> new BlockItem(ModBlocks.chi_tong_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> jin_jing_ore = ITEMS.register("jin_jing_ore",
            () -> new BlockItem(ModBlocks.jin_jing_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> xuan_tie_ore = ITEMS.register("xuan_tie_ore",
            () -> new BlockItem(ModBlocks.xuan_tie_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> bai_yu_ore = ITEMS.register("bai_yu_ore",
            () -> new BlockItem(ModBlocks.bai_yu_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ling_yu_ore = ITEMS.register("ling_yu_ore",
            () -> new BlockItem(ModBlocks.ling_yu_ore.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> bi_hai_ore = ITEMS.register("bi_hai_ore",
            () -> new BlockItem(ModBlocks.bi_hai_ore.get(), new Item.Properties()));
    public static final DeferredItem<Item> bamboo = ITEMS.register("bamboo",
            () -> new BlockItem(ModBlocks.bamboo_block.get(), new Item.Properties()));
    public static final DeferredItem<Item> suspicious_dirt = ITEMS.register("suspicious_dirt",
            () -> new BlockItem(ModBlocks.suspicious_dirt.get(), new Item.Properties()));

    //材料
    public static final DeferredItem<Item> ys1_jin = ITEMS.register("ys1_jin",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys1_mu = ITEMS.register("ys1_mu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys1_shui = ITEMS.register("ys1_shui",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys1_huo = ITEMS.register("ys1_huo",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys1_tu = ITEMS.register("ys1_tu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys2_jin = ITEMS.register("ys2_jin",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys2_mu = ITEMS.register("ys2_mu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys2_shui = ITEMS.register("ys2_shui",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys2_huo = ITEMS.register("ys2_huo",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys2_tu = ITEMS.register("ys2_tu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys3_jin = ITEMS.register("ys3_jin",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys3_mu = ITEMS.register("ys3_mu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys3_shui = ITEMS.register("ys3_shui",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys3_huo = ITEMS.register("ys3_huo",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ys3_tu = ITEMS.register("ys3_tu",() -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> rare2_yuanhe_fra = ITEMS.register("rare2_yuanhe_fra",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare2_yuanhe_fra.lore1"),
                    Component.translatable("item.panlingre.rare2_yuanhe_fra.lore2")
            )))));
    public static final DeferredItem<Item> rare2_yuanhe = ITEMS.register("rare2_yuanhe",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare2_yuanhe.lore1"),
                    Component.translatable("item.panlingre.rare2_yuanhe.lore2")
            )))));
    public static final DeferredItem<Item> jing_tie = ITEMS.register("jing_tie",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.jing_tie.lore1"),
            Component.translatable("item.panlingre.jing_tie.lore2")
    )))));
    public static final DeferredItem<Item> jing_shou_pi = ITEMS.register("jing_shou_pi",() -> new Item(new Item.Properties()
            .component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.jing_shou_pi.lore1"),
            Component.translatable("item.panlingre.jing_shou_pi.lore2")
    )))));


    public static final DeferredItem<Item> rare3_yuanhe_fra = ITEMS.register("rare3_yuanhe_fra",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare3_yuanhe_fra.lore1"),
                    Component.translatable("item.panlingre.rare3_yuanhe_fra.lore2")
            )))));
    public static final DeferredItem<Item> rare3_yuanhe = ITEMS.register("rare3_yuanhe",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare3_yuanhe.lore1"),
                    Component.translatable("item.panlingre.rare3_yuanhe.lore2")
            )))));
    public static final DeferredItem<Item> chi_tong_ding = ITEMS.register("chi_tong_ding",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.chi_tong_ding.lore1"),
            Component.translatable("item.panlingre.chi_tong_ding.lore2")
    )))));
    public static final DeferredItem<Item> jin_jing = ITEMS.register("jin_jing",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.jin_jing.lore1"),
            Component.translatable("item.panlingre.jin_jing.lore2")
    )))));


    public static final DeferredItem<Item> rare4_yuanhe_fra = ITEMS.register("rare4_yuanhe_fra",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare4_yuanhe_fra.lore1"),
                    Component.translatable("item.panlingre.rare4_yuanhe_fra.lore2")
            )))));
    public static final DeferredItem<Item> rare4_yuanhe = ITEMS.register("rare4_yuanhe",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare4_yuanhe.lore1"),
                    Component.translatable("item.panlingre.rare4_yuanhe.lore2")
            )))));
    public static final DeferredItem<Item> xuan_tie = ITEMS.register("xuan_tie",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.xuan_tie.lore1"),
            Component.translatable("item.panlingre.xuan_tie.lore2")
    )))));
    public static final DeferredItem<Item> bai_yu_jing = ITEMS.register("bai_yu_jing",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.bai_yu_jing.lore1"),
            Component.translatable("item.panlingre.bai_yu_jing.lore2")
    )))));


    public static final DeferredItem<Item> rare5_yuanhe_fra = ITEMS.register("rare5_yuanhe_fra",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare5_yuanhe_fra.lore1"),
                    Component.translatable("item.panlingre.rare5_yuanhe_fra.lore2")
            )))));
    public static final DeferredItem<Item> rare5_yuanhe = ITEMS.register("rare5_yuanhe",
            () -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.panlingre.rare5_yuanhe.lore1"),
                    Component.translatable("item.panlingre.rare5_yuanhe.lore2")
            )))));
    public static final DeferredItem<Item> ling_yu_jian = ITEMS.register("ling_yu_jian",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.ling_yu_jian.lore1"),
            Component.translatable("item.panlingre.ling_yu_jian.lore2")
    )))));
    public static final DeferredItem<Item> bi_hai_zhu = ITEMS.register("bi_hai_zhu",() -> new Item(new Item.Properties().component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.panlingre.bi_hai_zhu.lore1"),
            Component.translatable("item.panlingre.bi_hai_zhu.lore2")
    )))));

    public static final DeferredItem<Item> yao_yin_1 = ITEMS.register("yao_yin_1", yao_yin::new);
    public static final DeferredItem<Item> yao_yin_2 = ITEMS.register("yao_yin_2", yao_yin::new);
    public static final DeferredItem<Item> yao_yin_3 = ITEMS.register("yao_yin_3", yao_yin::new);
    public static final DeferredItem<Item> yao_yin_4 = ITEMS.register("yao_yin_4", yao_yin::new);

    public static final DeferredItem<Item> zhu_sha_1 = ITEMS.register("zhu_sha_1", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> zhu_sha_2 = ITEMS.register("zhu_sha_2", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> zhu_sha_3 = ITEMS.register("zhu_sha_3", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> zhu_sha_4 = ITEMS.register("zhu_sha_4", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> fu_zhi = ITEMS.register("fu_zhi", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> he_shou_wu = ITEMS.register("he_shou_wu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ling_zhi = ITEMS.register("ling_zhi",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ren_shen = ITEMS.register("ren_shen",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> lan_cao = ITEMS.register("lan_cao",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> zhen_zhu = ITEMS.register("zhen_zhu",() -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> pu_ti_zi = ITEMS.register("pu_ti_zi",() -> new Item(new Item.Properties()));

    //符纸
    public static final DeferredItem<Item> po_di_fu = ITEMS.register("po_di_fu", po_di_fu::new);
    public static final DeferredItem<Item> shou_yu_fu = ITEMS.register("shou_yu_fu", shou_yu_fu::new);
    public static final DeferredItem<Item> tui_huo_fu = ITEMS.register("tui_huo_fu", tui_huo_fu::new);
    public static final DeferredItem<Item> ju_li_fu = ITEMS.register("ju_li_fu", ju_li_fu::new);


    //丹药
    public static final DeferredItem<Item> he_ding_dan = ITEMS.register("he_ding_dan", he_ding_dan::new);
    public static final DeferredItem<Item> wan_ling = ITEMS.register("wan_ling", wan_ling::new);
    public static final DeferredItem<Item> luo_shen = ITEMS.register("luo_shen", luo_shen::new);
    public static final DeferredItem<Item> hun_yuan_1 = ITEMS.register("hun_yuan_1", hun_yuan_1::new);
    public static final DeferredItem<Item> hun_yuan_2 = ITEMS.register("hun_yuan_2", hun_yuan_2::new);
    public static final DeferredItem<Item> hun_yuan_3 = ITEMS.register("hun_yuan_3", hun_yuan_3::new);
    public static final DeferredItem<Item> pi_gu_1 = ITEMS.register("pi_gu_1", pi_gu_1::new);
    public static final DeferredItem<Item> pi_gu_2 = ITEMS.register("pi_gu_2", pi_gu_2::new);
    public static final DeferredItem<Item> pi_gu_3 = ITEMS.register("pi_gu_3", pi_gu_3::new);
    public static final DeferredItem<Item> hui_chun_1 = ITEMS.register("hui_chun_1", hui_chun_1::new);
    public static final DeferredItem<Item> hui_chun_2 = ITEMS.register("hui_chun_2", hui_chun_2::new);
    public static final DeferredItem<Item> hui_chun_3 = ITEMS.register("hui_chun_3", hui_chun_3::new);
    public static final DeferredItem<Item> tian_shen_1 = ITEMS.register("tian_shen_1", tian_shen_1::new);
    public static final DeferredItem<Item> tian_shen_2 = ITEMS.register("tian_shen_2", tian_shen_2::new);
    public static final DeferredItem<Item> tian_shen_3 = ITEMS.register("tian_shen_3", tian_shen_3::new);
    public static final DeferredItem<Item> jiu_zhuan_1 = ITEMS.register("jiu_zhuan_1", jiu_zhuan_1::new);
    public static final DeferredItem<Item> jiu_zhuan_2 = ITEMS.register("jiu_zhuan_2", jiu_zhuan_2::new);
    public static final DeferredItem<Item> jiu_zhuan_3 = ITEMS.register("jiu_zhuan_3", jiu_zhuan_3::new);
    public static final DeferredItem<Item> qi_si = ITEMS.register("qi_si", qi_si::new);
    public static final DeferredItem<Item> feng_hou_1 = ITEMS.register("feng_hou_1", feng_hou_1::new);
    public static final DeferredItem<Item> feng_hou_2 = ITEMS.register("feng_hou_2", feng_hou_2::new);
    public static final DeferredItem<Item> feng_hou_3 = ITEMS.register("feng_hou_3", feng_hou_3::new);
    public static final DeferredItem<Item> jian_xue = ITEMS.register("jian_xue", jian_xue::new);

    //武器
    public static final DeferredItem<Item> warrior =
            ITEMS.register("warrior", warrior::new);
    public static final DeferredItem<Item> archer =
            ITEMS.register("archer", archer::new);
    public static final DeferredItem<Item> warlock =
            ITEMS.register("warlock", warlock::new);
    public static final DeferredItem<Item> race_ren =
            ITEMS.register("race_ren", race_ren::new);
    public static final DeferredItem<Item> race_shen =
            ITEMS.register("race_shen", race_shen::new);
    public static final DeferredItem<Item> race_xian =
            ITEMS.register("race_xian", race_xian::new);
    public static final DeferredItem<Item> race_yao =
            ITEMS.register("race_yao", race_yao::new);
    public static final DeferredItem<Item> race_zhan =
            ITEMS.register("race_zhan", race_zhan::new);
    public static final DeferredItem<Item> bless_shengshou =
            ITEMS.register("bless_shengshou", bless_shengshou::new);

    public static final DeferredItem<Item> tao_mu_jian =
            ITEMS.register("tao_mu_jian", tao_mu_jian::new);
    public static final DeferredItem<Item> kai_shan_dao =
            ITEMS.register("kai_shan_dao", kai_shan_dao::new);
    public static final DeferredItem<Item> jing_tie_jian =
            ITEMS.register("jing_tie_jian", jing_tie_jian::new);
    public static final DeferredItem<Item> chi_tong_jian =
            ITEMS.register("chi_tong_jian", chi_tong_jian::new);
    public static final DeferredItem<Item> ju_tong_chui =
            ITEMS.register("ju_tong_chui", ju_tong_chui::new);
    public static final DeferredItem<Item> po_kong_fu =
            ITEMS.register("po_kong_fu", po_kong_fu::new);
    public static final DeferredItem<Item> tai_ji_jian =
            ITEMS.register("tai_ji_jian", tai_ji_jian::new);
    public static final DeferredItem<Item> yu_ru_yi =
            ITEMS.register("yu_ru_yi", yu_ru_yi::new);
    public static final DeferredItem<Item> fei_xian_jian =
            ITEMS.register("fei_xian_jian", fei_xian_jian::new);
    public static final DeferredItem<Item> ding_hai_shen_zhen =
            ITEMS.register("ding_hai_shen_zhen", ding_hai_shen_zhen::new);

    public static final DeferredItem<Item> teng_mu_gong =
            ITEMS.register("teng_mu_gong", teng_mu_gong::new);
    public static final DeferredItem<Item> jing_tie_gong =
            ITEMS.register("jing_tie_gong", jing_tie_gong::new);
    public static final DeferredItem<Item> hei_tie_nu =
            ITEMS.register("hei_tie_nu", hei_tie_nu::new);
    public static final DeferredItem<Item> yan_tie_gong =
            ITEMS.register("yan_tie_gong", yan_tie_gong::new);
    public static final DeferredItem<Item> hong_ling_nu =
            ITEMS.register("hong_ling_nu", hong_ling_nu::new);
    public static final DeferredItem<Item> zhong_chui_gong =
            ITEMS.register("zhong_chui_gong", zhong_chui_gong::new);
    public static final DeferredItem<Item> jiao_long_nu =
            ITEMS.register("jiao_long_nu", jiao_long_nu::new);
    public static final DeferredItem<Item> bei_dou_gong =
            ITEMS.register("bei_dou_gong", bei_dou_gong::new);
    public static final DeferredItem<Item> liu_xing_nu =
            ITEMS.register("liu_xing_nu", liu_xing_nu::new);
    public static final DeferredItem<Item> zhu_ri =
            ITEMS.register("zhu_ri", zhu_ri::new);

    public static final DeferredItem<Item> huang_tong_lu =
            ITEMS.register("huang_tong_lu", huang_tong_lu::new);
    public static final DeferredItem<Item> jing_tie_lu =
            ITEMS.register("jing_tie_lu", jing_tie_lu::new);
    public static final DeferredItem<Item> chi_tong_lu =
            ITEMS.register("chi_tong_lu", chi_tong_lu::new);
    public static final DeferredItem<Item> suo_hun_lu =
            ITEMS.register("suo_hun_lu", suo_hun_lu::new);
    public static final DeferredItem<Item> qi_sha_din =
            ITEMS.register("qi_sha_din", qi_sha_din::new);
    public static final DeferredItem<Item> hun_yuan_shen_din =
            ITEMS.register("hun_yuan_shen_din", hun_yuan_shen_din::new);

    //防具
    public static final DeferredItem<Item> chu_xin_helmet =
            ITEMS.register("chu_xin_helmet", () -> new chu_xin(ModArmorMaterials.LEATHER,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> chu_xin_chestplate =
            ITEMS.register("chu_xin_chestplate", () -> new chu_xin(ModArmorMaterials.LEATHER,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> chu_xin_leggings =
            ITEMS.register("chu_xin_leggings", () -> new chu_xin(ModArmorMaterials.LEATHER,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> chu_xin_boots =
            ITEMS.register("chu_xin_boots", () -> new chu_xin(ModArmorMaterials.LEATHER,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> zhan_lang_helmet =
            ITEMS.register("zhan_lang_helmet", () -> new zhan_lang(ModArmorMaterials.WARRIOR,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> zhan_lang_chestplate =
            ITEMS.register("zhan_lang_chestplate", () -> new zhan_lang(ModArmorMaterials.WARRIOR,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> zhan_lang_leggings =
            ITEMS.register("zhan_lang_leggings", () -> new zhan_lang(ModArmorMaterials.WARRIOR,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> zhan_lang_boots =
            ITEMS.register("zhan_lang_boots", () -> new zhan_lang(ModArmorMaterials.WARRIOR,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> xiong_shou_helmet =
            ITEMS.register("xiong_shou_helmet", () -> new xiong_shou(ModArmorMaterials.WARRIOR,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> xiong_shou_chestplate =
            ITEMS.register("xiong_shou_chestplate", () -> new xiong_shou(ModArmorMaterials.WARRIOR,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> xiong_shou_leggings =
            ITEMS.register("xiong_shou_leggings", () -> new xiong_shou(ModArmorMaterials.WARRIOR,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> xiong_shou_boots =
            ITEMS.register("xiong_shou_boots", () -> new xiong_shou(ModArmorMaterials.WARRIOR,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> ling_gui_helmet =
            ITEMS.register("ling_gui_helmet", () -> new ling_gui(ModArmorMaterials.WARRIOR,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> ling_gui_chestplate =
            ITEMS.register("ling_gui_chestplate", () -> new ling_gui(ModArmorMaterials.WARRIOR,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> ling_gui_leggings =
            ITEMS.register("ling_gui_leggings", () -> new ling_gui(ModArmorMaterials.WARRIOR,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> ling_gui_boots =
            ITEMS.register("ling_gui_boots", () -> new ling_gui(ModArmorMaterials.WARRIOR,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> long_lin_helmet =
            ITEMS.register("long_lin_helmet", () -> new long_lin(ModArmorMaterials.WARRIOR,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> long_lin_chestplate =
            ITEMS.register("long_lin_chestplate", () -> new long_lin(ModArmorMaterials.WARRIOR,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> long_lin_leggings =
            ITEMS.register("long_lin_leggings", () -> new long_lin(ModArmorMaterials.WARRIOR,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> long_lin_boots =
            ITEMS.register("long_lin_boots", () -> new long_lin(ModArmorMaterials.WARRIOR,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> feng_xing_helmet =
            ITEMS.register("feng_xing_helmet", () -> new feng_xing(ModArmorMaterials.ARCHER,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> feng_xing_chestplate =
            ITEMS.register("feng_xing_chestplate", () -> new feng_xing(ModArmorMaterials.ARCHER,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> feng_xing_leggings =
            ITEMS.register("feng_xing_leggings", () -> new feng_xing(ModArmorMaterials.ARCHER,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> feng_xing_boots =
            ITEMS.register("feng_xing_boots", () -> new feng_xing(ModArmorMaterials.ARCHER,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> hu_pi_helmet =
            ITEMS.register("hu_pi_helmet", () -> new hu_pi(ModArmorMaterials.ARCHER,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> hu_pi_chestplate =
            ITEMS.register("hu_pi_chestplate", () -> new hu_pi(ModArmorMaterials.ARCHER,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> hu_pi_leggings =
            ITEMS.register("hu_pi_leggings", () -> new hu_pi(ModArmorMaterials.ARCHER,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> hu_pi_boots =
            ITEMS.register("hu_pi_boots", () -> new hu_pi(ModArmorMaterials.ARCHER,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> she_lin_helmet =
            ITEMS.register("she_lin_helmet", () -> new she_lin(ModArmorMaterials.ARCHER,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> she_lin_chestplate =
            ITEMS.register("she_lin_chestplate", () -> new she_lin(ModArmorMaterials.ARCHER,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> she_lin_leggings =
            ITEMS.register("she_lin_leggings", () -> new she_lin(ModArmorMaterials.ARCHER,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> she_lin_boots =
            ITEMS.register("she_lin_boots", () -> new she_lin(ModArmorMaterials.ARCHER,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> chi_ying_helmet =
            ITEMS.register("chi_ying_helmet", () -> new chi_ying(ModArmorMaterials.ARCHER,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> chi_ying_chestplate =
            ITEMS.register("chi_ying_chestplate", () -> new chi_ying(ModArmorMaterials.ARCHER,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> chi_ying_leggings =
            ITEMS.register("chi_ying_leggings", () -> new chi_ying(ModArmorMaterials.ARCHER,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> chi_ying_boots =
            ITEMS.register("chi_ying_boots", () -> new chi_ying(ModArmorMaterials.ARCHER,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> gong_fu_helmet =
            ITEMS.register("gong_fu_helmet", () -> new gong_fu(ModArmorMaterials.WARLOCK,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> gong_fu_chestplate =
            ITEMS.register("gong_fu_chestplate", () -> new gong_fu(ModArmorMaterials.WARLOCK,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> gong_fu_leggings =
            ITEMS.register("gong_fu_leggings", () -> new gong_fu(ModArmorMaterials.WARLOCK,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> gong_fu_boots =
            ITEMS.register("gong_fu_boots", () -> new gong_fu(ModArmorMaterials.WARLOCK,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> hua_tuo_helmet =
            ITEMS.register("hua_tuo_helmet", () -> new hua_tuo(ModArmorMaterials.WARLOCK,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> hua_tuo_chestplate =
            ITEMS.register("hua_tuo_chestplate", () -> new hua_tuo(ModArmorMaterials.WARLOCK,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> hua_tuo_leggings =
            ITEMS.register("hua_tuo_leggings", () -> new hua_tuo(ModArmorMaterials.WARLOCK,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> hua_tuo_boots =
            ITEMS.register("hua_tuo_boots", () -> new hua_tuo(ModArmorMaterials.WARLOCK,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> cao_yao_helmet =
            ITEMS.register("cao_yao_helmet", () -> new cao_yao(ModArmorMaterials.WARLOCK,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> cao_yao_chestplate =
            ITEMS.register("cao_yao_chestplate", () -> new cao_yao(ModArmorMaterials.WARLOCK,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> cao_yao_leggings =
            ITEMS.register("cao_yao_leggings", () -> new cao_yao(ModArmorMaterials.WARLOCK,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> cao_yao_boots =
            ITEMS.register("cao_yao_boots", () -> new cao_yao(ModArmorMaterials.WARLOCK,ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> yun_xing_helmet =
            ITEMS.register("yun_xing_helmet", () -> new yun_xing(ModArmorMaterials.WARLOCK,ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> yun_xing_chestplate =
            ITEMS.register("yun_xing_chestplate", () -> new yun_xing(ModArmorMaterials.WARLOCK,ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> yun_xing_leggings =
            ITEMS.register("yun_xing_leggings", () -> new yun_xing(ModArmorMaterials.WARLOCK,ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> yun_xing_boots =
            ITEMS.register("yun_xing_boots", () -> new yun_xing(ModArmorMaterials.WARLOCK,ArmorItem.Type.BOOTS));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
