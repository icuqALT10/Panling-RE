package icu.icuqalt10.panlingre.subtool.opengui.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import icu.icuqalt10.panlingre.subtool.opengui.SimpleMerchant;
import icu.icuqalt10.panlingre.subtool.opengui.data.CustomTradeLoader;
import icu.icuqalt10.panlingre.subtool.opengui.data.TradeData;
import icu.icuqalt10.panlingre.subtool.opengui.network.OpenVillagerGuiPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class OpenGuiCommand {

    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("example_trades"),
                    builder
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("plre")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("opengui")
                            .then(Commands.literal("villager")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("trade_file", StringArgumentType.string())
                                                    .suggests(FILE_SUGGESTIONS)
                                                    .executes(ctx -> execute(
                                                            ctx.getSource(),
                                                            EntityArgument.getPlayer(ctx, "player"),
                                                            StringArgumentType.getString(ctx, "trade_file")
                                                    ))
                                            )
                                    )
                            )
                        )
        );
    }

    private static int execute(CommandSourceStack source, ServerPlayer player, String tradeFile) {
        TradeData data = CustomTradeLoader.load(source.getServer(), tradeFile);

        if (data == null || data.offers().isEmpty()) {
            source.sendFailure(Component.literal(
                    "§c无法加载交易文件 \"" + tradeFile + "\"，请确认文件存在于数据包中。"
            ));
            return 0;
        }

        MerchantOffers offers = data.offers();
        Component title = data.title();
        SimpleMerchant merchant = new SimpleMerchant(offers);

        // 用原版 SimpleMenuProvider + MerchantMenu 走完整的服务端 openMenu 流程
        // 这样 containerId 真实存在，交易物品操作完全正常
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> {
                    merchant.setTradingPlayer(p);
                    return new MerchantMenu(containerId, playerInventory, merchant);
                },
                title
        ));

        // openMenu 是同步的，完成后再发包让客户端覆盖 offers 数据和 title
        PacketDistributor.sendToPlayer(player, new OpenVillagerGuiPacket(offers, title));

        source.sendSuccess(() -> Component.literal(
                "§a已为 §e" + player.getName().getString() +
                        " §a打开自定义村民交易界面（共 §e" + offers.size() + " §a项）"
        ), true);

        return 1;
    }
}