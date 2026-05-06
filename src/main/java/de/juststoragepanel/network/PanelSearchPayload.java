package de.juststoragepanel.network;

import de.juststoragepanel.JustStoragePanel;
import de.juststoragepanel.menu.AbstractPanelMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PanelSearchPayload(int containerId, String query) implements CustomPacketPayload {
    public static final Type<PanelSearchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JustStoragePanel.MODID, "panel_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSearchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PanelSearchPayload::containerId,
            ByteBufCodecs.stringUtf8(AbstractPanelMenu.SEARCH_MAX_LENGTH),
            PanelSearchPayload::query,
            PanelSearchPayload::new);

    public PanelSearchPayload {
        query = sanitize(query);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, PanelSearchPayload::handle);
    }

    @Override
    public Type<PanelSearchPayload> type() {
        return TYPE;
    }

    private static void handle(PanelSearchPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.containerMenu instanceof AbstractPanelMenu menu)) {
            return;
        }

        if (player.containerMenu.containerId != payload.containerId()) {
            return;
        }

        menu.setSearchQuery(payload.query());
    }

    private static String sanitize(String query) {
        if (query == null) {
            return "";
        }

        String trimmed = query.trim();
        if (trimmed.length() > AbstractPanelMenu.SEARCH_MAX_LENGTH) {
            return trimmed.substring(0, AbstractPanelMenu.SEARCH_MAX_LENGTH);
        }

        return trimmed;
    }
}