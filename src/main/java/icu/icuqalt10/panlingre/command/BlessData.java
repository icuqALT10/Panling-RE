package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument; // 新增导入
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer; // 推荐使用 ServerPlayer
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class BlessData {

    private static final List<String> VALID_BLESSES = List.of("qinglong", "zhuque", "baihu", "xuanwu");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("curios")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", StringArgumentType.string())
                                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                        .executes(context -> {
                                                            var source = context.getSource();

                                                            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

                                                            String slotIdentifier = StringArgumentType.getString(context, "slot");
                                                            Item targetItem = ItemArgument.getItem(context, "item").getItem();

                                                            boolean hasItem = CuriosApi.getCuriosInventory(targetPlayer).map(handler -> {
                                                                return handler.getStacksHandler(slotIdentifier).map(stacksHandler -> {
                                                                    var stacks = stacksHandler.getStacks();
                                                                    for (int i = 0; i < stacks.getSlots(); i++) {
                                                                        if (stacks.getStackInSlot(i).is(targetItem)) {
                                                                            return true;
                                                                        }
                                                                    }
                                                                    return false;
                                                                }).orElse(false);
                                                            }).orElse(false);

                                                            String targetName = targetPlayer.getScoreboardName();

                                                            if (hasItem) {
                                                                source.sendSuccess(() -> Component.literal("§a[PanlingRE]检测成功！玩家 " + targetName + " 装备了该物品。"), false);
                                                                return 1;
                                                            } else {
                                                                source.sendFailure(Component.literal("§c[PanlingRE]检测失败！玩家 " + targetName + " 未装备该物品。"));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
        );

        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("bless")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("bless", StringArgumentType.string())
                                                        .suggests((context, builder) -> suggestBlessTypes(builder))
                                                        .executes(context -> handleBless(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "bless"), "add"))
                                                )
                                        )
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("bless", StringArgumentType.string())
                                                        .suggests((context, builder) -> suggestBlessTypes(builder))
                                                        .executes(context -> handleBless(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "bless"), "remove"))
                                                )
                                        )
                                        .then(Commands.literal("query")
                                                .then(Commands.argument("bless", StringArgumentType.string())
                                                        .suggests((context, builder) -> suggestBlessTypes(builder))
                                                        .executes(context -> handleBless(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "bless"), "query"))
                                                )
                                        )
                                )
                        )
        );

    }

    private static CompletableFuture<Suggestions> suggestBlessTypes(SuggestionsBuilder builder) {
        for (String bless : VALID_BLESSES) {
            if (bless.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(bless);
            }
        }
        return builder.buildFuture();
    }

    private static int handleBless(CommandSourceStack source, ServerPlayer target, String blessType, String action) {
        String type = blessType.toLowerCase();

        if (!VALID_BLESSES.contains(type)) {
            source.sendFailure(Component.literal("§c[PanlingRE] 未知的祝福类型: " + blessType));
            return 0;
        }

        String targetName = target.getScoreboardName();

        switch (action) {
            case "query" -> {
                boolean isEnabled = icu.icuqalt10.panlingre.attachment.BlessData.hasBless(target, type);
                source.sendSuccess(() -> Component.literal(String.format("§a[PanlingRE] 玩家 %s 的 %s 状态为: %b", targetName, type, isEnabled)), false);
                return isEnabled ? 1 : 0;
            }
            case "add" -> {
                if (icu.icuqalt10.panlingre.attachment.BlessData.addBless(target, type)) {
                    source.sendSuccess(() -> Component.literal(String.format("§a[PanlingRE] 已成功为 %s 开启 %s 祝福", targetName, type)), true);
                    return 1;
                } else {
                    source.sendFailure(Component.literal(String.format("§e[PanlingRE] 玩家 %s 本来就拥有 %s 祝福", targetName, type)));
                    return 0;
                }
            }
            case "remove" -> {
                if (icu.icuqalt10.panlingre.attachment.BlessData.removeBless(target, type)) {
                    source.sendSuccess(() -> Component.literal(String.format("§a[PanlingRE] 已成功移除 %s 的 %s 祝福", targetName, type)), true);
                    return 1;
                } else {
                    source.sendFailure(Component.literal(String.format("§e[PanlingRE] 玩家 %s 本来就没有 %s 祝福", targetName, type)));
                    return 0;
                }
            }
            default -> {
                return 0;
            }
        }
    }

}