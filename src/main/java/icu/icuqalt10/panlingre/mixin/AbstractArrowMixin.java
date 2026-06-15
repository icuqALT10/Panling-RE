package icu.icuqalt10.panlingre.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowMixin {

    // 使用 @Invoker 绑定源码第 338 行的私有方法
    @Invoker("setPierceLevel")
    void invokeSetPierceLevel(byte level);
}