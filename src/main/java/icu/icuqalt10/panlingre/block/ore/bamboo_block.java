package icu.icuqalt10.panlingre.block.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class bamboo_block extends Block implements EntityBlock {
    public static final BooleanProperty IS_SAPLING = BooleanProperty.create("is_sapling");

    // 碰撞箱定义：还原原版竹子的大小

    public bamboo_block(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_SAPLING, true));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 关键：所有阶段都持有 BE，防止 IS_SAPLING 改变时数据丢失
        return new bamboo_block_entity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            // 只有根部（下方不是同类方块）才负责计时[cite: 6]
            if (!level.getBlockState(pos.below()).is(this)) {
                if (level.getBlockEntity(pos) instanceof bamboo_block_entity be) {
                    be.setLastGrowTime(level.getGameTime());
                }
                level.scheduleTick(pos, this, 1200);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 只允许根部执行生长逻辑[cite: 6]
        if (!level.getBlockState(pos.below()).is(this)) {
            if (level.getBlockEntity(pos) instanceof bamboo_block_entity be) {
                long currentTime = level.getGameTime();
                long lastTime = be.getLastGrowTime();

                // 容错处理：如果时间为0则初始化
                if (lastTime <= 0) {
                    be.setLastGrowTime(currentTime);
                    lastTime = currentTime;
                }

                long elapsed = currentTime - lastTime;
                int cycles = (int) (elapsed / 1200);

                if (cycles > 0) {
                    for (int i = 0; i < cycles; i++) {
                        if (!doActualGrow(level, pos)) break;
                    }
                    be.setLastGrowTime(currentTime); // 更新时间戳实现补偿
                }
            }
            level.scheduleTick(pos, this, 1200);
        }
    }

    private boolean doActualGrow(ServerLevel level, BlockPos rootPos) {
        int height = 0;
        BlockPos.MutableBlockPos checkPos = rootPos.mutable();

        // 向上扫描计算高度[cite: 1, 6]
        while (level.getBlockState(checkPos).is(this)) {
            height++;
            checkPos.move(Direction.UP);
            if (height >= 10) return false;
        }

        BlockState rootState = level.getBlockState(rootPos);
        // 如果是竹笋，先变身并尝试长出第一节[cite: 3, 6]
        if (height == 1 && rootState.getValue(IS_SAPLING)) {
            level.setBlock(rootPos, rootState.setValue(IS_SAPLING, false), 3);
            if (level.isEmptyBlock(checkPos)) {
                level.setBlock(checkPos, this.defaultBlockState().setValue(IS_SAPLING, false), 3);
            }
            return true;
        } else if (height < 10 && level.isEmptyBlock(checkPos)) {
            level.setBlock(checkPos, this.defaultBlockState().setValue(IS_SAPLING, false), 3);
            return true;
        }
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockPos rootPos = findRoot(level, pos);
            breakConnectedBamboo(level, pos, player, player.getMainHandItem(), rootPos);

            // 变回竹笋并重新初始化计时[cite: 1, 6]
            level.setBlock(rootPos, this.defaultBlockState().setValue(IS_SAPLING, true), 3);
            if (level.getBlockEntity(rootPos) instanceof bamboo_block_entity be) {
                be.setLastGrowTime(level.getGameTime());
            }
            level.scheduleTick(rootPos, this, 1200);

            if (!pos.equals(rootPos)) {
                level.removeBlock(pos, false);
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    private BlockPos findRoot(Level level, BlockPos startPos) {
        BlockPos.MutableBlockPos current = startPos.mutable();
        while (level.getBlockState(current.below()).is(this)) {
            current.move(Direction.DOWN);
        }
        return current.immutable();
    }

    private void breakConnectedBamboo(Level level, BlockPos pos, Player player, ItemStack tool, BlockPos rootPos) {
        scanAndBreak(level, pos.above(), Direction.UP, player, tool, rootPos);
        scanAndBreak(level, pos.below(), Direction.DOWN, player, tool, rootPos);
        Block.dropResources(level.getBlockState(pos), level, pos, null, player, tool);
    }

    private void scanAndBreak(Level level, BlockPos startPos, Direction direction, Player player, ItemStack tool, BlockPos rootPos) {
        BlockPos.MutableBlockPos currentPos = startPos.mutable();
        while (level.getBlockState(currentPos).is(this)) {
            if (currentPos.equals(rootPos)) break;
            BlockState state = level.getBlockState(currentPos);
            Block.dropResources(state, level, currentPos, null, player, tool);
            level.removeBlock(currentPos, false);
            currentPos.move(direction);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(IS_SAPLING) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IS_SAPLING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState stateBelow = context.getLevel().getBlockState(context.getClickedPos().below());
        return stateBelow.is(this) ? this.defaultBlockState().setValue(IS_SAPLING, false) : super.getStateForPlacement(context);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // 下方有竹子则自己肯定不是竹笋
        if (direction == Direction.DOWN && neighborState.is(this)) {
            return state.setValue(IS_SAPLING, false);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}