package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.entity.OrientedHitbox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

public final class ShiHuangPartEntity extends PartEntity<ShiHuangEntity>
        implements MultipartEntity.MultipartPart, MultipartEntity.OrientedPart {
    private final int partIndex;
    private OrientedBoundingBox orientedBox;

    public ShiHuangPartEntity(ShiHuangEntity parent, int index) {
        super(parent);
        partIndex = index;
        noPhysics = true;
    }

    @Override public Entity getMultipartRoot() { return getParent(); }
    @Override public int getPartIndex() { return partIndex; }
    @Override public OrientedBoundingBox getOrientedBox() { return orientedBox; }

    public void setOrientedBox(OrientedBoundingBox box) {
        xo = xOld = getX(); yo = yOld = getY(); zo = zOld = getZ();
        orientedBox = box;
        setPos(box.center);
    }

    @Override protected AABB makeBoundingBox() {
        return orientedBox == null ? super.makeBoundingBox() : new OrientedHitbox(orientedBox);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAlive() { return getParent().isAlive() && !getParent().dying(); }
    @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public Component getDisplayName() { return getParent().getDisplayName(); }
    @Override public ItemStack getPickResult() { return getParent().getPickResult(); }

    @Override public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
