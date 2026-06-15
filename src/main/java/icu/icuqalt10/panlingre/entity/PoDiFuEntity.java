package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class PoDiFuEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Float> DATA_MULTIPLIER =
            SynchedEntityData.defineId(PoDiFuEntity.class, EntityDataSerializers.FLOAT);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DATA_MULTIPLIER, 1.0f);
    }

    public PoDiFuEntity(EntityType<? extends PoDiFuEntity> type, Level level) {
        super(type, level);
    }

    public PoDiFuEntity(Level level, LivingEntity shooter, float multiplier) {
        super(ModEntities.PO_DI_FU.get(), shooter, level);
        this.setMultiplier(multiplier);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.po_di_fu.get();
    }

    public void setMultiplier(float multiplier) {
        this.entityData.set(DATA_MULTIPLIER, multiplier);
    }

    public float getMultiplier() {
        return this.entityData.get(DATA_MULTIPLIER);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner instanceof Player player) {
                double falizhi = player.getAttributeValue(ModAttributes.FALIZHI);

                float finalDamage = (float) (falizhi * this.getMultiplier());

                result.getEntity().hurt(this.damageSources().thrown(this, player), finalDamage);
            }
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard(); // 撞击方块也消失
        }
    }
}