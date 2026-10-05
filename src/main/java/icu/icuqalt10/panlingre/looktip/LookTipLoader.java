package icu.icuqalt10.panlingre.looktip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class LookTipLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile LoadedTips loaded = new LoadedTips(Map.of(), Map.of());

    public LookTipLoader() {
        super(GSON, "look_tip");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, LookTipData> tips = new HashMap<>();
        Map<String, CompoundTag> requiredNbts = new HashMap<>();

        map.forEach((id, json) -> {
            try {
                LookTipData data = LookTipData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                for (LookTipData.EntityCondition condition : data.entries()) {
                    if (condition.nbt().isPresent() && !condition.nbt().get().isEmpty()) {
                        String nbt = condition.nbt().get();
                        if (!requiredNbts.containsKey(nbt)) requiredNbts.put(nbt, TagParser.parseTag(nbt));
                    }
                }
                tips.put(id, data);
            } catch (Exception e) {
                PanlingRE.LOGGER.error("Error loading look tip {}", id, e);
            }
        });

        loaded = new LoadedTips(Map.copyOf(tips), Map.copyOf(requiredNbts));
        PanlingRE.LOGGER.info("Loaded {} look tips", tips.size());
    }

    public static Map<ResourceLocation, LookTipData> getLookTips() {
        return loaded.tips();
    }

    public static CompoundTag getRequiredNbt(String nbt) {
        return loaded.requiredNbts().get(nbt);
    }

    private record LoadedTips(Map<ResourceLocation, LookTipData> tips, Map<String, CompoundTag> requiredNbts) {
    }
}
