package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.block.ldl.ldlEntity;
import icu.icuqalt10.panlingre.block.ore.bamboo_block_entity;
import icu.icuqalt10.panlingre.block.zft.zftEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PanlingRE.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ldlEntity>> ldl_be =
            BLOCK_ENTITIES.register("ldl_be", () -> BlockEntityType.Builder.of(
                    ldlEntity::new, ModBlocks.ldl.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<zftEntity>> zft_be =
            BLOCK_ENTITIES.register("zft_be", () -> BlockEntityType.Builder.of(
                    zftEntity::new, ModBlocks.zft.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<bamboo_block_entity>> BAMBOO_BE =
            BLOCK_ENTITIES.register("bamboo_block_entity",
                    () -> BlockEntityType.Builder.of(bamboo_block_entity::new, ModBlocks.bamboo_block.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}