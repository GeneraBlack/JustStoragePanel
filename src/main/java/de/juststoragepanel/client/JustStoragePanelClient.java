package de.juststoragepanel.client;

import de.juststoragepanel.JustStoragePanel;
import de.juststoragepanel.client.screen.AccessPanelScreen;
import de.juststoragepanel.client.screen.CraftingPanelScreen;
import de.juststoragepanel.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = JustStoragePanel.MODID, dist = Dist.CLIENT)
public final class JustStoragePanelClient {
    public JustStoragePanelClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerScreens);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ACCESS_PANEL_MENU.get(), AccessPanelScreen::new);
        event.register(ModMenus.CRAFTING_PANEL_MENU.get(), CraftingPanelScreen::new);
    }
}