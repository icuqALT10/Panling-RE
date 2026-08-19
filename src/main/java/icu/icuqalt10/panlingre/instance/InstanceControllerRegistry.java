package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.instance.baihu.BaihuController;
import icu.icuqalt10.panlingre.instance.qinglong.QinglongController;
import icu.icuqalt10.panlingre.instance.xuanwu.XuanwuController;
import icu.icuqalt10.panlingre.instance.zhuque.ZhuqueController;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class InstanceControllerRegistry {
    private static final Map<ResourceLocation, Supplier<? extends InstanceController>> FACTORIES = new HashMap<>();

    static {
        register(ResourceLocation.fromNamespaceAndPath("panlingre", "baihu"), BaihuController::new);
        register(ResourceLocation.fromNamespaceAndPath("panlingre", "qinglong"), QinglongController::new);
        register(ResourceLocation.fromNamespaceAndPath("panlingre", "xuanwu"), XuanwuController::new);
        register(ResourceLocation.fromNamespaceAndPath("panlingre", "zhuque"), ZhuqueController::new);
    }

    private InstanceControllerRegistry() {
    }

    public static void register(ResourceLocation id, Supplier<? extends InstanceController> factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("Duplicate instance controller: " + id);
        }
    }

    public static Optional<InstanceController> create(ResourceLocation id) {
        Supplier<? extends InstanceController> factory = FACTORIES.get(id);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }
}
