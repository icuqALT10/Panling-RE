package icu.icuqalt10.panlingre.skill;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record SkillData(String id, @Nullable ResourceLocation icon, String name, float lingqiCost, long cooldown) {}
