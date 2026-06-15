package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import icu.icuqalt10.panlingre.item.warrior.tao_mu_jian;
import icu.icuqalt10.panlingre.network.SyncBlessPayload;
import icu.icuqalt10.panlingre.player.check;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class GameBusEvents {

    // 定义标签常量
    public static final TagKey<Item> WARRIOR_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("panlingre", "zhiye/warrior"));
    public static final TagKey<Item> ARCHER_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("panlingre", "zhiye/archer"));
    public static final TagKey<Item> WARLOCK_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("panlingre", "zhiye/warlock"));

    //玩家进入服务器
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            player.getData(ModAttachments.LINGQI).sync(player);
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BlessData currentData = serverPlayer.getData(ModAttachments.BLESS.get());
            PacketDistributor.sendToPlayer(serverPlayer, new SyncBlessPayload(currentData));
        }
    }

    //玩家重生时
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            player.getData(ModAttachments.LINGQI).sync(player);
        }
    }


    //装备变更 职业限制 手持 防具
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;

        ItemStack newStack = event.getTo();
        if (newStack.isEmpty()) return;

        if (newStack.is(WARRIOR_TAG) && !check.zhiye_check(player, "panlingre:warrior")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.0"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}

        else if (newStack.is(ARCHER_TAG) && !check.zhiye_check(player, "panlingre:archer")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.1"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}

        else if (newStack.is(WARLOCK_TAG) && !check.zhiye_check(player, "panlingre:warlock")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.2"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}
    }
    //饰品栏变更 职业限制 饰品栏
    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;

        String slotIdentifier = event.getIdentifier();
        ItemStack newStack = event.getTo();
        if (newStack.isEmpty()) return;

        if (newStack.is(WARRIOR_TAG) && !check.zhiye_check(player, "panlingre:warrior")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.0"), false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }

        else if (newStack.is(ARCHER_TAG) && !check.zhiye_check(player, "panlingre:archer")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.1"),false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }

        else if (newStack.is(WARLOCK_TAG) && !check.zhiye_check(player, "panlingre:warlock")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.2"),false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }
    }

    //玩家tick
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.tickCount % 10 == 0) {
                LingQiData data = player.getData(ModAttachments.LINGQI);
                float max = (float) player.getAttributeValue(ModAttributes.MAX_LINGQI);

                //每10刻恢复2.5%
                if (data.getCurrent() < max) {
                    data.setCurrent(data.getCurrent() + max * 0.025f, player);
                }

                //同步灵气条
                data.setCurrent(Math.min(data.getCurrent(),max),player);
                data.sync(player);
        }
    }

    //箭矢命中/落地时
    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        Entity projectile = event.getProjectile();
        Level level = projectile.level();

        if (!level.isClientSide && projectile instanceof AbstractArrow arrow) {
            if(arrow.getTags().contains("panlingre:zhong_chui_arrow")) {
                triggerArrowExplosion(level, arrow,10f);
                arrow.discard();
            }
            if(arrow.getTags().contains("panlingre:bei_dou_arrow")) {
                triggerArrowExplosion(level, arrow,5f);
                arrow.discard();
            }
        }
    }
    //箭矢爆炸效果
    private static void triggerArrowExplosion(Level level, AbstractArrow arrow,float multiplied) {

        if (level instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    arrow.getX(), arrow.getY(), arrow.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double radius = 2.0D;
        AABB area = arrow.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        Entity owner = arrow.getOwner();

        for (LivingEntity target : targets) {

            if (owner != null && (target.is(owner) || (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)))) {
                continue;
            }

            float explosionDamage = (float) (arrow.getBaseDamage() * multiplied);
            target.hurt(level.damageSources().explosion(arrow, owner), explosionDamage);
        }
    }

}