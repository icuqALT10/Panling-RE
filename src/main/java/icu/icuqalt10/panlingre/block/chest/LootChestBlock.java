package icu.icuqalt10.panlingre.block.chest;

import com.mojang.serialization.MapCodec;
import icu.icuqalt10.panlingre.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LootChestBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final EnumProperty<LootChestType> CHEST_TYPE = EnumProperty.create("chest_type", LootChestType.class);

    public LootChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CHEST_TYPE, LootChestType.GOLDEN));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(1, 0, 1, 15, 14, 15);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(LootChestBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CHEST_TYPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        LootChestType type = LootChestType.GOLDEN;
        ItemStack stack = ctx.getItemInHand();
        CustomData beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData != null) {
            String typeStr = beData.copyTag().getString("chestType");
            if (!typeStr.isEmpty()) {
                type = LootChestType.byName(typeStr);
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(CHEST_TYPE, type);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.LOOT_CHEST_BE.get()) {
            @SuppressWarnings("unchecked")
            BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) (BlockEntityTicker<LootChestBlockEntity>) LootChestBlockEntity::tick;
            return ticker;
        }
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        LootChestBlockEntity be = (LootChestBlockEntity) level.getBlockEntity(pos);
        if (be == null || be.isOpening()) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        if (be.tryOpenWith(held, player.getName().getString())) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }
}
