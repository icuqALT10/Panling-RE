package icu.icuqalt10.panlingre.looktip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class LookTipLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, LookTipData> LOOK_TIPS = new HashMap<>();

    public LookTipLoader() {
        super(GSON, "look_tip");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOOK_TIPS.clear();

        map.forEach((id, json) -> {
            try {
                LookTipData.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> PanlingRE.LOGGER.error("Failed to parse look tip {}: {}", id, error))
                        .ifPresent(data -> LOOK_TIPS.put(id, data));
            } catch (Exception e) {
                PanlingRE.LOGGER.error("Error loading look tip {}", id, e);
            }
        });

        PanlingRE.LOGGER.info("Loaded {} look tips", LOOK_TIPS.size());
    }

    public static Map<ResourceLocation, LookTipData> getLookTips() {
        return LOOK_TIPS;
    }
}
