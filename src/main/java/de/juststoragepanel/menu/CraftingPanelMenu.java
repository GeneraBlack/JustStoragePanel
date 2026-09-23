package de.juststoragepanel.menu;

import de.juststoragepanel.registry.ModBlocks;
import de.juststoragepanel.registry.ModMenus;
import de.juststoragepanel.network.StorageNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.neoforged.neoforge.common.util.ItemStackMap;
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
            this.markDisplayDirty();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            StorageNetwork network = StorageNetwork.discover(this.level, this.panelPos);
            for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
                ItemStack stack = this.craftSlots.getItem(slot);
                if (!stack.isEmpty()) {
                    this.craftSlots.setItem(slot, ItemStack.EMPTY);
                    this.returnStackSafely(serverPlayer, network, stack);
                }
            }
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
            if (!stackInSlot.isEmpty()) {
                this.insertIntoNetwork(stackInSlot);
            }
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
                ItemStack remainder = this.insertIntoNetwork(stackInSlot.copy());
                int moved = stackInSlot.getCount() - remainder.getCount();
                if (moved <= 0) {
                    return ItemStack.EMPTY;
                }
                stackInSlot.shrink(moved);
            }
        } else if (this.isPlayerInventorySlot(slotIndex)) {
            return this.quickMovePlayerStackToNetwork(slotIndex);
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

    public int getCraftStartIndex() {
        return this.craftStartIndex;
    }

    public int getCraftEndIndex() {
        return this.craftEndIndex;
    }

    public void handleRecipeTransfer(ServerPlayer player, List<ItemStack> ingredients, boolean maxTransfer) {
        if (ingredients.size() != 9) {
            return;
        }

        StorageNetwork network = StorageNetwork.discover(this.level, this.panelPos);

        Map<ItemStack, Integer> requiredCounts = ItemStackMap.createTypeAndTagMap();
        int slotLimit = 64;
        boolean hasIngredients = false;

        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            hasIngredients = true;
            slotLimit = Math.min(slotLimit, Math.min(this.craftSlots.getMaxStackSize(), ingredient.getMaxStackSize()));
            ItemStack key = ingredient.copy();
            key.setCount(1);
            requiredCounts.put(key, requiredCounts.getOrDefault(key, 0) + 1);
        }

        if (!hasIngredients) {
            return;
        }

        int availableSets = maxTransfer ? slotLimit : 1;
        for (Map.Entry<ItemStack, Integer> entry : requiredCounts.entrySet()) {
            ItemStack template = entry.getKey();
            int neededPerSet = entry.getValue();

            int inGrid = this.countInCraftSlots(template);
            int inInv = this.countInPlayerInventory(template);
            int inNetwork = network.count(template);
            int total = inGrid + inInv + inNetwork;

            int possibleSets = total / neededPerSet;
            availableSets = Math.min(availableSets, possibleSets);
        }

        if (availableSets <= 0) {
            return;
        }

        int targetSets = availableSets;

        // Step A: Evict any mismatched items or excess items from craft slots
        for (int slot = 0; slot < 9; slot++) {
            ItemStack template = ingredients.get(slot);
            ItemStack current = this.craftSlots.getItem(slot);

            if (current.isEmpty()) {
                continue;
            }

            if (template.isEmpty() || !ItemStack.isSameItemSameComponents(current, template)) {
                this.craftSlots.setItem(slot, ItemStack.EMPTY);
                this.returnStackSafely(player, network, current);
            } else if (current.getCount() > targetSets) {
                ItemStack excess = current.split(current.getCount() - targetSets);
                this.craftSlots.setItem(slot, current);
                this.returnStackSafely(player, network, excess);
            }
        }

        // Step B: Fill each recipe slot up to targetSets
        for (int slot = 0; slot < 9; slot++) {
            ItemStack template = ingredients.get(slot);
            if (template.isEmpty()) {
                continue;
            }

            ItemStack current = this.craftSlots.getItem(slot);
            int currentCount = current.isEmpty() ? 0 : current.getCount();
            int needed = targetSets - currentCount;

            if (needed <= 0) {
                continue;
            }

            int fromInv = this.pullFromPlayerInventory(template, needed);
            needed -= fromInv;

            int fromNet = 0;
            if (needed > 0) {
                ItemStack extracted = network.extract(template, needed);
                fromNet = extracted.getCount();
                needed -= fromNet;
            }

            int added = fromInv + fromNet;
            if (added > 0) {
                if (current.isEmpty()) {
                    ItemStack newStack = template.copy();
                    newStack.setCount(added);
                    this.craftSlots.setItem(slot, newStack);
                } else {
                    current.grow(added);
                    this.craftSlots.setItem(slot, current);
                }
            }
        }

        this.slotsChanged(this.craftSlots);
        this.refreshNow();
    }

    private void updateCraftingResult(ServerLevel level) {
        CraftingInput craftingInput = this.createCraftingInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput, level);
        ItemStack result = ItemStack.EMPTY;

        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = recipe.get();
            ItemStack assembled = recipeHolder.value().assemble(craftingInput);
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

    private int countInCraftSlots(ItemStack template) {
        int total = 0;
        for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
            ItemStack stack = this.craftSlots.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private int countInPlayerInventory(ItemStack template) {
        int total = 0;
        for (int slotIndex = this.getPlayerInventoryStart(); slotIndex < this.getPlayerInventoryEnd(); slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private int pullFromPlayerInventory(ItemStack template, int count) {
        int remaining = count;
        for (int slotIndex = this.getPlayerInventoryStart(); slotIndex < this.getPlayerInventoryEnd(); slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) {
                continue;
            }

            int toTake = Math.min(remaining, stack.getCount());
            stack.shrink(toTake);
            remaining -= toTake;

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (remaining <= 0) {
                break;
            }
        }
        return count - remaining;
    }

    private void returnStackSafely(ServerPlayer player, StorageNetwork network, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = network.insert(stack);
        if (!remaining.isEmpty()) {
            if (!player.getInventory().add(remaining)) {
                player.drop(remaining, false);
            }
        }
    }
}