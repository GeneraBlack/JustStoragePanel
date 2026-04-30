package de.juststoragepanel.client.screen;

import de.juststoragepanel.menu.AccessPanelMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;

public final class AccessPanelScreen extends AbstractPanelScreen<AccessPanelMenu> {
    public AccessPanelScreen(AccessPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 188, 252);
    }

    @Override
    protected void renderExtraBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY) {
    }
}