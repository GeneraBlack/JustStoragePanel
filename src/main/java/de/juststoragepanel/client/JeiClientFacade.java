package de.juststoragepanel.client;

import java.lang.reflect.Method;

public final class JeiClientFacade {
    private static final String RUNTIME_BRIDGE_CLASS = "de.juststoragepanel.compat.jei.JustStoragePanelJeiRuntime";

    private JeiClientFacade() {
    }

    public static boolean isAvailable() {
        Object result = invoke("isAvailable", new Class<?>[0]);
        return result instanceof Boolean available && available;
    }

    public static void openCraftingRecipes(String filterText) {
        invoke("openCraftingRecipes", new Class<?>[]{String.class}, filterText == null ? "" : filterText);
    }

    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> bridgeClass = Class.forName(RUNTIME_BRIDGE_CLASS);
            Method method = bridgeClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}