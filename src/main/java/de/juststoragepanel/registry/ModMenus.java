package de.juststoragepanel.registry;

import de.juststoragepanel.JustStoragePanel;
import de.juststoragepanel.menu.AccessPanelMenu;
import de.juststoragepanel.menu.CraftingPanelMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, JustStoragePanel.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AccessPanelMenu>> ACCESS_PANEL_MENU = MENUS.register(
            "access_panel",
            () -> IMenuTypeExtension.create(AccessPanelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingPanelMenu>> CRAFTING_PANEL_MENU = MENUS.register(
            "crafting_panel",
            () -> IMenuTypeExtension.create(CraftingPanelMenu::new));

    private ModMenus() {
    }
}