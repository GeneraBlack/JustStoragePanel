package de.juststoragepanel.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class JustStoragePanelConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ModConfigSpec> serverSpecPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = serverSpecPair.getLeft();
        SERVER_SPEC = serverSpecPair.getRight();

        Pair<Client, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientSpecPair.getLeft();
        CLIENT_SPEC = clientSpecPair.getRight();
    }

    private JustStoragePanelConfig() {
    }

    public static int getPassiveRefreshIntervalTicks() {
        return SERVER.passiveRefreshIntervalTicks.get();
    }

    public static int getSearchDebounceTicks() {
        return CLIENT.searchDebounceTicks.get();
    }

    public static final class Server {
        private final ModConfigSpec.IntValue passiveRefreshIntervalTicks;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("panel_updates");

            this.passiveRefreshIntervalTicks = builder
                    .comment("How many server ticks panel menus wait between passive display refreshes while open.", "Set to 1 to refresh every tick.")
                    .defineInRange("passiveRefreshIntervalTicks", 4, 1, 200);

            builder.pop();
        }
    }

    public static final class Client {
        private final ModConfigSpec.IntValue searchDebounceTicks;

        private Client(ModConfigSpec.Builder builder) {
            builder.push("search");

            this.searchDebounceTicks = builder
                    .comment("How many client ticks search input waits before it is sent to the server.", "Set to 0 to send every change immediately.")
                    .defineInRange("searchDebounceTicks", 4, 0, 40);

            builder.pop();
        }
    }
}