package de.juststoragepanel.block;

import com.mojang.serialization.MapCodec;
import de.juststoragepanel.menu.AccessPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public final class AccessPanelBlock extends AbstractPanelBlock {
    public static final MapCodec<AccessPanelBlock> CODEC = simpleCodec(AccessPanelBlock::new);

    public AccessPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("block.juststoragepanel.access_panel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory, BlockPos pos) {
        return new AccessPanelMenu(containerId, inventory, pos);
    }
}