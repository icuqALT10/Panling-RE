package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.*;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    // 1. 创建注册表容器
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, PanlingRE.MODID);

    //注册
    public static final DeferredHolder<EntityType<?>, EntityType<PoDiFuEntity>> PO_DI_FU =
            ENTITIES.register("po_di_fu", () -> EntityType.Builder.<PoDiFuEntity>of(PoDiFuEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("po_di_fu"));
    public static final DeferredHolder<EntityType<?>, EntityType<CustomPelletEntity>> CUSTOM_PELLET =
            ENTITIES.register("custom_pellet", () -> EntityType.Builder.<CustomPelletEntity>of(CustomPelletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("custom_pellet"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuoQiuFuEntity>> HUO_QIU_FU =
            ENTITIES.register("huo_qiu_fu", () -> EntityType.Builder.<HuoQiuFuEntity>of(HuoQiuFuEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("huo_qiu_fu"));

    public static final DeferredHolder<EntityType<?>, EntityType<JinLiRenEntity>> JIN_LI_REN =
            ENTITIES.register("jin_li_ren", () -> EntityType.Builder.<JinLiRenEntity>of(JinLiRenEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("jin_li_ren"));

    public static final DeferredHolder<EntityType<?>, EntityType<FeiXianJianZhenEntity>> FEI_XIAN_JIAN_ZHEN =
            ENTITIES.register("fei_xian_jian_zhen", () -> EntityType.Builder.of(FeiXianJianZhenEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .build("fei_xian_jian_zhen"));
    public static final DeferredHolder<EntityType<?>, EntityType<XingHaiEntity>> XING_HAI =
            ENTITIES.register("xing_hai", () -> EntityType.Builder.of(XingHaiEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .build("xing_hai"));

    //生物
    //boss
    public static final DeferredHolder<EntityType<?>, EntityType<PanGuEntity>> PAN_GU =
            ENTITIES.register("pan_gu", () -> EntityType.Builder.of(PanGuEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.9F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("pan_gu"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZhuRiArrowEntity>> ZHU_RI_ARROW =
            ENTITIES.register("zhu_ri_arrow", () -> EntityType.Builder.<ZhuRiArrowEntity>of(ZhuRiArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("zhu_ri_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<YsMuHealingEntity>> YS_MU_HEALING =
            ENTITIES.register("ys_mu_healing", () -> EntityType.Builder.<YsMuHealingEntity>of(YsMuHealingEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ys_mu_healing"));

    public static final DeferredHolder<EntityType<?>, EntityType<Ys3JinTornadoEntity>> YS3_JIN_TORNADO =
            ENTITIES.register("ys3_jin_tornado", () -> EntityType.Builder.<Ys3JinTornadoEntity>of(Ys3JinTornadoEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(10).updateInterval(1).noSave().noSummon()
                    .build("ys3_jin_tornado"));

    public static final DeferredHolder<EntityType<?>, EntityType<Ys3MuDomainEntity>> YS3_MU_DOMAIN =
            ENTITIES.register("ys3_mu_domain", () -> EntityType.Builder.<Ys3MuDomainEntity>of(Ys3MuDomainEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(12).updateInterval(1).noSave().noSummon()
                    .build("ys3_mu_domain"));

    public static final DeferredHolder<EntityType<?>, EntityType<Ys3ShuiDomainEntity>> YS3_SHUI_DOMAIN =
            ENTITIES.register("ys3_shui_domain", () -> EntityType.Builder.<Ys3ShuiDomainEntity>of(Ys3ShuiDomainEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(12).updateInterval(1).noSave().noSummon()
                    .build("ys3_shui_domain"));

    public static final DeferredHolder<EntityType<?>, EntityType<Ys3HuoDomainEntity>> YS3_HUO_DOMAIN =
            ENTITIES.register("ys3_huo_domain", () -> EntityType.Builder.<Ys3HuoDomainEntity>of(Ys3HuoDomainEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(12).updateInterval(1).noSave().noSummon()
                    .build("ys3_huo_domain"));

    public static final DeferredHolder<EntityType<?>, EntityType<TuBarrierEntity>> TU_BARRIER =
            ENTITIES.register("tu_barrier", () -> EntityType.Builder.<TuBarrierEntity>of(TuBarrierEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("tu_barrier"));

    // 注册火龙卷实体
    public static final DeferredHolder<EntityType<?>, EntityType<FireTornadoEntity>> FIRE_TORNADO =
            ENTITIES.register("fire_tornado",
                    () -> EntityType.Builder.<FireTornadoEntity>of(FireTornadoEntity::new, MobCategory.MISC)
                            .sized(3.0f, 9.0f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .fireImmune()
                            .build("fire_tornado")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
