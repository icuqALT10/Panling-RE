package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMenuMixin {
    @Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void panlingre$blockBeyondDimensionsStorage(
            @Nullable MenuProvider provider,
            @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter,
            CallbackInfoReturnable<OptionalInt> cir
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!BeyondDimensionsAccess.shouldBlock(player.getServer(), provider)) return;

        player.displayClientMessage(Component.translatable("command.panlingre.byd.blocked"), true);
        cir.setReturnValue(OptionalInt.empty());
    }
}
