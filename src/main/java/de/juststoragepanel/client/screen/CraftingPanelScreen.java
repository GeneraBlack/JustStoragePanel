package de.juststoragepanel.client.screen;

import de.juststoragepanel.menu.CraftingPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CraftingPanelScreen extends AbstractPanelScreen<CraftingPanelMenu> {
    public CraftingPanelScreen(CraftingPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 286, 252);
    }

    @Override
    protected void renderExtraBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY) {
        this.drawPanel(guiGraphics, leftPos + 180, topPos + 16, 100, 96, 0xFF171D24, 0xFF2B333D, 0xFF11161D);
        this.drawSlotGrid(guiGraphics, leftPos + 182, topPos + 36, 3, 3);
        this.drawSlot(guiGraphics, leftPos + 258, topPos + 54);
        guiGraphics.drawString(this.font, Component.translatable("screen.juststoragepanel.crafting"), leftPos + 190, topPos + 22, 0xFFF4F1DE, false);
        guiGraphics.drawString(this.font, Component.literal(">"), leftPos + 241, topPos + 59, 0xFFE07A5F, false);
    }
}