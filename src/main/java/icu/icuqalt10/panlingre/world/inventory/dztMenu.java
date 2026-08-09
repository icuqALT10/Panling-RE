package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.init.ModRecipes;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.List;

public class dztMenu extends AbstractContainerMenu {
    public static final int SELECT_PREVIOUS_RECIPE = 0;
    public static final int SELECT_NEXT_RECIPE = 1;

    private final SimpleContainer container = new SimpleContainer(4);
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final DataSlot matchingRecipeCount = DataSlot.standalone();
    private List<RecipeHolder<dztRecipe>> matchingRecipes = List.of();

    public dztMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, ContainerLevelAccess.create(playerInv.player.level(), buf.readBlockPos()));
    }

    public dztMenu(int containerId, Inventory playerInv, ContainerLevelAccess access) {
        super(ModMenus.dzt_menu.get(), containerId);
        this.access = access;
        this.player = playerInv.player;

        //输入
        this.addSlot(new Slot(this.container, 0, 62, 17) {
            @Override
            public void setChanged() {
                super.setChanged();
                dztMenu.this.checkRecipe(true);
            }
        });
        this.addSlot(new Slot(this.container, 1, 62, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                dztMenu.this.checkRecipe(true);
            }
        });
        this.addSlot(new Slot(this.container, 2, 62, 53) {
            @Override
            public void setChanged() {
                super.setChanged();
                dztMenu.this.checkRecipe(true);
            }
        });

        //输出
        this.addSlot(new Slot(this.container, 3, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                dztMenu.this.consumeIngredients();
                super.onTake(player, stack);
            }
        });

        addPlayerInventorySlots(playerInv);
        this.addDataSlot(this.selectedRecipeIndex);
        this.addDataSlot(this.matchingRecipeCount);
        this.checkRecipe(true);
    }

    private RecipeInput createInput() {
        return new RecipeInput() {
            @Override
            public ItemStack getItem(int index) {
                return container.getItem(index);
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

    private boolean isUnlocked(RecipeHolder<dztRecipe> recipeHolder) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return serverPlayer.getRecipeBook().contains(recipeHolder);
    }

    private void checkRecipe(boolean resetSelection) {
        Level level = player.level();
        if (level.isClientSide) return;

        int oldIndex = this.selectedRecipeIndex.get();
        ResourceLocation oldRecipeId = oldIndex >= 0 && oldIndex < this.matchingRecipes.size()
                ? this.matchingRecipes.get(oldIndex).id()
                : null;
        boolean limitedCrafting = level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING);
        RecipeInput input = createInput();
        this.matchingRecipes = input.isEmpty()
                ? List.of()
                : level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.DZT_TYPE.get())
                        .stream()
                        .filter(recipe -> recipe.value().matches(input, level))
                        .filter(recipe -> !limitedCrafting || isUnlocked(recipe))
                        .toList();

        this.matchingRecipeCount.set(this.matchingRecipes.size());
        if (resetSelection) {
            this.selectedRecipeIndex.set(0);
        } else {
            int retainedIndex = -1;
            if (oldRecipeId != null) {
                for (int i = 0; i < this.matchingRecipes.size(); i++) {
                    if (this.matchingRecipes.get(i).id().equals(oldRecipeId)) {
                        retainedIndex = i;
                        break;
                    }
                }
            }
            this.selectedRecipeIndex.set(retainedIndex >= 0
                    ? retainedIndex
                    : Math.min(oldIndex, Math.max(0, this.matchingRecipes.size() - 1)));
        }

        this.updateResult();
        this.broadcastChanges();
    }

    private void updateResult() {
        int index = this.selectedRecipeIndex.get();
        if (index >= 0 && index < this.matchingRecipes.size()) {
            dztRecipe recipe = this.matchingRecipes.get(index).value();
            this.container.setItem(3, recipe.assemble(createInput(), this.player.level().registryAccess()));
        } else {
            this.container.setItem(3, ItemStack.EMPTY);
        }
    }

    private void consumeIngredients() {
        Level level = player.level();
        if (level.isClientSide) return;

        int index = this.selectedRecipeIndex.get();
        if (index >= 0 && index < this.matchingRecipes.size()) {
            RecipeHolder<dztRecipe> selectedRecipe = this.matchingRecipes.get(index);
            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(selectedRecipe)) {
                return;
            }

            for (dztRecipe.SlotIngredient si : selectedRecipe.value().slotIngredients()) {
                container.removeItem(si.slot(), si.ingredient().count());
            }
            checkRecipe(false);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != SELECT_PREVIOUS_RECIPE && id != SELECT_NEXT_RECIPE) {
            return false;
        }

        int recipeCount = this.matchingRecipeCount.get();
        if (recipeCount < 2) {
            return false;
        }

        int direction = id == SELECT_PREVIOUS_RECIPE ? -1 : 1;
        this.selectedRecipeIndex.set(Math.floorMod(this.selectedRecipeIndex.get() + direction, recipeCount));

        if (!player.level().isClientSide) {
            this.updateResult();
            this.broadcastChanges();
        }
        return true;
    }

    public boolean hasMultipleRecipes() {
        return this.matchingRecipeCount.get() > 1;
    }

    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex.get();
    }

    public int getMatchingRecipeCount() {
        return this.matchingRecipeCount.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 3) {
                if (!this.moveItemStackTo(itemstack1, 4, 40, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index < 3) {
                if (!this.moveItemStackTo(itemstack1, 4, 40, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 3, false)) return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            for (int i = 0; i < 3; i++) {
                ItemStack stack = this.container.removeItem(i, container.getItem(i).getCount());
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.dzt.get());
    }

    private void addPlayerInventorySlots(Inventory inv) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
}
