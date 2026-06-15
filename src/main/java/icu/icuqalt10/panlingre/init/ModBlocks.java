package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.block.dzt;
import icu.icuqalt10.panlingre.block.ldl.ldl;
import icu.icuqalt10.panlingre.block.ldl.ldlProxyBlock;
import icu.icuqalt10.panlingre.block.ore.*;
import icu.icuqalt10.panlingre.block.zft.zft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PanlingRE.MODID);

    //功能方块
    public static final DeferredBlock<Block> ldl = BLOCKS.register("ldl",
            () -> new ldl(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .noOcclusion()
                    .dynamicShape()
            ));
    public static final DeferredBlock<Block> ldl_proxy = BLOCKS.register("ldl_proxy",
            () -> new ldlProxyBlock(BlockBehaviour.Properties.ofFullCopy(ldl.get())
                    .noOcclusion()
                    .noLootTable()
            ));

    public static final DeferredBlock<Block> zft = BLOCKS.register("zft",
            () -> new zft(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .noOcclusion()
                    .dynamicShape()
            ));

    public static final DeferredBlock<Block> dzt = BLOCKS.register("dzt",
            () -> new dzt(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .noOcclusion()
                    .dynamicShape()
            ));
    //矿石
    public static final DeferredBlock<Block> suspicious_dirt = BLOCKS.registerBlock("suspicious_dirt",
            properties -> new suspicious_dirt(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ROOTED_DIRT)
            )
    );
    public static final DeferredBlock<Block> zhu_sha_ore = BLOCKS.registerBlock("zhu_sha_ore",
            properties -> new zhu_sha_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> jing_tie_ore = BLOCKS.registerBlock("jing_tie_ore",
            properties -> new jing_tie_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> chi_tong_ore = BLOCKS.registerBlock("chi_tong_ore",
            properties -> new chi_tong_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> jin_jing_ore = BLOCKS.registerBlock("jin_jing_ore",
            properties -> new jin_jing_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> xuan_tie_ore = BLOCKS.registerBlock("xuan_tie_ore",
            properties -> new xuan_tie_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> bai_yu_ore = BLOCKS.registerBlock("bai_yu_ore",
            properties -> new bai_yu_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> ling_yu_ore = BLOCKS.registerBlock("ling_yu_ore",
            properties -> new ling_yu_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> bi_hai_ore = BLOCKS.registerBlock("bi_hai_ore",
            properties -> new bi_hai_ore(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
    public static final DeferredBlock<Block> bamboo_block = BLOCKS.registerBlock("bamboo_block",
            properties -> new bamboo_block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .dynamicShape()
                    .randomTicks()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.BAMBOO)
            )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}