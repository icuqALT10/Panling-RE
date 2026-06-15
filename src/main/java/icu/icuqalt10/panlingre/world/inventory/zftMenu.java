package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.init.ModRecipes;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class zftMenu extends AbstractContainerMenu {
    private final SimpleContainer container = new SimpleContainer(8);
    private final ContainerLevelAccess access;
    private final Player player;

    public zftMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, ContainerLevelAccess.create(playerInv.player.level(), buf.readBlockPos()));
    }

    public zftMenu(int containerId, Inventory playerInv, ContainerLevelAccess access) {
        super(ModMenus.zft_menu.get(), containerId);
        this.access = access;
        this.player = playerInv.player;

        //沙子
        this.addSlot(new Slot(this.container, 0, 8, 7) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        //符纸
        this.addSlot(new Slot(this.container, 1, 59, 34) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        //从上顺时针
        this.addSlot(new Slot(this.container, 2, 59, 5) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        this.addSlot(new Slot(this.container, 3, 89, 28) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        this.addSlot(new Slot(this.container, 4, 80, 58) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        this.addSlot(new Slot(this.container, 5, 38, 58) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        this.addSlot(new Slot(this.container, 6, 29, 28) {
            @Override
            public void setChanged() {
                super.setChanged();
                zftMenu.this.checkRecipe();
            }
        });

        this.addSlot(new Slot(this.container, 7, 144, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                zftMenu.this.consumeIngredients();
                super.onTake(player, stack);
            }
        });

        addPlayerInventorySlots(playerInv);
        this.checkRecipe();
    }

    private RecipeInput createInput() {
        return new RecipeInput() {
            @Override
            public ItemStack getItem(int index) {
                return container.getItem(index);
            }

            @Override
            public int size() {
                return 7;
            }
        };
    }

    private boolean isUnlocked(RecipeHolder<zftRecipe> recipeHolder) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return serverPlayer.getRecipeBook().contains(recipeHolder);
    }

    private void checkRecipe() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.ZFT_TYPE.get(), createInput(), level);

        if (recipeOpt.isPresent()) {

            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(recipeOpt.get())) {
                container.setItem(7, ItemStack.EMPTY);
            } else {
                container.setItem(7, recipeOpt.get().value().assemble(createInput(), level.registryAccess()));
            }
        } else {
            container.setItem(7, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    private void consumeIngredients() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.ZFT_TYPE.get(), createInput(), level);
        recipeOpt.ifPresent(r -> {
            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(r)) return;

            for (zftRecipe.SlotIngredient si : r.value().slotIngredients()) {
                container.removeItem(si.slot(), si.ingredient().count());
            }
            checkRecipe();
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 7) {
                while (this.slots.get(7).hasItem()) {
                    ItemStack currentOutput = this.slots.get(7).getItem();
                    ItemStack toCopy = currentOutput.copy();

                    if (!this.moveItemStackTo(currentOutput, 8, 44, true)) {
                        break;
                    }

                    this.consumeIngredients();
                    slot.onQuickCraft(currentOutput, toCopy);

                    if (currentOutput.getCount() == toCopy.getCount()) break;
                }
            } else if (index < 7) {
                if (!this.moveItemStackTo(itemstack1, 8, 44, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 7, false)) return ItemStack.EMPTY;
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
            for (int i = 0; i < 7; i++) {
                ItemStack stack = this.container.removeItem(i, container.getItem(i).getCount());
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.zft.get());
    }

    private void addPlayerInventorySlots(Inventory inv) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
}