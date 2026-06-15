package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.init.ModRecipes;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
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

public class dztMenu extends AbstractContainerMenu {
    private final SimpleContainer container = new SimpleContainer(4);
    private final ContainerLevelAccess access;
    private final Player player;

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
                dztMenu.this.checkRecipe();
            }
        });
        this.addSlot(new Slot(this.container, 1, 62, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                dztMenu.this.checkRecipe();
            }
        });
        this.addSlot(new Slot(this.container, 2, 62, 53) {
            @Override
            public void setChanged() {
                super.setChanged();
                dztMenu.this.checkRecipe();
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
                return 3;
            }
        };
    }

    private boolean isUnlocked(RecipeHolder<dztRecipe> recipeHolder) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return serverPlayer.getRecipeBook().contains(recipeHolder);
    }

    private void checkRecipe() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.DZT_TYPE.get(), createInput(), level);

        if (recipeOpt.isPresent()) {

            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(recipeOpt.get())) {
                container.setItem(3, ItemStack.EMPTY);
            } else {
                container.setItem(3, recipeOpt.get().value().assemble(createInput(), level.registryAccess()));
            }
        } else {
            container.setItem(3, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    private void consumeIngredients() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.DZT_TYPE.get(), createInput(), level);
        recipeOpt.ifPresent(r -> {
            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(r)) return;

            for (dztRecipe.SlotIngredient si : r.value().slotIngredients()) {
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

            if (index == 3) {
                while (this.slots.get(3).hasItem()) {
                    ItemStack currentOutput = this.slots.get(3).getItem();
                    ItemStack toCopy = currentOutput.copy();

                    if (!this.moveItemStackTo(currentOutput, 4, 40, true)) {
                        break;
                    }

                    this.consumeIngredients();
                    slot.onQuickCraft(currentOutput, toCopy);

                    if (currentOutput.getCount() == toCopy.getCount()) break;
                }
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