package icu.icuqalt10.panlingre.data.ba_fang_yi;

import net.minecraft.network.chat.Component;

import java.util.List;

public record BaFangYiMajorEntry(
        Component title,
        String id,
        String texture,
        List<BaFangYiSubEntry> poses
) {}
