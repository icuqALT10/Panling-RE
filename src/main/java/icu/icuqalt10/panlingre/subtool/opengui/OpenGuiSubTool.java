package icu.icuqalt10.panlingre.subtool.opengui;

import icu.icuqalt10.panlingre.subtool.opengui.command.OpenGuiCommand;
import icu.icuqalt10.panlingre.subtool.opengui.network.OpenGuiPackets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class OpenGuiSubTool {

    private OpenGuiSubTool() {
    }

    public static void init(IEventBus modEventBus) {
        OpenGuiPackets.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(OpenGuiSubTool::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        OpenGuiCommand.register(event.getDispatcher());
    }
}
