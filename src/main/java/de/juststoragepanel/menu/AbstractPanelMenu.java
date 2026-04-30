package de.juststoragepanel.menu;

import de.juststoragepanel.network.StorageNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public abstract class AbstractPanelMenu extends AbstractContainerMenu {
    public static final int SEARCH_MAX_LENGTH = 40;
    public static final int DISPLAY_COLUMNS = 9;
    public static final int DISPLAY_ROWS = 6;
    public static final int DISPLAY_SLOT_COUNT = DISPLAY_COLUMNS * DISPLAY_ROWS;
    public static final int DISPLAY_X = 10;
    public static final int DISPLAY_Y = 42;
    public static final int PLAYER_INVENTORY_X = 10;
    public static final int PLAYER_INVENTORY_Y = 160;

    private static final int PREV_PAGE_BUTTON = 0;
    private static final int NEXT_PAGE_BUTTON = 1;

    protected final BlockPos panelPos;
    protected final Level level;
    protected final ContainerLevelAccess access;
    protected final SimpleContainer displayContainer = new SimpleContainer(DISPLAY_SLOT_COUNT);

    private final int[] displayCounts = new int[DISPLAY_SLOT_COUNT];
    private int playerInventoryStart = -1;
    private int hotbarStart = -1;
    private int page = 0;
    private int maxPage = 1;
    private long lastRefreshGameTime = Long.MIN_VALUE;
    private String searchQuery = "";

    protected AbstractPanelMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, BlockPos panelPos) {
        super(menuType, containerId);
        this.level = playerInventory.player.level();
        this.panelPos = panelPos.immutable();
        this.access = ContainerLevelAccess.create(this.level, this.panelPos);

        this.addDisplaySlots();
        this.registerDataSlots();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.getValidBlock());
    }

    @Override
    public void broadcastChanges() {
        if (!this.level.isClientSide && this.level.getGameTime() != this.lastRefreshGameTime) {
            this.refreshDisplay();
            this.lastRefreshGameTime = this.level.getGameTime();
        }

        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == PREV_PAGE_BUTTON && this.page > 0) {
            this.page--;
            this.refreshNow();
            return true;
        }

        if (id == NEXT_PAGE_BUTTON && this.page + 1 < this.maxPage) {
            this.page++;
            this.refreshNow();
            return true;
        }

        return false;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof NetworkDisplaySlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.PICKUP && this.isDisplaySlot(slotId)) {
            this.handleDisplaySlotClick(slotId, button);
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    public int getDisplayCount(int slotIndex) {
        return slotIndex >= 0 && slotIndex < this.displayCounts.length ? this.displayCounts[slotIndex] : 0;
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public int getCurrentPage() {
        return this.page + 1;
    }

    public int getMaxPage() {
        return this.maxPage;
    }

    public boolean hasPreviousPage() {
        return this.page > 0;
    }

    public boolean hasNextPage() {
        return this.page + 1 < this.maxPage;
    }

    protected abstract Block getValidBlock();

    protected final void addPlayerInventorySlots(Inventory playerInventory) {
        this.playerInventoryStart = this.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        this.hotbarStart = this.slots.size();
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + 58));
        }
    }

    protected final boolean isDisplaySlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < DISPLAY_SLOT_COUNT;
    }

    protected final boolean isPlayerInventorySlot(int slotIndex) {
        return this.playerInventoryStart >= 0 && slotIndex >= this.playerInventoryStart && slotIndex < this.slots.size();
    }

    protected final int getPlayerInventoryStart() {
        return this.playerInventoryStart;
    }

    protected final int getHotbarStart() {
        return this.hotbarStart;
    }

    protected final int getPlayerInventoryEnd() {
        return this.slots.size();
    }

    protected final ItemStack extractVisibleStackToInventory(int displaySlotIndex) {
        ItemStack template = this.displayContainer.getItem(displaySlotIndex);
        if (template.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = this.extractFromNetwork(template, template.getMaxStackSize());
        if (extracted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = extracted.copy();
        if (!this.moveItemStackTo(extracted, this.playerInventoryStart, this.slots.size(), true)) {
            this.insertIntoNetwork(original);
            return ItemStack.EMPTY;
        }

        if (!extracted.isEmpty()) {
            this.insertIntoNetwork(extracted);
        }

        return original;
    }

    protected final ItemStack quickMovePlayerStackToNetwork(int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();
        ItemStack remainder = this.insertIntoNetwork(stackInSlot.copy());
        int moved = stackInSlot.getCount() - remainder.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }

        stackInSlot.shrink(moved);
        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    protected final ItemStack extractFromNetwork(ItemStack template, int amount) {
        ItemStack extracted = StorageNetwork.discover(this.level, this.panelPos).extract(template, amount);
        this.refreshNow();
        return extracted;
    }

    protected final ItemStack insertIntoNetwork(ItemStack stack) {
        ItemStack remainder = StorageNetwork.discover(this.level, this.panelPos).insert(stack);
        this.refreshNow();
        return remainder;
    }

    protected final void refreshNow() {
        if (!this.level.isClientSide) {
            this.refreshDisplay();
            this.lastRefreshGameTime = this.level.getGameTime();
            this.broadcastChanges();
        }
    }

    public void setSearchQuery(String searchQuery) {
        String normalizedQuery = normalizeSearchQuery(searchQuery);
        if (normalizedQuery.equals(this.searchQuery)) {
            return;
        }

        this.searchQuery = normalizedQuery;
        this.page = 0;
        this.refreshNow();
    }

    private void addDisplaySlots() {
        for (int row = 0; row < DISPLAY_ROWS; row++) {
            for (int column = 0; column < DISPLAY_COLUMNS; column++) {
                int slotIndex = column + row * DISPLAY_COLUMNS;
                this.addSlot(new NetworkDisplaySlot(this.displayContainer, slotIndex, DISPLAY_X + column * 18, DISPLAY_Y + row * 18));
            }
        }
    }

    private void registerDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return AbstractPanelMenu.this.page;
            }

            @Override
            public void set(int value) {
                AbstractPanelMenu.this.page = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return AbstractPanelMenu.this.maxPage;
            }

            @Override
            public void set(int value) {
                AbstractPanelMenu.this.maxPage = value;
            }
        });

        for (int index = 0; index < this.displayCounts.length; index++) {
            final int slotIndex = index;
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return AbstractPanelMenu.this.displayCounts[slotIndex];
                }

                @Override
                public void set(int value) {
                    AbstractPanelMenu.this.displayCounts[slotIndex] = value;
                }
            });
        }
    }

    private void refreshDisplay() {
        List<StorageNetwork.NetworkItem> items = StorageNetwork.discover(this.level, this.panelPos).listItems(this.searchQuery);
        this.maxPage = Math.max(1, Mth.ceil((float) items.size() / DISPLAY_SLOT_COUNT));
        this.page = Mth.clamp(this.page, 0, this.maxPage - 1);

        int startIndex = this.page * DISPLAY_SLOT_COUNT;
        for (int slotIndex = 0; slotIndex < DISPLAY_SLOT_COUNT; slotIndex++) {
            int itemIndex = startIndex + slotIndex;
            if (itemIndex < items.size()) {
                StorageNetwork.NetworkItem item = items.get(itemIndex);
                ItemStack displayStack = item.displayStack().copy();
                displayStack.setCount(1);
                this.displayContainer.setItem(slotIndex, displayStack);
                this.displayCounts[slotIndex] = item.count();
            } else {
                this.displayContainer.setItem(slotIndex, ItemStack.EMPTY);
                this.displayCounts[slotIndex] = 0;
            }
        }
    }

    private static String normalizeSearchQuery(String searchQuery) {
        if (searchQuery == null) {
            return "";
        }

        String trimmed = searchQuery.trim();
        if (trimmed.length() > SEARCH_MAX_LENGTH) {
            return trimmed.substring(0, SEARCH_MAX_LENGTH);
        }

        return trimmed;
    }

    private void handleDisplaySlotClick(int slotId, int button) {
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty()) {
            ItemStack toInsert = carried.copy();
            if (button == 1) {
                toInsert.setCount(1);
            }

            ItemStack remainder = this.insertIntoNetwork(toInsert);
            int inserted = toInsert.getCount() - remainder.getCount();
            if (inserted > 0) {
                carried.shrink(inserted);
                this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            }
            return;
        }

        ItemStack template = this.displayContainer.getItem(slotId);
        if (template.isEmpty()) {
            return;
        }

        int amount = button == 1 ? Math.max(1, template.getMaxStackSize() / 2) : template.getMaxStackSize();
        ItemStack extracted = this.extractFromNetwork(template, amount);
        if (!extracted.isEmpty()) {
            this.setCarried(extracted);
        }
    }
}