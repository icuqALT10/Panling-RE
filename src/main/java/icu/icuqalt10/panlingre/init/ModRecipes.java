package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, PanlingRE.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, PanlingRE.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<LdlRecipe>> LDL_TYPE =
            RECIPE_TYPES.register("ldl_crafting", () -> new RecipeType<LdlRecipe>() {});

    public static final DeferredHolder<RecipeType<?>, RecipeType<zftRecipe>> ZFT_TYPE =
            RECIPE_TYPES.register("zft_crafting", () -> new RecipeType<zftRecipe>() {});

    public static final DeferredHolder<RecipeType<?>, RecipeType<dztRecipe>> DZT_TYPE =
            RECIPE_TYPES.register("dzt_crafting", () -> new RecipeType<dztRecipe>() {});

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LdlRecipe>> LDL_SERIALIZER =
            SERIALIZERS.register("ldl_crafting", LdlRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<zftRecipe>> ZFT_SERIALIZER =
            SERIALIZERS.register("zft_crafting", zftRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dztRecipe>> DZT_SERIALIZER =
            SERIALIZERS.register("dzt_crafting", dztRecipe.Serializer::new);
}