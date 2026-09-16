package de.juststoragepanel.block;

import de.juststoragepanel.menu.CraftingPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class CraftingPanelBlock extends AbstractPanelBlock {
    public CraftingPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("block.juststoragepanel.crafting_panel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory, BlockPos pos) {
        return new CraftingPanelMenu(containerId, inventory, pos);
    }
}