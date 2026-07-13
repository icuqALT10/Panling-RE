package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.init.ModRecipes;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class ldlMenu extends AbstractContainerMenu {
    private final SimpleContainer container = new SimpleContainer(6);
    private final ContainerLevelAccess access;
    private final Player player;

    public ldlMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, ContainerLevelAccess.create(playerInv.player.level(), buf.readBlockPos()));
    }

    public ldlMenu(int containerId, Inventory playerInv, ContainerLevelAccess access) {
        super(ModMenus.ldl_menu.get(), containerId);
        this.access = access;
        this.player = playerInv.player;

        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(this.container, i, 44 + i * 18, 53) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    ldlMenu.this.checkRecipe();
                }
            });
        }

        // 输出槽位 (5)
        this.addSlot(new Slot(this.container, 5, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                ldlMenu.this.consumeIngredients();
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
                return 5;
            }
        };
    }

    private boolean isUnlocked(RecipeHolder<LdlRecipe> recipeHolder) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return serverPlayer.getRecipeBook().contains(recipeHolder);
    }

    private void checkRecipe() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.LDL_TYPE.get(), createInput(), level);

        if (recipeOpt.isPresent()) {

            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(recipeOpt.get())) {
                container.setItem(5, ItemStack.EMPTY);
            } else {
                container.setItem(5, recipeOpt.get().value().assemble(createInput(), level.registryAccess()));
            }
        } else {
            container.setItem(5, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    private void consumeIngredients() {
        Level level = player.level();
        if (level.isClientSide) return;

        var recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.LDL_TYPE.get(), createInput(), level);
        recipeOpt.ifPresent(r -> {
            if (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !isUnlocked(r)) return;

            for (LdlRecipe.SlotIngredient si : r.value().slotIngredients()) {
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

            if (index == 5) {
                while (this.slots.get(5).hasItem()) {
                    ItemStack currentOutput = this.slots.get(5).getItem();
                    ItemStack toCopy = currentOutput.copy();

                    if (!this.moveItemStackTo(currentOutput, 6, 42, true)) {
                        break;
                    }

                    this.consumeIngredients();
                    slot.onQuickCraft(currentOutput, toCopy);

                    if (currentOutput.getCount() == toCopy.getCount()) break;
                }
            } else if (index < 7) {
                if (!this.moveItemStackTo(itemstack1, 6, 42, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 5, false)) return ItemStack.EMPTY;
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
        if (!player.level().isClientSide) {
            for (int i = 0; i < 5; i++) {
                ItemStack stack = this.container.removeItem(i, this.container.getItem(i).getCount());
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ldl.get());
    }

    private void addPlayerInventorySlots(Inventory inv) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
}