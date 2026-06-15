package icu.icuqalt10.panlingre.block.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;

public class jing_tie_ore extends Block {
    public static final BooleanProperty HAS_ORE = BooleanProperty.create("has_ore");

    public jing_tie_ore(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_ORE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_ORE);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (state.getValue(HAS_ORE) && !player.isCreative()) {
            if (!level.isClientSide) {
                ItemStack tool = player.getMainHandItem();
                Block.dropResources(state, level, pos, null, player, tool);

                level.setBlock(pos, state.setValue(HAS_ORE, false), 3);

                level.scheduleTick(pos, this, 1200);
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!state.getValue(HAS_ORE)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HAS_ORE)) {
            level.setBlock(pos, state.setValue(HAS_ORE, true), 3);
        }
    }
}