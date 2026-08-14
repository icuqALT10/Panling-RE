package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Ys3HuoDomainEntity extends Ys3DomainEntity {
    private static final int DURATION = 10 * 20;
    private static final float LING_QI_COST_PER_SECOND = 10.0F;
    private static final ResourceLocation ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ys3_huo_attack_damage");
    private static final ResourceLocation ARROW_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ys3_huo_arrow_damage");
    private static final ResourceLocation MAGIC_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ys3_huo_magic_damage");
    private static final ResourceLocation COOLDOWN_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ys3_huo_cooldown_remove");
    private final Set<UUID> previousTargets = new HashSet<>();
    private final Set<UUID> currentTargets = new HashSet<>();
    private boolean cleanedUp;

    public Ys3HuoDomainEntity(EntityType<? extends Ys3HuoDomainEntity> type, Level level) {
        super(type, level);
    }

    public Ys3HuoDomainEntity(Level level, LivingEntity owner, Vec3 center, ItemStack stack) {
        super(ModEntities.YS3_HUO_DOMAIN.get(), level, owner, center, stack, 0.0F, DURATION);
    }

    @Override
    protected int effectTickOffset() {
        return 1;
    }

    @Override
    protected void beforeApplyingEffects() {
        currentTargets.clear();
    }

    @Override
    protected void applyEffect(LivingEntity target, float ignored) {
        if (!(target instanceof Player player)) return;
        LingQiData data = player.getData(ModAttachments.LINGQI);
        if (data.getCurrent() < LING_QI_COST_PER_SECOND) {
            removeBuff(player);
            return;
        }
        data.setCurrent(data.getCurrent() - LING_QI_COST_PER_SECOND, player);
        data.sync(player);
        addBuff(player);
        currentTargets.add(player.getUUID());
    }

    @Override
    protected void afterApplyingEffects() {
        if (level() instanceof ServerLevel serverLevel) {
            for (UUID id : previousTargets) {
                if (currentTargets.contains(id)) continue;
                Entity entity = serverLevel.getEntity(id);
                if (entity instanceof Player player) removeBuff(player);
            }
        }
        previousTargets.clear();
        previousTargets.addAll(currentTargets);
    }

    @Override
    protected void onDomainEnd() {
        if (cleanedUp) return;
        cleanedUp = true;
        if (level() instanceof ServerLevel serverLevel) {
            for (UUID id : previousTargets) {
                Entity entity = serverLevel.getEntity(id);
                if (entity instanceof Player player) removeBuff(player);
            }
        }
        previousTargets.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) onDomainEnd();
        super.remove(reason);
    }

    private static void addBuff(Player player) {
        addFinalMultiplier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID);
        addFinalMultiplier(player.getAttribute(ModAttributes.ARROW_DAMAGE), ARROW_DAMAGE_ID);
        addFinalMultiplier(player.getAttribute(ModAttributes.MAGIC_DAMAGE), MAGIC_DAMAGE_ID);
        AttributeInstance cooldown = player.getAttribute(ModAttributes.COOLDOWN_REMOVE);
        if (cooldown != null) {
            cooldown.removeModifier(COOLDOWN_ID);
            cooldown.addTransientModifier(new AttributeModifier(
                    COOLDOWN_ID, 0.25D, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeBuff(Player player) {
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID);
        removeModifier(player.getAttribute(ModAttributes.ARROW_DAMAGE), ARROW_DAMAGE_ID);
        removeModifier(player.getAttribute(ModAttributes.MAGIC_DAMAGE), MAGIC_DAMAGE_ID);
        AttributeInstance cooldown = player.getAttribute(ModAttributes.COOLDOWN_REMOVE);
        if (cooldown != null) cooldown.removeModifier(COOLDOWN_ID);
    }

    private static void addFinalMultiplier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(
                id, 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) attribute.removeModifier(id);
    }

    @Override
    protected void spawnClientParticles(int activeAge) {
        if (activeAge % 4 != 0) return;
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * getDomainRadius();
            level().addParticle(ParticleTypes.FLAME, true,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.2D + random.nextDouble() * 4.6D,
                    getZ() + Math.sin(angle) * radius,
                    0.0D, 0.015D, 0.0D);
        }
    }

}
