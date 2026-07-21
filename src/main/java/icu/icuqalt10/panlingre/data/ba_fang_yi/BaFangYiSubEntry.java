package icu.icuqalt10.panlingre.data.ba_fang_yi;

import net.minecraft.network.chat.Component;

public record BaFangYiSubEntry(
        Component title,
        String id,
        String texture,
        double x,
        double y,
        double z
) {}
