package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BaFangYiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiLoader;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiMajorEntry;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiSubEntry;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.network.BaFangYiTeleportPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class BaFangYiCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("bafangyi")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("major", StringArgumentType.word())
                                                .executes(ctx -> addMajor(ctx, StringArgumentType.getString(ctx, "major")))
                                                .then(Commands.argument("sub", StringArgumentType.word())
                                                        .executes(ctx -> addSub(ctx,
                                                                StringArgumentType.getString(ctx, "major"),
                                                                StringArgumentType.getString(ctx, "sub")))
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("major", StringArgumentType.word())
                                                .executes(ctx -> removeMajor(ctx, StringArgumentType.getString(ctx, "major")))
                                                .then(Commands.argument("sub", StringArgumentType.word())
                                                        .executes(ctx -> removeSub(ctx,
                                                                StringArgumentType.getString(ctx, "major"),
                                                                StringArgumentType.getString(ctx, "sub")))
                                                )
                                        )
                                )
                                .then(Commands.literal("query")
                                        .then(Commands.argument("major", StringArgumentType.word())
                                                .executes(ctx -> queryMajor(ctx, StringArgumentType.getString(ctx, "major")))
                                                .then(Commands.argument("sub", StringArgumentType.word())
                                                        .executes(ctx -> querySub(ctx,
                                                                StringArgumentType.getString(ctx, "major"),
                                                                StringArgumentType.getString(ctx, "sub")))
                                                )
                                        )
                                )
                        )
        );
    }

    private static final String PREFIX = "command.panlingre.ba_fang_yi.";
    private static final int TELEPORT_COOLDOWN_TICKS = 600;

    private static int addMajor(CommandContext<CommandSourceStack> ctx, String majorId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            if (BaFangYiData.addMajor(player, majorId)) {
                source.sendSuccess(() -> Component.translatable(PREFIX + "add_major.success", majorId), true);
                return 1;
            }
            source.sendFailure(Component.translatable(PREFIX + "add_major.fail", majorId));
            return 0;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    private static int removeMajor(CommandContext<CommandSourceStack> ctx, String majorId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            if (BaFangYiData.removeMajor(player, majorId)) {
                source.sendSuccess(() -> Component.translatable(PREFIX + "remove_major.success", majorId), true);
                return 1;
            }
            source.sendFailure(Component.translatable(PREFIX + "remove_major.fail", majorId));
            return 0;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    private static int queryMajor(CommandContext<CommandSourceStack> ctx, String majorId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            boolean unlocked = BaFangYiData.queryMajor(player, majorId);
            Component status = unlocked
                    ? Component.translatable(PREFIX + "status.unlocked")
                    : Component.translatable(PREFIX + "status.locked");
            source.sendSuccess(() -> Component.translatable(PREFIX + "query_major", majorId, status), false);
            return 1;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    private static int addSub(CommandContext<CommandSourceStack> ctx, String majorId, String subId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            if (BaFangYiData.addSub(player, majorId, subId)) {
                source.sendSuccess(() -> Component.translatable(PREFIX + "add_sub.success", majorId + "." + subId), true);
                return 1;
            }
            source.sendFailure(Component.translatable(PREFIX + "add_sub.fail", majorId + "." + subId));
            return 0;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    private static int removeSub(CommandContext<CommandSourceStack> ctx, String majorId, String subId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            if (BaFangYiData.removeSub(player, majorId, subId)) {
                source.sendSuccess(() -> Component.translatable(PREFIX + "remove_sub.success", majorId + "." + subId), true);
                return 1;
            }
            source.sendFailure(Component.translatable(PREFIX + "remove_sub.fail", majorId + "." + subId));
            return 0;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    private static int querySub(CommandContext<CommandSourceStack> ctx, String majorId, String subId) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player player) {
            boolean unlocked = BaFangYiData.querySub(player, majorId, subId);
            Component status = unlocked
                    ? Component.translatable(PREFIX + "status.unlocked")
                    : Component.translatable(PREFIX + "status.locked");
            source.sendSuccess(() -> Component.translatable(PREFIX + "query_sub", majorId + "." + subId, status), false);
            return 1;
        }
        source.sendFailure(Component.translatable(PREFIX + "player_only"));
        return 0;
    }

    public static void handleTeleportRequest(BaFangYiTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BaFangYiData data = BaFangYiData.get(serverPlayer);

                if (!data.isMajorUnlocked(payload.majorId())) {
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "major_locked"), true);
                    return;
                }
                if (!data.isSubUnlocked(payload.majorId(), payload.subId())) {
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "sub_locked"), true);
                    return;
                }

                // 检查持久冷却
                long gameTime = serverPlayer.serverLevel().getGameTime();
                if (data.isTeleportOnCooldown(gameTime)) {
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "cooldown"), true);
                    return;
                }

                // 检查经验
                if (serverPlayer.totalExperience < 10) {
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "no_exp"), true);
                    return;
                }

                List<BaFangYiMajorEntry> allMajors = BaFangYiLoader.loadAll(serverPlayer.serverLevel());
                Optional<BaFangYiSubEntry> target = allMajors.stream()
                        .filter(m -> m.id().equals(payload.majorId()))
                        .flatMap(m -> m.poses().stream())
                        .filter(s -> s.id().equals(payload.subId()))
                        .findFirst();

                if (target.isPresent()) {
                    BaFangYiSubEntry sub = target.get();

                    // 扣除经验
                    serverPlayer.giveExperiencePoints(-10);

                    // 设置物品冷却
                    cooldown_remove.cd_remove(serverPlayer, ModItems.ba_fang_yi.get(), TELEPORT_COOLDOWN_TICKS);

                    // 设置持久冷却
                    long persistentCd = cooldown_remove.skill_cd_remove(serverPlayer, TELEPORT_COOLDOWN_TICKS);
                    data.setTeleportCooldown(gameTime, persistentCd);
                    serverPlayer.setData(ModAttachments.BA_FANG_YI_DATA.get(), data);

                    // 传送
                    serverPlayer.unRide();
                    serverPlayer.teleportTo(
                            serverPlayer.serverLevel(), sub.x(), sub.y(), sub.z(),
                            java.util.Collections.EMPTY_SET,
                            serverPlayer.getYRot(), serverPlayer.getXRot()
                    );
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "teleport.success", sub.title()), true);
                } else {
                    serverPlayer.sendSystemMessage(Component.translatable(PREFIX + "teleport.not_found"), true);
                }
            }
        });
    }
}
