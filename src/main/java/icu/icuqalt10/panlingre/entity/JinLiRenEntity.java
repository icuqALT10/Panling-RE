package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** A pickup-less, gravity-free arrow used by the refined metal skill. */
public class JinLiRenEntity extends AbstractArrow {
    private static final int MAX_LIFETIME_TICKS = 1200;
    private static final EntityDataAccessor<Byte> MAX_PIERCE =
            SynchedEntityData.defineId(JinLiRenEntity.class, EntityDataSerializers.BYTE);

    private double skillDamage;
    private int entitiesHit;

    public JinLiRenEntity(EntityType<? extends JinLiRenEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    /**
     * @param damage exact damage dealt per hit
     * @param pierceCount number of entities the blade passes through after its first target
     */
    public JinLiRenEntity(Level level, LivingEntity owner, double damage, int pierceCount) {
        super(ModEntities.JIN_LI_REN.get(), owner, level, ItemStack.EMPTY, null);
        this.skillDamage = Math.max(0.0D, damage);
        this.entityData.set(MAX_PIERCE, (byte)Math.clamp(pierceCount, 0, Byte.MAX_VALUE));
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAX_PIERCE, (byte)0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        // A gravity-free arrow may never reach a block, so also enforce the
        // vanilla arrow despawn duration while it is still in flight.
        if (!this.level().isClientSide && this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public byte getPierceLevel() {
        return this.entityData.get(MAX_PIERCE);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // AbstractArrow normally scales damage by speed. Compensate so the
        // constructor argument remains the actual damage even after air drag.
        double speed = this.getDeltaMovement().length();
        this.setBaseDamage(speed > 1.0E-7D ? this.skillDamage / speed : this.skillDamage);
        super.onHitEntity(result);

        if (!this.level().isClientSide && !this.isRemoved()) {
            this.entitiesHit++;
            if (this.entitiesHit >= Byte.toUnsignedInt(this.getPierceLevel()) + 1) {
                this.discard();
            }
        }
    }

    @Override
    public void setOwner(Entity owner) {
        super.setOwner(owner);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void playerTouch(Player player) {
        // This projectile deliberately has no collectible item form.
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("SkillDamage", this.skillDamage);
        tag.putByte("MaxPierce", this.entityData.get(MAX_PIERCE));
        tag.putInt("EntitiesHit", this.entitiesHit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.skillDamage = Math.max(0.0D, tag.getDouble("SkillDamage"));
        this.entityData.set(MAX_PIERCE, tag.getByte("MaxPierce"));
        this.entitiesHit = Math.max(0, tag.getInt("EntitiesHit"));
        this.pickup = Pickup.DISALLOWED;
    }
}
