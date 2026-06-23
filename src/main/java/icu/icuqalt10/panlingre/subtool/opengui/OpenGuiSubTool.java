package icu.icuqalt10.panlingre.subtool.opengui;

import icu.icuqalt10.panlingre.subtool.opengui.network.OpenGuiPackets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import icu.icuqalt10.panlingre.subtool.opengui.command.OpenGuiCommand;

public class OpenGuiSubTool {

    /**
     * 在主类构造函数中调用此方法即可启用本子功能
     * OpenGuiSubTool.init(modEventBus);
     */
    public static void init(IEventBus modEventBus) {
        // 注册网络包
        OpenGuiPackets.register(modEventBus);

        // 注册指令
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                OpenGuiCommand.register(event.getDispatcher())
        );
    }
}