package icu.icuqalt10.panlingre.world.inventory;

import icu.icuqalt10.panlingre.component.FuZhiBagContents;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.item.fuzhi.FuZhiBagItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FuZhiBagMenu extends AbstractContainerMenu {
    public static final int BAG_SLOT_COUNT = FuZhiBagContents.SLOT_COUNT;
    public static final int BAG_SLOT_X = 26;
    public static final int BAG_SLOT_Y = 28;
    public static final int BAG_SLOT_SPACING = 27;
    private static final int PLAYER_SLOT_START = BAG_SLOT_COUNT;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;
    private static final int COUNT_PARTS = 4;

    private final Inventory playerInventory;
    private final SimpleContainer bagView = new SimpleContainer(BAG_SLOT_COUNT);
    private final ItemStack serverBagStack;
    private final int sourceInventorySlot;
    private final int[] syncedCountParts = new int[BAG_SLOT_COUNT * COUNT_PARTS];

    public FuZhiBagMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, null, buffer.readVarInt());
    }

    public FuZhiBagMenu(int containerId, Inventory inventory, ItemStack bagStack, int sourceInventorySlot) {
        super(ModMenus.fu_zhi_bag_menu.get(), containerId);
        this.playerInventory = inventory;
        this.serverBagStack = bagStack;
        this.sourceInventorySlot = sourceInventorySlot;

        ItemStack initialBag = bagStack != null ? bagStack : inventory.getItem(sourceInventorySlot);
        refreshBagView(initialBag);

        for (int i = 0; i < BAG_SLOT_COUNT; i++) {
            this.addSlot(new Slot(bagView, i,
                    BAG_SLOT_X + i * BAG_SLOT_SPACING, BAG_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FuZhiBagItem.isFuZhi(stack);
                }
            });
        }

        addPlayerInventorySlots(inventory);
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                int bagSlot = index / COUNT_PARTS;
                long count = serverBagStack == null
                        ? getSyncedCount(bagSlot)
                        : FuZhiBagItem.getContents(serverBagStack).count(bagSlot);
                int shift = index % COUNT_PARTS * 16;
                return (int) (count >>> shift) & 0xFFFF;
            }

            @Override
            public void set(int index, int value) {
                syncedCountParts[index] = value;
            }

            @Override
            public int getCount() {
                return syncedCountParts.length;
            }
        });
    }

    public long getStoredCount(int bagSlot) {
        if (bagSlot < 0 || bagSlot >= BAG_SLOT_COUNT) return 0L;
        return serverBagStack == null
                ? getSyncedCount(bagSlot)
                : FuZhiBagItem.getContents(serverBagStack).count(bagSlot);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < BAG_SLOT_COUNT) {
            if (serverBagStack == null) return;

            if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
                ItemStack carried = getCarried();
                if (carried.isEmpty()) {
                    extractToCursor(slotId, button == 0 ? 1 : 32);
                } else {
                    depositIntoSlot(slotId, carried, button == 0 ? carried.getCount() : 1);
                    setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                }
            } else if (clickType == ClickType.QUICK_MOVE && button == 0) {
                extractToInventory(slotId, 64);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (serverBagStack == null || index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;

        if (index < BAG_SLOT_COUNT) {
            return extractToInventory(index, 64);
        }

        Slot source = this.slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack sourceStack = source.getItem();
        if (!FuZhiBagItem.isFuZhi(sourceStack)) return ItemStack.EMPTY;

        ItemStack original = sourceStack.copy();
        int moved = depositAnywhere(sourceStack, sourceStack.getCount());
        if (moved <= 0) return ItemStack.EMPTY;

        if (sourceStack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return original;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= BAG_SLOT_COUNT && super.canDragTo(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        if (serverBagStack == null) return true;
        return player.isAlive()
                && sourceInventorySlot >= 0
                && sourceInventorySlot < playerInventory.getContainerSize()
                && playerInventory.getItem(sourceInventorySlot) == serverBagStack
                && serverBagStack.getItem() instanceof FuZhiBagItem;
    }

    private void extractToCursor(int bagSlot, int requested) {
        FuZhiBagContents contents = FuZhiBagItem.getContents(serverBagStack);
        FuZhiBagContents.Entry entry = contents.get(bagSlot).orElse(null);
        if (entry == null) return;

        Item item = BuiltInRegistries.ITEM.get(entry.itemId());
        if (item == null) return;
        int amount = (int) Math.min(entry.count(), requested);
        if (amount <= 0) return;

        setCarried(new ItemStack(item, amount));
        setBagContents(contents.remove(bagSlot, amount));
    }

    private ItemStack extractToInventory(int bagSlot, int requested) {
        FuZhiBagContents contents = FuZhiBagItem.getContents(serverBagStack);
        FuZhiBagContents.Entry entry = contents.get(bagSlot).orElse(null);
        if (entry == null) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(entry.itemId());
        if (item == null) return ItemStack.EMPTY;
        int requestedAmount = (int) Math.min(entry.count(), requested);
        if (requestedAmount <= 0) return ItemStack.EMPTY;

        ItemStack output = new ItemStack(item, requestedAmount);
        ItemStack result = output.copy();
        moveItemStackTo(output, PLAYER_SLOT_START, PLAYER_SLOT_END, true);
        int moved = requestedAmount - output.getCount();
        if (moved <= 0) return ItemStack.EMPTY;

        result.setCount(moved);
        setBagContents(contents.remove(bagSlot, moved));
        return result;
    }

    private int depositAnywhere(ItemStack carried, int requested) {
        if (!FuZhiBagItem.isFuZhi(carried)) return 0;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(carried.getItem());
        FuZhiBagContents contents = FuZhiBagItem.getContents(serverBagStack);
        int target = contents.find(itemId).map(FuZhiBagContents.Entry::slot).orElseGet(() -> {
            for (int i = 0; i < BAG_SLOT_COUNT; i++) {
                if (contents.get(i).isEmpty()) return i;
            }
            return -1;
        });
        return target < 0 ? 0 : depositIntoSlot(target, carried, requested);
    }

    private int depositIntoSlot(int bagSlot, ItemStack carried, int requested) {
        if (!FuZhiBagItem.isFuZhi(carried) || requested <= 0) return 0;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(carried.getItem());
        FuZhiBagContents contents = FuZhiBagItem.getContents(serverBagStack);

        FuZhiBagContents.Entry sameItem = contents.find(itemId).orElse(null);
        int targetSlot = sameItem == null ? bagSlot : sameItem.slot();
        FuZhiBagContents.Entry occupant = contents.get(targetSlot).orElse(null);
        if (occupant != null && !occupant.itemId().equals(itemId)) return 0;

        long oldCount = contents.count(targetSlot);
        int offered = Math.min(requested, carried.getCount());
        FuZhiBagContents changed = contents.add(targetSlot, itemId, offered);
        long acceptedLong = changed.count(targetSlot) - oldCount;
        int accepted = (int) Math.min(offered, Math.max(0L, acceptedLong));
        if (accepted <= 0) return 0;

        carried.shrink(accepted);
        setBagContents(changed);
        return accepted;
    }

    private void setBagContents(FuZhiBagContents contents) {
        FuZhiBagItem.setContents(serverBagStack, contents);
        playerInventory.setChanged();
        refreshBagView(serverBagStack);
        broadcastChanges();
    }

    private void refreshBagView(ItemStack bagStack) {
        FuZhiBagContents contents = bagStack != null && bagStack.getItem() instanceof FuZhiBagItem
                ? FuZhiBagItem.getContents(bagStack)
                : FuZhiBagContents.EMPTY;
        for (int i = 0; i < BAG_SLOT_COUNT; i++) {
            FuZhiBagContents.Entry entry = contents.get(i).orElse(null);
            Item item = entry == null ? null : BuiltInRegistries.ITEM.get(entry.itemId());
            bagView.setItem(i, item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
    }

    private long getSyncedCount(int bagSlot) {
        long count = 0L;
        for (int part = 0; part < COUNT_PARTS; part++) {
            count |= ((long) syncedCountParts[bagSlot * COUNT_PARTS + part] & 0xFFFFL) << (part * 16);
        }
        return count;
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
        this.addSlot(new Slot(inventory, inventorySlot, x, y) {
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
