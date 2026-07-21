package icu.icuqalt10.panlingre.subtool.opengui.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.subtool.opengui.CustomMerchantMenu;
import icu.icuqalt10.panlingre.subtool.opengui.data.CustomTradeLoader;
import icu.icuqalt10.panlingre.subtool.opengui.data.TradeData;
import icu.icuqalt10.panlingre.subtool.opengui.network.OpenVillagerGuiPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class OpenGuiCommand {

    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTIONS =
            (ctx, builder) -> {
                String remaining = builder.getRemaining().trim();
                // 如果已经输入了 JSON 开头，不提供文件建议
                if (remaining.startsWith("{")) {
                    return builder.buildFuture();
                }
                ResourceManager rm = ctx.getSource().getServer().getResourceManager();
                List<String> names = new ArrayList<>();
                try {
                    rm.listResources("custom_trades",
                            loc -> loc.getNamespace().equals(PanlingRE.MODID)
                                    && loc.getPath().endsWith(".json")
                    ).keySet().forEach(rl -> {
                        String path = rl.getPath();
                        String name = path.substring("custom_trades/".length(),
                                path.length() - ".json".length());
                        names.add(name);
                    });
                } catch (Exception ignored) {
                    names.add("example_trades");
                }
                names.sort(String.CASE_INSENSITIVE_ORDER);
                return SharedSuggestionProvider.suggest(names, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("plre")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("opengui")
                            .then(Commands.literal("villager")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("trade_file", StringArgumentType.greedyString())
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

    private static int execute(CommandSourceStack source, ServerPlayer player, String input) {
        String trimmed = input.trim();
        TradeData data;
        if (trimmed.startsWith("{")) {
            data = CustomTradeLoader.loadFromJson(source.getServer(), trimmed);
        } else {
            data = CustomTradeLoader.load(source.getServer(), trimmed);
        }

        if (data == null || data.offers().isEmpty()) {
            String hint = trimmed.startsWith("{")
                    ? "§c无法解析内联 JSON，请检查格式是否正确。"
                    : "§c无法加载交易文件 \"" + trimmed + "\"，请确认文件存在于数据包中。";
            source.sendFailure(Component.literal(hint));
            return 0;
        }

        MerchantOffers offers = data.offers();
        Component title = data.title();

        // 用自定义 MenuType 打开，CustomMerchantMenu 控制所有匹配逻辑
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) ->
                        new CustomMerchantMenu(containerId, playerInventory, offers),
                title
        ));

        // 发包同步 offers 到客户端（供 MerchantScreen 显示交易列表）
        PacketDistributor.sendToPlayer(player, new OpenVillagerGuiPacket(offers, title));

        source.sendSuccess(() -> Component.literal(
                "§a已为 §e" + player.getName().getString() +
                        " §a打开自定义村民交易界面（共 §e" + offers.size() + " §a项）"
        ), true);

        return 1;
    }
}