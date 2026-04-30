package de.juststoragepanel.client.screen;

import de.juststoragepanel.client.JeiClientFacade;
import de.juststoragepanel.menu.CraftingPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class CraftingPanelScreen extends AbstractPanelScreen<CraftingPanelMenu> {
    private Button jeiButton;

    public CraftingPanelScreen(CraftingPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 286, 252);
    }

    @Override
    protected void init() {
        super.init();
        this.jeiButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.juststoragepanel.jei"), button -> this.openJeiRecipes())
                .bounds(this.leftPos + 180, this.topPos + 6, 46, 20)
                .build());
        this.updateJeiButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateJeiButton();
    }

    @Override
    protected void renderExtraBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY) {
        this.drawPanel(guiGraphics, leftPos + 180, topPos + 16, 98, 96, 0xFF171D24, 0xFF2B333D, 0xFF11161D);
        this.drawSlotGrid(guiGraphics, leftPos + 182, topPos + 36, 3, 3);
        this.drawSlot(guiGraphics, leftPos + 240, topPos + 54);
        guiGraphics.drawString(this.font, Component.translatable("screen.juststoragepanel.crafting"), 190, 22, 0xFFF4F1DE, false);
        guiGraphics.drawString(this.font, Component.literal("=>"), 222, 59, 0xFFE07A5F, false);
    }

    private void openJeiRecipes() {
        JeiClientFacade.openCraftingRecipes(this.getJeiSearchText());
    }

    private String getJeiSearchText() {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return this.hoveredSlot.getItem().getHoverName().getString();
        }

        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty()) {
            return carried.getHoverName().getString();
        }

        ItemStack result = this.menu.getSlot(this.menu.getResultSlotIndex()).getItem();
        if (!result.isEmpty()) {
            return result.getHoverName().getString();
        }

        return "";
    }

    private void updateJeiButton() {
        if (this.jeiButton == null) {
            return;
        }

        boolean available = JeiClientFacade.isAvailable();
        this.jeiButton.active = available;
        this.jeiButton.visible = available;
    }
}