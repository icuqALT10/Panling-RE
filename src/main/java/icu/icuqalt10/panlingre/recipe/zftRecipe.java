package icu.icuqalt10.panlingre.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icu.icuqalt10.panlingre.init.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public record zftRecipe(List<SlotIngredient> slotIngredients, ItemStack result) implements Recipe<RecipeInput> {

    public Ingredient getIngredientAt(int index) {
        return slotIngredients.stream()
                .filter(si -> si.slot() == index)
                .map(si -> si.ingredient().ingredient())
                .findFirst()
                .orElse(Ingredient.EMPTY);
    }

    public int getCountAt(int index) {
        return slotIngredients.stream()
                .filter(si -> si.slot() == index)
                .map(si -> si.ingredient().count())
                .findFirst()
                .orElse(0);
    }

    public record SlotIngredient(SizedIngredient ingredient, int slot) {
        public static final MapCodec<SlotIngredient> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SizedIngredient.FLAT_CODEC.fieldOf("ingredient").forGetter(SlotIngredient::ingredient),
                Codec.INT.fieldOf("slot").forGetter(SlotIngredient::slot)
        ).apply(inst, SlotIngredient::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SlotIngredient> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC, SlotIngredient::ingredient,
                ByteBufCodecs.VAR_INT, SlotIngredient::slot,
                SlotIngredient::new
        );
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (level.isClientSide) return false;

        boolean[] occupiedSlots = new boolean[7];

        for (SlotIngredient si : slotIngredients) {
            int targetSlot = si.slot();
            if (targetSlot < 0 || targetSlot >= 7) continue;

            ItemStack stackInSlot = input.getItem(targetSlot);

            if (!si.ingredient().test(stackInSlot) || stackInSlot.getCount() < si.ingredient().count()) {
                return false;
            }
            occupiedSlots[targetSlot] = true;
        }

        for (int i = 0; i < 7; i++) {
            if (!occupiedSlots[i] && !input.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ZFT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ZFT_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<zftRecipe> {
        public static final MapCodec<zftRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SlotIngredient.CODEC.codec().listOf().fieldOf("ingredients").forGetter(zftRecipe::slotIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(zftRecipe::result)
        ).apply(inst, zftRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, zftRecipe> STREAM_CODEC = StreamCodec.composite(
                SlotIngredient.STREAM_CODEC.apply(ByteBufCodecs.collection(java.util.ArrayList::new)),
                zftRecipe::slotIngredients,
                ItemStack.STREAM_CODEC,
                zftRecipe::result,
                zftRecipe::new
        );

        @Override
        public MapCodec<zftRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, zftRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}