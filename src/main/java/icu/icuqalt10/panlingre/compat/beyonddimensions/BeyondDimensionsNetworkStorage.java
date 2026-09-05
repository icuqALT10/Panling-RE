package icu.icuqalt10.panlingre.compat.beyonddimensions;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Reflection-only bridge so BeyondDimensions remains an optional dependency. */
public final class BeyondDimensionsNetworkStorage {
    private static volatile Bindings bindings;
    private static volatile boolean initializationAttempted;
    private static boolean failureLogged;

    private BeyondDimensionsNetworkStorage() {
    }

    public static int getPlayerNetworkId(Player player) {
        Bindings resolved = bindings();
        if (resolved == null) return -1;

        try {
            Object network = resolved.getNetFromPlayer().invoke(null, player);
            return network == null ? -1 : (int) resolved.getId().invoke(network);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailure(exception);
            return -1;
        }
    }

    public static int insert(int networkId, ItemStack stack) {
        if (networkId < 0 || stack.isEmpty()) return 0;
        Bindings resolved = bindings();
        if (resolved == null) return 0;

        try {
            Object network = resolved.getNetFromId().invoke(null, networkId);
            if (network == null) return 0;

            Object storage = resolved.getUnifiedStorage().invoke(network);
            Object key = resolved.itemStackKeyConstructor().newInstance(stack);
            Object remainder = resolved.insert().invoke(storage, key, (long) stack.getCount(), false);
            long remainderAmount = (long) resolved.amount().invoke(remainder);
            return (int) Math.max(0L, Math.min(stack.getCount(), (long) stack.getCount() - remainderAmount));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailure(exception);
            return 0;
        }
    }

    private static Bindings bindings() {
        if (!BeyondDimensionsAccess.isInstalled()) return null;
        Bindings resolved = bindings;
        if (resolved != null || initializationAttempted) return resolved;

        synchronized (BeyondDimensionsNetworkStorage.class) {
            resolved = bindings;
            if (resolved != null || initializationAttempted) return resolved;
            initializationAttempted = true;

            try {
                ClassLoader loader = BeyondDimensionsNetworkStorage.class.getClassLoader();
                Class<?> networkClass = Class.forName(
                        "com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet", false, loader);
                Class<?> storageClass = Class.forName(
                        "com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage", false, loader);
                Class<?> stackKeyClass = Class.forName(
                        "com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey", false, loader);
                Class<?> keyClass = Class.forName(
                        "com.wintercogs.beyonddimensions.api.storage.key.IStackKey", false, loader);
                Class<?> keyAmountClass = Class.forName(
                        "com.wintercogs.beyonddimensions.api.storage.key.KeyAmount", false, loader);

                resolved = new Bindings(
                        networkClass.getMethod("getNetFromPlayer", Player.class),
                        networkClass.getMethod("getNetFromId", int.class),
                        networkClass.getMethod("getId"),
                        networkClass.getMethod("getUnifiedStorage"),
                        stackKeyClass.getConstructor(ItemStack.class),
                        storageClass.getMethod("insert", keyClass, long.class, boolean.class),
                        keyAmountClass.getMethod("amount")
                );
                bindings = resolved;
                return resolved;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                logFailure(exception);
                return null;
            }
        }
    }

    private static void logFailure(Throwable throwable) {
        if (failureLogged) return;
        failureLogged = true;
        PanlingRE.LOGGER.error("Failed to access BeyondDimensions storage for the Na Wu Ci magnet", throwable);
    }

    private record Bindings(
            Method getNetFromPlayer,
            Method getNetFromId,
            Method getId,
            Method getUnifiedStorage,
            Constructor<?> itemStackKeyConstructor,
            Method insert,
            Method amount
    ) {
    }
}
