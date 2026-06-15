package icu.icuqalt10.panlingre.block.zft;

import icu.icuqalt10.panlingre.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class zftEntity extends BlockEntity {

    public zftEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.zft_be.get(), pos, state);
    }

    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(2.0);
    }

}