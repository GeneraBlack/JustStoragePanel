package de.juststoragepanel.compat.jei;

import de.juststoragepanel.menu.CraftingPanelMenu;
import de.juststoragepanel.network.CraftingPanelRecipeTransferPayload;
import de.juststoragepanel.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CraftingPanelJeiTransferHandler implements IRecipeTransferHandler<CraftingPanelMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper transferHelper;

    public CraftingPanelJeiTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<? extends CraftingPanelMenu> getContainerClass() {
        return CraftingPanelMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingPanelMenu>> getMenuType() {
        return Optional.of(ModMenus.CRAFTING_PANEL_MENU.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingPanelMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        Map<Integer, Ingredient> slotMap = this.transferHelper.getGuiSlotIndexToIngredientMap(recipe);
        if (slotMap.keySet().stream().anyMatch(idx -> idx < 0 || idx >= 9)) {
            return this.transferHelper.createUserErrorWithTooltip(Component.translatable("jei.juststoragepanel.transfer.too_large"));
        }

        if (!doTransfer) {
            return null;
        }

        List<ItemStack> ingredients = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            Ingredient ingredient = slotMap.get(slot);
            if (ingredient == null || ingredient.isEmpty()) {
                ingredients.add(ItemStack.EMPTY);
            } else {
                ingredients.add(findBestMatchingItem(container, slot, ingredient));
            }
        }

        PacketDistributor.sendToServer(new CraftingPanelRecipeTransferPayload(container.containerId, maxTransfer, ingredients));
        return null;
    }

    private static ItemStack findBestMatchingItem(CraftingPanelMenu container, int slotIndex, Ingredient ingredient) {
        // 1. Check existing item in this crafting grid slot
        int craftSlot = container.getCraftStartIndex() + slotIndex;
        if (craftSlot < container.slots.size()) {
            ItemStack inGrid = container.getSlot(craftSlot).getItem();
            if (!inGrid.isEmpty() && ingredient.test(inGrid)) {
                ItemStack copy = inGrid.copy();
                copy.setCount(1);
                return copy;
            }
        }

        // 2. Check player inventory
        for (int i = container.getPlayerInventoryStart(); i < container.getPlayerInventoryEnd(); i++) {
            ItemStack inInv = container.getSlot(i).getItem();
            if (!inInv.isEmpty() && ingredient.test(inInv)) {
                ItemStack copy = inInv.copy();
                copy.setCount(1);
                return copy;
            }
        }

        // 3. Check network display slots
        for (int i = 0; i < CraftingPanelMenu.DISPLAY_SLOT_COUNT; i++) {
            ItemStack inDisplay = container.getSlot(i).getItem();
            if (!inDisplay.isEmpty() && ingredient.test(inDisplay)) {
                ItemStack copy = inDisplay.copy();
                copy.setCount(1);
                return copy;
            }
        }

        // 4. Fallback to ingredient's first valid item
        ItemStack[] items = ingredient.getItems();
        if (items.length > 0) {
            ItemStack copy = items[0].copy();
            copy.setCount(1);
            return copy;
        }

        return ItemStack.EMPTY;
    }
}