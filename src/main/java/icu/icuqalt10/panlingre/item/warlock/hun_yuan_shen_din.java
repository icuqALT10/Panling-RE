package icu.icuqalt10.panlingre.item.warlock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.event.GameBusEvents;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.item.liandan;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.network.GroundSmashPayload;
import icu.icuqalt10.panlingre.network.ShakePayload;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import icu.icuqalt10.panlingre.util.Shockwave;
import icu.icuqalt10.panlingre.util.SkillHelper;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class hun_yuan_shen_din extends Item implements ICurioItem,skill_trigger, liandan {

    public hun_yuan_shen_din() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "hun_yuan_shen_din");

        modifiers.put(ModAttributes.MAGIC_DAMAGE, new AttributeModifier(
                UID,
                40,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.FALIZHI, new AttributeModifier(
                UID,
                30,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                UID,
                20,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                UID,
                0.25,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));

        return modifiers;
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        if (level.isClientSide) return true;

        switch (skillIndex) {
            case 0 -> Skill1(player);
            case 1 -> Skill2(player);
            case 2 -> Skill3(player);
            case 3 -> Skill4(player);
        };
        return true;
    }

    @Override
    public int getSkillCount() { return 4; }

    @Override
    public long getSkillCD(int skillIndex) {
        return switch (skillIndex) {
            case 0 -> 10000L;
            case 1 -> 12000L;
            case 2 -> 30000L;
            case 3 -> 8000L;
            default -> 1000L;
        };
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return switch (skillIndex) {
            case 0 -> "item.PanlingRE.hun_yuan_shen_din.skill1";
            case 1 -> "item.PanlingRE.hun_yuan_shen_din.skill2";
            case 2 -> "item.PanlingRE.hun_yuan_shen_din.skill3";
            case 3 -> "item.PanlingRE.hun_yuan_shen_din.skill4";
            default -> "item.panlingre.hun_yuan_shen_din";
        };
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return switch (skillIndex) {
            case 0 -> 25;
            case 2 -> 35;
            case 1,3 -> 20;
            default -> 5.0f;
        };
    }

    @Override
    public ResourceLocation getSkillIcon(int skillIndex) {
        return switch (skillIndex) {
            case 0 -> ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/icons/hun_yuan_shen_din/0.png");
            case 1 -> ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/icons/hun_yuan_shen_din/1.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/icons/hun_yuan_shen_din/2.png");
            case 3 -> ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/icons/hun_yuan_shen_din/3.png");
            default -> null;
        };
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return switch (skillIndex) {
            case 0 -> new String[]{"item.PanlingRE.hun_yuan_shen_din.skill1.2","item.PanlingRE.hun_yuan_shen_din.skill1.3"};
            case 1 -> new String[]{"item.PanlingRE.hun_yuan_shen_din.skill2.2","item.PanlingRE.hun_yuan_shen_din.skill2.3"};
            case 2 -> new String[]{"item.PanlingRE.hun_yuan_shen_din.skill3.2","item.PanlingRE.hun_yuan_shen_din.skill3.3"};
            case 3 -> new String[]{"item.PanlingRE.hun_yuan_shen_din.skill4.2","item.PanlingRE.hun_yuan_shen_din.skill4.3"};
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.lore2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.lore3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.lore4"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill1.1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill2.1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill3.1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill4.1"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill1.2"
                    ,Component.keybind("key.PanlingRE.liandan").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill2"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill.1"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    private void Skill1(Player player) {
        PacketDistributor.sendToPlayersTrackingEntity(player, new ShakePayload(player.position(), 10, 5, 1.5f));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ShakePayload(player.position(), 10, 5, 1.5f));
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        //伤害
        float lighting_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 3);
        float gundilei_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 2);

        // 1. 获取周围半径 10 格内的所有实体
        AABB searchBox = player.getBoundingBox().inflate(10.0);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox);

        for (LivingEntity entity : entities) {
            if (player.getTeam() != null && player.getTeam() != entity.getTeam()) {
                // 记录实体当前的位置
                final Vec3 targetPos = entity.position();

                // 主雷
                // 触发主雷粒子
                ParticleLighting mainLighting = new ParticleLighting(targetPos);
                PacketDistributor.sendToPlayersTrackingEntity(player, mainLighting);
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, mainLighting);
                }

                // 检测主雷伤害
                if (entity.position().distanceToSqr(targetPos) <= 4.0) {
                    entity.hurt(serverLevel.damageSources().lightningBolt(), lighting_damage);
                }

                // 原地生成第一个滚地雷
                spawnGundilei(serverLevel, player, targetPos, gundilei_damage);

                // 3道扩散雷
                for (int i = 0; i < 3; i++) {
                    final int index = i;
                    int staggeredDelay = 2 + (i * 3); // 阶梯式延迟

                    // 极坐标均匀散开
                    double angle = (index * 72.0 + serverLevel.getRandom().nextInt(15)) * Math.PI / 180.0;
                    double distance = 4.0 + serverLevel.getRandom().nextDouble() * 4.0; // 离中心 4~8 格

                    double offsetX = Math.cos(angle) * distance;
                    double offsetZ = Math.sin(angle) * distance;

                    GameBusEvents.queueTask(staggeredDelay, () -> {
                        if (!serverLevel.getServer().isRunning() || !entity.isAlive()) return;

                        // 精准锁定目标 XZ
                        double targetX = targetPos.x + offsetX;
                        double targetZ = targetPos.z + offsetZ;

                        // 【核心修改】智能地面探测：不使用全局Heightmap，而是围绕实体的 Y 轴上下寻找落脚点
                        double safeY = targetPos.y;
                        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(targetX, targetPos.y + 2, targetZ);

                        // 从实体头顶 2 格往下探测 5 格，寻找第一个非空气方块作为地面
                        for (int dy = 0; dy < 6; dy++) {
                            if (!serverLevel.isEmptyBlock(mutablePos) && serverLevel.isEmptyBlock(mutablePos.above())) {
                                safeY = mutablePos.getY() + 1;
                                break;
                            }
                            mutablePos.move(0, -1, 0);
                        }

                        Vec3 spreadPos = new Vec3(targetX, safeY, targetZ);

                        // 发送粒子效果
                        ParticleLighting spreadLighting = new ParticleLighting(spreadPos);
                        PacketDistributor.sendToPlayersTrackingEntity(player, spreadLighting);
                        if (player instanceof ServerPlayer sp) {
                            PacketDistributor.sendToPlayer(sp, spreadLighting);
                        }

                        // 检测扩散雷伤害
                        if (entity.position().distanceToSqr(spreadPos) <= 4.0) {
                            entity.hurt(serverLevel.damageSources().lightningBolt(), 15.0f);
                        }

                        // 生成苦力怕
                        spawnGundilei(serverLevel, player, spreadPos, gundilei_damage);
                    });
                }
            }
        }

        //播报
        player.displayClientMessage(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill1.success"), true);
    }

    private void Skill2(Player player) {
        PacketDistributor.sendToPlayersTrackingEntity(player, new ShakePayload(player.position(), 10, 5, 1.5f));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ShakePayload(player.position(), 10, 5, 1.5f));
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        //伤害 时间
        float damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 3.5);
        int maxAge = (int) player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) / 4;

        // 以玩家为中心生成震动波
        PlayerTeam team = player.getTeam() instanceof PlayerTeam pt ? pt : null;
        GameBusEvents.addShockwave(player, new Shockwave(player.position(), maxAge,damage, team));

        //裂地效果
        PacketDistributor.sendToPlayersTrackingEntity(player, new GroundSmashPayload(player.position(), maxAge/2f, 2));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new GroundSmashPayload(player.position(), maxAge/2f, 20));
        }

        // 音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.displayClientMessage(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill2.success"), true);
    }

    private void Skill3(Player player) {
        //时间
        float width = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) / 5);
        float length = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) / 2);
        int duration = (int) player.getAttributeValue(ModAttributes.MAGIC_DAMAGE);

        Vec3 position = new Vec3(player.position().x,player.position().y+10,player.position().z);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(20));;
        PacketDistributor.sendToPlayersTrackingEntity(player, new ShakePayload(player.position(), 10, 5, 1.5f));
        PacketDistributor.sendToPlayersTrackingEntity(
                player,
                new ParticleCluster(
                        position, targetPos,
                        ParticleTypes.SNOWFLAKE,
                        3000, 12
                ));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ShakePayload(player.position(), 10, 5, 1.5f));
            PacketDistributor.sendToPlayer(
                    sp,
                    new ParticleCluster(
                            position, targetPos,
                            ParticleTypes.SNOWFLAKE,
                            3000, 12
                    ));
        }

        List<LivingEntity> targetEntities = SkillHelper.getLivingEntitiesInFront(player, width, width, length);
        for (LivingEntity entity : targetEntities) {
            if (player.getTeam() != null && player.getTeam() != entity.getTeam()) {
                entity.addEffect(new MobEffectInstance(ModEffects.freeze, duration, 0));
                entity.setTicksFrozen(140);
            }
        }

        // 音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.displayClientMessage(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill3.success"), true);
    }

    private void Skill4(Player player) {
        PacketDistributor.sendToPlayersTrackingEntity(player, new ShakePayload(player.position(), 10, 5, 1.5f));
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ShakePayload(player.position(), 10, 5, 1.5f));
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        //伤害
        float damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 2.5);

        PlayerTeam team = player.getTeam();

        Vec3 pos = player.position();                    // 实体绝对坐标
        Vec3 forward = player.getLookAngle();            // 单位前向量（含俯仰）
        Vec3 up = new Vec3(0, 1, 0);                    // 世界 Y 轴
        Vec3 left = up.cross(forward).normalize();      // 单位左向量（水平方向，与俯仰无关）

        Vec3 midStart = pos.add(forward);               // 实体前方 1 格
        Vec3 leftStart = pos.add(left.scale(-8)).add(forward);  // 左 5，前 1
        Vec3 rightStart = pos.add(left.scale(8)).add(forward);  // 右 5，前 1

        Vec3 midTarget = pos.add(forward.scale(31));                // 中间笔直 30 格
        Vec3 leftTarget = pos.add(left.scale(8)).add(forward.scale(29));   // 左起点 → 右前方
        Vec3 rightTarget = pos.add(left.scale(-8)).add(forward.scale(29)); // 右起点 → 左前方

        //生成3个火龙卷
        FireTornadoEntity tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                midStart, midTarget, 20, damage, team);
        serverLevel.addFreshEntity(tornado);

        FireTornadoEntity tornadoL = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                leftStart, leftTarget, 20, damage, team);
        serverLevel.addFreshEntity(tornadoL);

        FireTornadoEntity tornadoR = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                rightStart, rightTarget, 20, damage, team);
        serverLevel.addFreshEntity(tornadoR);

        // 音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.displayClientMessage(Component.translatable("item.PanlingRE.hun_yuan_shen_din.skill4.success"), true);
    }

    //滚地雷（闪电苦力怕）生成
    private void spawnGundilei(ServerLevel level,Player player, Vec3 pos, float damage) {
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper != null) {
            creeper.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

            // 闪电苦力怕状态
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("powered", true);
            creeper.readAdditionalSaveData(nbt);

            creeper.setNoAi(true);             // 无 AI
            creeper.setInvulnerable(true);     // 无敌
            creeper.setCustomName(Component.translatable("entity.panlingre.pan_gu.creeper"));
            creeper.setCustomNameVisible(true);

            // 写入自定义的 NBT Tag 用于记录生存时间
            creeper.getPersistentData().putInt("GundileiTicks", 20);
            //写入伤害
            creeper.getPersistentData().putFloat("GundileiDamage", damage);

            //加入队伍
            if (player.getTeam() != null) {
                creeper.level().getScoreboard()
                        .addPlayerToTeam(creeper.getStringUUID(), player.getTeam());
            }

            level.addFreshEntity(creeper);
        }
    }

    @Override
    public boolean liandan_trigger(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            new ldlMenu(id, inv, ContainerLevelAccess.NULL),
                            Component.translatable("block.panlingre.ldl")),
                    buf -> buf.writeBlockPos(player.blockPosition()));
        }
        return true;
    }
}
