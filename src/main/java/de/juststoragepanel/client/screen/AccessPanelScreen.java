package de.juststoragepanel.client.screen;

import de.juststoragepanel.menu.AccessPanelMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AccessPanelScreen extends AbstractPanelScreen<AccessPanelMenu> {
    public AccessPanelScreen(AccessPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 188, 252);
    }

    @Override
    protected void extractExtraBackground(GuiGraphicsExtractor guiGraphics, int leftPos, int topPos, int mouseX, int mouseY) {
    }
}