package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.instance.qinglong.QinglongController;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class InstanceControllerRegistry {
    private static final Map<ResourceLocation, Supplier<? extends InstanceController>> FACTORIES = new HashMap<>();

    static {
        register(ResourceLocation.fromNamespaceAndPath("panlingre", "qinglong"), QinglongController::new);
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
