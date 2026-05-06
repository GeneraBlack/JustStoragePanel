package de.juststoragepanel.registry;

import de.juststoragepanel.JustStoragePanel;
import de.juststoragepanel.block.AccessPanelBlock;
import de.juststoragepanel.block.CraftingPanelBlock;
import de.juststoragepanel.block.LogicCableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JustStoragePanel.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JustStoragePanel.MODID);

    public static final DeferredBlock<AccessPanelBlock> ACCESS_PANEL = BLOCKS.registerBlock("access_panel",
            AccessPanelBlock::new,
            () -> panelProperties(MapColor.METAL));
    public static final DeferredBlock<CraftingPanelBlock> CRAFTING_PANEL = BLOCKS.registerBlock("crafting_panel",
            CraftingPanelBlock::new,
            () -> panelProperties(MapColor.METAL));
    public static final DeferredBlock<LogicCableBlock> LOGIC_CABLE = BLOCKS.registerBlock("logic_cable",
            LogicCableBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> ACCESS_PANEL_ITEM = ITEMS.registerSimpleBlockItem("access_panel", ACCESS_PANEL);
    public static final DeferredItem<BlockItem> CRAFTING_PANEL_ITEM = ITEMS.registerSimpleBlockItem("crafting_panel", CRAFTING_PANEL);
    public static final DeferredItem<BlockItem> LOGIC_CABLE_ITEM = ITEMS.registerSimpleBlockItem("logic_cable", LOGIC_CABLE, Item.Properties::new);

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties panelProperties(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .strength(2.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}