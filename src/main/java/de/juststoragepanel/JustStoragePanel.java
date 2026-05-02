package de.juststoragepanel;

import com.mojang.logging.LogUtils;
import de.juststoragepanel.config.JustStoragePanelConfig;
import de.juststoragepanel.network.CraftingPanelRecipeTransferPayload;
import de.juststoragepanel.network.PanelSearchPayload;
import de.juststoragepanel.network.StorageNetwork;
import de.juststoragepanel.registry.ModBlocks;
import de.juststoragepanel.registry.ModMenus;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(JustStoragePanel.MODID)
public final class JustStoragePanel {
    public static final String MODID = "juststoragepanel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JustStoragePanel(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, JustStoragePanelConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, JustStoragePanelConfig.CLIENT_SPEC);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(CraftingPanelRecipeTransferPayload::register);
        modEventBus.addListener(PanelSearchPayload::register);

        NeoForge.EVENT_BUS.addListener(StorageNetwork::invalidateOnBlockBreak);
        NeoForge.EVENT_BUS.addListener(StorageNetwork::invalidateOnBlockPlace);
        NeoForge.EVENT_BUS.addListener(StorageNetwork::invalidateOnFluidPlace);
        NeoForge.EVENT_BUS.addListener(StorageNetwork::invalidateOnLevelUnload);
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