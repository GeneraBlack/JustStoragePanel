package de.juststoragepanel.compat.jei;

import de.juststoragepanel.menu.CraftingPanelMenu;
import de.juststoragepanel.network.CraftingPanelRecipeTransferPayload;
import de.juststoragepanel.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
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
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingPanelMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        if (inputSlots.size() > 9) {
            List<IRecipeSlotView> overflow = inputSlots.subList(9, inputSlots.size()).stream()
                    .filter(slot -> !slot.isEmpty())
                    .toList();
            if (!overflow.isEmpty()) {
                return this.transferHelper.createUserErrorWithTooltip(Component.translatable("jei.juststoragepanel.transfer.too_large"));
            }
        }

        if (!doTransfer) {
            return null;
        }

        List<ItemStack> ingredients = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack ingredient = slot < inputSlots.size()
                    ? inputSlots.get(slot).getDisplayedItemStack().map(ItemStack::copy).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            ingredients.add(ingredient);
        }

        PacketDistributor.sendToServer(new CraftingPanelRecipeTransferPayload(container.containerId, maxTransfer, ingredients));
        return null;
    }
}