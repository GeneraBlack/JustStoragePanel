package de.juststoragepanel.menu;

import de.juststoragepanel.registry.ModBlocks;
import de.juststoragepanel.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class AccessPanelMenu extends AbstractPanelMenu {
    public AccessPanelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    public AccessPanelMenu(int containerId, Inventory playerInventory, BlockPos panelPos) {
        super(ModMenus.ACCESS_PANEL_MENU.get(), containerId, playerInventory, panelPos);
        this.addPlayerInventorySlots(playerInventory);
    }

    @Override
    protected Block getValidBlock() {
        return ModBlocks.ACCESS_PANEL.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (this.isDisplaySlot(slotIndex)) {
            return this.extractVisibleStackToInventory(slotIndex);
        }

        if (this.isPlayerInventorySlot(slotIndex)) {
            return this.quickMovePlayerStackToNetwork(slotIndex);
        }

        return ItemStack.EMPTY;
    }
}