package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.component.WeaponCaseContents;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.item.common.WeaponCaseItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WeaponCaseMenu extends AbstractContainerMenu {
    public static final int CASE_SLOT_COUNT = WeaponCaseContents.SLOT_COUNT;
    public static final int CASE_SLOT_X = 26;
    public static final int CASE_SLOT_Y = 28;
    public static final int CASE_SLOT_SPACING = 27;
    private static final int PLAYER_SLOT_START = CASE_SLOT_COUNT;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final Inventory playerInventory;
    private final SimpleContainer caseView = new SimpleContainer(CASE_SLOT_COUNT);
    private final ItemStack serverCaseStack;
    private final int sourceInventorySlot;

    public WeaponCaseMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, null, buffer.readVarInt());
    }

    public WeaponCaseMenu(int containerId, Inventory inventory,
                          ItemStack caseStack, int sourceInventorySlot) {
        super(ModMenus.weapon_case_menu.get(), containerId);
        this.playerInventory = inventory;
        this.serverCaseStack = caseStack;
        this.sourceInventorySlot = sourceInventorySlot;

        ItemStack initialCase = caseStack != null ? caseStack : inventory.getItem(sourceInventorySlot);
        refreshCaseView(initialCase);

        for (int i = 0; i < CASE_SLOT_COUNT; i++) {
            this.addSlot(new Slot(caseView, i,
                    CASE_SLOT_X + i * CASE_SLOT_SPACING, CASE_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack currentCase = serverCaseStack != null
                            ? serverCaseStack
                            : playerInventory.getItem(WeaponCaseMenu.this.sourceInventorySlot);
                    return currentCase.getItem() instanceof WeaponCaseItem
                            && WeaponCaseItem.canStore(playerInventory.player, currentCase, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        addPlayerInventorySlots(inventory);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < CASE_SLOT_COUNT) {
            if (serverCaseStack == null) return;

            if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
                ItemStack carried = getCarried();
                if (carried.isEmpty()) extractToCursor(slotId);
                else depositIntoSlot(slotId, carried);
            } else if (clickType == ClickType.QUICK_MOVE && button == 0) {
                extractToInventory(slotId);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (serverCaseStack == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (index < CASE_SLOT_COUNT) return extractToInventory(index);

        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack sourceStack = source.getItem();
        if (!WeaponCaseItem.canStore(player, serverCaseStack, sourceStack)) return ItemStack.EMPTY;

        ItemStack original = sourceStack.copy();
        int target = firstEmptyCaseSlot();
        if (target < 0 || !depositIntoSlot(target, sourceStack)) return ItemStack.EMPTY;

        if (sourceStack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        original.setCount(1);
        return original;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= CASE_SLOT_COUNT && super.canDragTo(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        if (serverCaseStack == null) return true;
        return player.isAlive()
                && sourceInventorySlot >= 0
                && sourceInventorySlot < playerInventory.getContainerSize()
                && playerInventory.getItem(sourceInventorySlot) == serverCaseStack
                && serverCaseStack.getItem() instanceof WeaponCaseItem;
    }

    private void extractToCursor(int caseSlot) {
        WeaponCaseContents contents = WeaponCaseItem.getContents(serverCaseStack);
        WeaponCaseContents.Entry entry = contents.get(caseSlot).orElse(null);
        if (entry == null) return;
        setCarried(entry.stack().copyWithCount(1));
        setCaseContents(contents.remove(caseSlot));
    }

    private ItemStack extractToInventory(int caseSlot) {
        WeaponCaseContents contents = WeaponCaseItem.getContents(serverCaseStack);
        WeaponCaseContents.Entry entry = contents.get(caseSlot).orElse(null);
        if (entry == null) return ItemStack.EMPTY;

        ItemStack output = entry.stack().copyWithCount(1);
        ItemStack result = output.copy();
        moveItemStackTo(output, PLAYER_SLOT_START, PLAYER_SLOT_END, true);
        if (!output.isEmpty()) return ItemStack.EMPTY;

        setCaseContents(contents.remove(caseSlot));
        return result;
    }

    private boolean depositIntoSlot(int caseSlot, ItemStack carried) {
        if (carried.isEmpty()
                || !WeaponCaseItem.canStore(playerInventory.player, serverCaseStack, carried)) {
            return false;
        }
        WeaponCaseContents contents = WeaponCaseItem.getContents(serverCaseStack);
        if (contents.get(caseSlot).isPresent()) return false;

        setCaseContents(contents.set(caseSlot, carried));
        carried.shrink(1);
        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        return true;
    }

    private int firstEmptyCaseSlot() {
        WeaponCaseContents contents = WeaponCaseItem.getContents(serverCaseStack);
        for (int slot = 0; slot < CASE_SLOT_COUNT; slot++) {
            if (contents.get(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private void setCaseContents(WeaponCaseContents contents) {
        WeaponCaseItem.setContents(serverCaseStack, contents);
        playerInventory.setChanged();
        refreshCaseView(serverCaseStack);
        broadcastChanges();
    }

    private void refreshCaseView(ItemStack caseStack) {
        WeaponCaseContents contents = caseStack != null && caseStack.getItem() instanceof WeaponCaseItem
                ? WeaponCaseItem.getContents(caseStack)
                : WeaponCaseContents.EMPTY;
        for (int slot = 0; slot < CASE_SLOT_COUNT; slot++) {
            ItemStack stored = contents.get(slot)
                    .map(WeaponCaseContents.Entry::stack)
                    .map(stack -> stack.copyWithCount(1))
                    .orElse(ItemStack.EMPTY);
            caseView.setItem(slot, stored);
        }
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                addPlayerSlot(inventory, inventorySlot, 8 + column * 18, 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            addPlayerSlot(inventory, column, 8 + column * 18, 142);
        }
    }

    private void addPlayerSlot(Inventory inventory, int inventorySlot, int x, int y) {
        addSlot(new Slot(inventory, inventorySlot, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return inventorySlot != sourceInventorySlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventorySlot != sourceInventorySlot;
            }
        });
    }
}
