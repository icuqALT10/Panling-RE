package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.lang.reflect.Method;

public class CustomPelletEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(CustomPelletEntity.class, EntityDataSerializers.INT);

    public CustomPelletEntity(EntityType<? extends CustomPelletEntity> type, Level level) {
        super(type, level);
    }

    public CustomPelletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.CUSTOM_PELLET.get(), shooter, level);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, 0xFFFFFF);
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);


            this.level().levelEvent(2002, this.blockPosition(), this.getColor());

            ItemStack stack = this.getItem();
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

            this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.0, 2.0, 2.0))
                    .forEach(target -> invokeEffectMethod(itemName, target, stack));

            this.discard();
        }
    }

    private void invokeEffectMethod(String methodName, LivingEntity target, ItemStack stack) {
        try {
            Method method = this.getClass().getDeclaredMethod(methodName, LivingEntity.class, ItemStack.class);
            method.setAccessible(true);
            method.invoke(this, target, stack);
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isFriendly(Entity owner, LivingEntity target) {
        if (owner.getTeam() == null || target.getTeam() == null) return true;
        else return owner.getTeam() == target.getTeam();
    }

    private boolean isUnFriendly(Entity owner, LivingEntity target) {
        if (owner.getTeam() == null || target.getTeam() == null) return true;
        else return owner.getTeam() != target.getTeam();
    }

    //================= 效果方法区 =================

    private void feng_hou_1(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isFriendly(owner, target)) return;

            target.hurt(this.damageSources().indirectMagic(owner,owner), 20);
        }
    }
    private void feng_hou_2(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isFriendly(owner, target)) return;

            target.hurt(this.damageSources().indirectMagic(owner,owner), 50);
        }
    }
    private void feng_hou_3(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isFriendly(owner, target)) return;

            target.hurt(this.damageSources().indirectMagic(owner,owner), 100);
        }
    }
    private void jian_xue(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isFriendly(owner, target)) return;

            target.hurt(this.damageSources().indirectMagic(owner,owner), 500);
        }
    }


    private void hui_chun_1(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.heal(8);
        }
    }
    private void hui_chun_2(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.heal(16);
        }
    }
    private void hui_chun_3(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.heal(32);
        }
    }
    private void qi_si(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.heal(96);
        }
    }

    private void tian_shen_1(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 12000, 1,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 0,false,false,true));
        }
    }
    private void tian_shen_2(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 12000, 2,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1,false,false,true));
        }
    }
    private void tian_shen_3(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 12000, 3,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 2,false,false,true));
        }
    }

    private void jiu_zhuan_1(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12000, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 12000, 0,false,false,true));
        }
    }
    private void jiu_zhuan_2(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12000, 1,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 12000, 1,false,false,true));
        }
    }
    private void jiu_zhuan_3(LivingEntity target, ItemStack stack) {
        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner != null && isUnFriendly(owner, target)) return;

            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12000, 1,false,false,true));
            target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 12000, 2,false,false,true));
        }
    }
}