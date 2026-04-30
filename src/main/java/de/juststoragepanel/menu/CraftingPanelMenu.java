package de.juststoragepanel.menu;

import de.juststoragepanel.registry.ModBlocks;
import de.juststoragepanel.registry.ModMenus;
import de.juststoragepanel.network.StorageNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public final class CraftingPanelMenu extends AbstractPanelMenu {
    private static final int CRAFT_RESULT_X = 240;
    private static final int CRAFT_RESULT_Y = 54;
    private static final int CRAFT_GRID_X = 182;
    private static final int CRAFT_GRID_Y = 36;

    private final Player player;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final int resultSlotIndex;
    private final int craftStartIndex;
    private final int craftEndIndex;

    public CraftingPanelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    public CraftingPanelMenu(int containerId, Inventory playerInventory, BlockPos panelPos) {
        super(ModMenus.CRAFTING_PANEL_MENU.get(), containerId, playerInventory, panelPos);
        this.player = playerInventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        this.resultSlotIndex = this.slots.size();
        this.addSlot(new ResultSlot(this.player, this.craftSlots, this.resultSlots, 0, CRAFT_RESULT_X, CRAFT_RESULT_Y));

        this.craftStartIndex = this.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = column + row * 3;
                this.addSlot(new Slot(this.craftSlots, slotIndex, CRAFT_GRID_X + column * 18, CRAFT_GRID_Y + row * 18));
            }
        }
        this.craftEndIndex = this.slots.size();

        this.addPlayerInventorySlots(playerInventory);
        if (this.player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
            this.updateCraftingResult(serverLevel);
        }
    }

    @Override
    protected Block getValidBlock() {
        return ModBlocks.CRAFTING_PANEL.get();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (container == this.craftSlots && this.player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
            this.updateCraftingResult(serverLevel);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (slotIndex == this.resultSlotIndex) {
            if (!this.moveItemStackTo(stackInSlot, this.getPlayerInventoryStart(), this.getPlayerInventoryEnd(), true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stackInSlot, original);
            slot.onTake(player, stackInSlot);
            return original;
        }

        if (this.isDisplaySlot(slotIndex)) {
            ItemStack extracted = this.extractFromNetwork(this.slots.get(slotIndex).getItem(), this.slots.get(slotIndex).getItem().getMaxStackSize());
            if (extracted.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack pulled = extracted.copy();
            this.moveItemStackTo(extracted, this.craftStartIndex, this.craftEndIndex, false);
            if (!extracted.isEmpty()) {
                this.moveItemStackTo(extracted, this.getPlayerInventoryStart(), this.getPlayerInventoryEnd(), true);
            }
            if (!extracted.isEmpty()) {
                this.insertIntoNetwork(extracted);
            }
            return pulled;
        }

        if (slotIndex >= this.craftStartIndex && slotIndex < this.craftEndIndex) {
            if (!this.moveItemStackTo(stackInSlot, this.getPlayerInventoryStart(), this.getPlayerInventoryEnd(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.isPlayerInventorySlot(slotIndex)) {
            if (!this.moveItemStackTo(stackInSlot, this.craftStartIndex, this.craftEndIndex, false)) {
                ItemStack remainder = this.insertIntoNetwork(stackInSlot.copy());
                int moved = stackInSlot.getCount() - remainder.getCount();
                if (moved <= 0) {
                    return ItemStack.EMPTY;
                }
                stackInSlot.shrink(moved);
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return original;
    }

    public int getResultSlotIndex() {
        return this.resultSlotIndex;
    }

    public void handleRecipeTransfer(ServerPlayer player, List<ItemStack> ingredients, boolean maxTransfer) {
        if (ingredients.size() != 9) {
            return;
        }

        StorageNetwork network = StorageNetwork.discover(this.level, this.panelPos);
        List<ItemStack> craftGridBackup = new ArrayList<>(9);
        List<ItemStack> bufferedItems = new ArrayList<>();

        for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
            ItemStack existing = this.craftSlots.getItem(slot).copy();
            craftGridBackup.add(existing);
            if (!existing.isEmpty()) {
                bufferedItems.add(existing.copy());
                this.craftSlots.setItem(slot, ItemStack.EMPTY);
            }
        }

        int transferredSets = 0;
        while (true) {
            List<ItemStack> takenSet = new ArrayList<>(9);
            boolean completeSet = true;

            for (int slot = 0; slot < ingredients.size(); slot++) {
                ItemStack ingredient = ingredients.get(slot);
                if (ingredient.isEmpty()) {
                    takenSet.add(ItemStack.EMPTY);
                    continue;
                }

                if (!this.canIncreaseCraftSlot(slot, ingredient)) {
                    completeSet = false;
                    break;
                }

                ItemStack taken = this.takeSingleIngredient(ingredient, bufferedItems, network);
                if (taken.isEmpty()) {
                    completeSet = false;
                    break;
                }

                takenSet.add(taken);
            }

            if (!completeSet) {
                for (ItemStack taken : takenSet) {
                    if (!taken.isEmpty()) {
                        bufferedItems.add(taken);
                    }
                }
                break;
            }

            this.applyTakenSet(takenSet);
            transferredSets++;
            if (!maxTransfer) {
                break;
            }
        }

        if (transferredSets == 0) {
            for (int slot = 0; slot < craftGridBackup.size(); slot++) {
                this.craftSlots.setItem(slot, craftGridBackup.get(slot).copy());
            }
        } else {
            this.returnBufferedItems(player, bufferedItems, network);
        }

        this.slotsChanged(this.craftSlots);
        this.refreshNow();
    }

    private void updateCraftingResult(ServerLevel level) {
        CraftingInput craftingInput = this.createCraftingInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput, level);
        ItemStack result = ItemStack.EMPTY;

        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = recipe.get();
            ItemStack assembled = recipeHolder.value().assemble(craftingInput, level.registryAccess());
            if (!assembled.isEmpty() && assembled.isItemEnabled(level.enabledFeatures())) {
                result = assembled;
            }
        }

        this.resultSlots.setItem(0, result);
        this.setRemoteSlot(this.resultSlotIndex, result);
        if (this.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), this.resultSlotIndex, result));
        }
    }

    private CraftingInput createCraftingInput() {
        NonNullList<ItemStack> inputs = NonNullList.withSize(this.craftSlots.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
            inputs.set(slot, this.craftSlots.getItem(slot));
        }
        return CraftingInput.of(3, 3, inputs);
    }

    private boolean canIncreaseCraftSlot(int slotIndex, ItemStack ingredient) {
        ItemStack current = this.craftSlots.getItem(slotIndex);
        int capacity = Math.min(this.craftSlots.getMaxStackSize(), ingredient.getMaxStackSize());
        if (current.isEmpty()) {
            return capacity > 0;
        }

        return ItemStack.isSameItemSameComponents(current, ingredient) && current.getCount() < capacity;
    }

    private ItemStack takeSingleIngredient(ItemStack ingredient, List<ItemStack> bufferedItems, StorageNetwork network) {
        ItemStack fromBuffer = this.takeFromBufferedItems(ingredient, bufferedItems);
        if (!fromBuffer.isEmpty()) {
            return fromBuffer;
        }

        ItemStack fromInventory = this.takeFromPlayerInventory(ingredient);
        if (!fromInventory.isEmpty()) {
            return fromInventory;
        }

        ItemStack fromNetwork = network.extract(ingredient, 1);
        if (!fromNetwork.isEmpty()) {
            fromNetwork.setCount(1);
            return fromNetwork;
        }

        return ItemStack.EMPTY;
    }

    private ItemStack takeFromBufferedItems(ItemStack ingredient, List<ItemStack> bufferedItems) {
        for (int index = 0; index < bufferedItems.size(); index++) {
            ItemStack stack = bufferedItems.get(index);
            if (!ItemStack.isSameItemSameComponents(stack, ingredient)) {
                continue;
            }

            ItemStack taken = stack.split(1);
            if (stack.isEmpty()) {
                bufferedItems.remove(index);
            }
            return taken;
        }

        return ItemStack.EMPTY;
    }

    private ItemStack takeFromPlayerInventory(ItemStack ingredient) {
        for (int slotIndex = this.getPlayerInventoryStart(); slotIndex < this.getPlayerInventoryEnd(); slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, ingredient)) {
                continue;
            }

            ItemStack taken = stack.split(1);
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return taken;
        }

        return ItemStack.EMPTY;
    }

    private void applyTakenSet(List<ItemStack> takenSet) {
        for (int slot = 0; slot < takenSet.size(); slot++) {
            ItemStack taken = takenSet.get(slot);
            if (taken.isEmpty()) {
                continue;
            }

            ItemStack current = this.craftSlots.getItem(slot);
            if (current.isEmpty()) {
                this.craftSlots.setItem(slot, taken.copy());
            } else {
                current.grow(1);
                this.craftSlots.setItem(slot, current);
            }
        }
    }

    private void returnBufferedItems(ServerPlayer player, List<ItemStack> bufferedItems, StorageNetwork network) {
        for (ItemStack bufferedItem : bufferedItems) {
            if (bufferedItem.isEmpty()) {
                continue;
            }

            ItemStack remaining = bufferedItem.copy();
            this.moveItemStackTo(remaining, this.getPlayerInventoryStart(), this.getPlayerInventoryEnd(), false);
            if (!remaining.isEmpty()) {
                remaining = network.insert(remaining);
            }
            if (!remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
    }
}