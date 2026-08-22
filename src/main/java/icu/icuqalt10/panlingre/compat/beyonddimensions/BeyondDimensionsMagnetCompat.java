package icu.icuqalt10.panlingre.compat.beyonddimensions;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BeyondDimensionsMagnetCompat {
    public static final String MAGNET_SCREEN_CLASS =
            "com.wintercogs.beyonddimensions.client.gui.NetMagnetGUI";

    private static final ResourceLocation MAGNET_ITEM = id("net_magnet_item");
    private static final List<ModeSpec> MODE_SPECS = List.of(
            new ModeSpec("hopper_item_mode",
                    "com.wintercogs.beyonddimensions.common.machine.HopperItemMode", "ALLOW"),
            new ModeSpec("hopper_xp_mode",
                    "com.wintercogs.beyonddimensions.common.machine.HopperXpMode", "DENY"),
            new ModeSpec("hopper_nbt_mode",
                    "com.wintercogs.beyonddimensions.common.machine.HopperNBTMode", "ALLOW"),
            new ModeSpec("hopper_fluid_mode",
                    "com.wintercogs.beyonddimensions.common.machine.HopperFluidMode", "DENY")
    );

    private static volatile List<ModeBinding> modeBindings;

    private BeyondDimensionsMagnetCompat() {
    }

    public static void forceModes(ItemStack stack) {
        if (!BeyondDimensionsAccess.isInstalled()
                || stack.isEmpty()
                || !MAGNET_ITEM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }

        for (ModeBinding binding : modeBindings()) {
            setComponent(stack, binding.component(), binding.value());
        }
    }

    public static void forceModeTag(CompoundTag tag) {
        tag.putString("hopper_item_mode", "ALLOW");
        tag.putString("hopper_xp_mode", "DENY");
        tag.putString("hopper_nbt_mode", "ALLOW");
        tag.putString("hopper_fluid_mode", "DENY");
    }

    private static List<ModeBinding> modeBindings() {
        List<ModeBinding> bindings = modeBindings;
        if (bindings != null) return bindings;

        synchronized (BeyondDimensionsMagnetCompat.class) {
            bindings = modeBindings;
            if (bindings != null) return bindings;

            List<ModeBinding> resolved = new ArrayList<>(MODE_SPECS.size());
            try {
                for (ModeSpec spec : MODE_SPECS) {
                    DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id(spec.componentId()));
                    if (component == null) {
                        throw new IllegalStateException("Missing BeyondDimensions data component " + spec.componentId());
                    }

                    Class<?> enumClass = Class.forName(spec.enumClassName(), false,
                            BeyondDimensionsMagnetCompat.class.getClassLoader());
                    if (!enumClass.isEnum()) {
                        throw new IllegalStateException(spec.enumClassName() + " is not an enum");
                    }
                    Object value = enumConstant(enumClass, spec.valueName());
                    resolved.add(new ModeBinding(component, value));
                }
                bindings = List.copyOf(resolved);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                PanlingRE.LOGGER.error("Failed to lock BeyondDimensions network magnet modes", exception);
                bindings = List.of();
            }

            modeBindings = bindings;
            return bindings;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(Class<?> enumClass, String valueName) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, valueName);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setComponent(ItemStack stack, DataComponentType<?> component, Object value) {
        DataComponentType rawComponent = component;
        if (!value.equals(stack.get(rawComponent))) {
            stack.set(rawComponent, value);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BeyondDimensionsAccess.MOD_ID, path);
    }

    private record ModeSpec(String componentId, String enumClassName, String valueName) {
    }

    private record ModeBinding(DataComponentType<?> component, Object value) {
    }
}
