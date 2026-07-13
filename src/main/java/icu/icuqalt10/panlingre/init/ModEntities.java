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

    // 注册火龙卷实体
    public static final Supplier<EntityType<FireTornadoEntity>> FIRE_TORNADO =
            ENTITIES.register("fire_tornado",
                    () -> EntityType.Builder.of(FireTornadoEntity::new, MobCategory.MISC)
                            .sized(3.0f, 9.0f)          // 碰撞箱大小（宽，高）
                            .clientTrackingRange(10)     // 客户端追踪范围（区块）
                            .updateInterval(1)           // 更新间隔（tick）
                            .fireImmune()                // 免疫火焰伤害（可选）
                            .build("fire_tornado")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}