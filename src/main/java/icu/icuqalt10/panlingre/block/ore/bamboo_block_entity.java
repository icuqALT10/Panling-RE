package icu.icuqalt10.panlingre.block.ore;

import icu.icuqalt10.panlingre.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class bamboo_block_entity extends BlockEntity {
    private long lastGrowTime = 0;

    public bamboo_block_entity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAMBOO_BE.get(), pos, state);
    }

    public void setLastGrowTime(long time) {
        this.lastGrowTime = time;
        this.setChanged(); // 必须标记改变，否则无法保存到磁盘[cite: 7]
    }

    public long getLastGrowTime() {
        return this.lastGrowTime;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 区块重新加载时，如果没有记录时间，则初始化为当前游戏时间[cite: 7]
        if (this.lastGrowTime <= 0 && this.level != null) {
            this.lastGrowTime = this.level.getGameTime();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastGrowTime", this.lastGrowTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lastGrowTime = tag.getLong("LastGrowTime");
    }
}