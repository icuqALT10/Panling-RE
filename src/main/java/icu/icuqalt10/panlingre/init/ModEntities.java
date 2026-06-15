package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.CustomPelletEntity;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.entity.PoDiFuEntity;
import icu.icuqalt10.panlingre.entity.XingHaiEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}