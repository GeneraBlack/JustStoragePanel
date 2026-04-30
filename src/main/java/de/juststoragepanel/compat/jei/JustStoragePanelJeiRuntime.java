package de.juststoragepanel.compat.jei;

import java.util.List;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;

public final class JustStoragePanelJeiRuntime {
    private static volatile IJeiRuntime runtime;

    private JustStoragePanelJeiRuntime() {
    }

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void clearRuntime() {
        runtime = null;
    }

    public static boolean isAvailable() {
        return runtime != null;
    }

    public static void openCraftingRecipes(String filterText) {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null) {
            return;
        }

        IIngredientFilter ingredientFilter = jeiRuntime.getIngredientFilter();
        ingredientFilter.setFilterText(filterText == null ? "" : filterText);
        jeiRuntime.getRecipesGui().showTypes(List.of(RecipeTypes.CRAFTING));
    }
}