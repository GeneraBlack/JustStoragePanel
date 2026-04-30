package de.juststoragepanel;

import com.mojang.logging.LogUtils;
import de.juststoragepanel.network.CraftingPanelRecipeTransferPayload;
import de.juststoragepanel.network.PanelSearchPayload;
import de.juststoragepanel.registry.ModBlocks;
import de.juststoragepanel.registry.ModMenus;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(JustStoragePanel.MODID)
public final class JustStoragePanel {
    public static final String MODID = "juststoragepanel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JustStoragePanel(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(CraftingPanelRecipeTransferPayload::register);
        modEventBus.addListener(PanelSearchPayload::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ACCESS_PANEL_ITEM);
            event.accept(ModBlocks.CRAFTING_PANEL_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModBlocks.LOGIC_CABLE_ITEM);
        }
    }
}