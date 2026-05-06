package de.juststoragepanel.network;

import de.juststoragepanel.JustStoragePanel;
import de.juststoragepanel.menu.CraftingPanelMenu;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CraftingPanelRecipeTransferPayload(int containerId, boolean maxTransfer, List<ItemStack> ingredients) implements CustomPacketPayload {
    public static final Type<CraftingPanelRecipeTransferPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JustStoragePanel.MODID, "crafting_panel_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingPanelRecipeTransferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CraftingPanelRecipeTransferPayload::containerId,
            ByteBufCodecs.BOOL,
            CraftingPanelRecipeTransferPayload::maxTransfer,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            CraftingPanelRecipeTransferPayload::ingredients,
            CraftingPanelRecipeTransferPayload::new);

    public CraftingPanelRecipeTransferPayload {
        ingredients = List.copyOf(ingredients);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, CraftingPanelRecipeTransferPayload::handle);
    }

    @Override
    public Type<CraftingPanelRecipeTransferPayload> type() {
        return TYPE;
    }

    private static void handle(CraftingPanelRecipeTransferPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.containerMenu instanceof CraftingPanelMenu menu)) {
            return;
        }

        if (player.containerMenu.containerId != payload.containerId()) {
            return;
        }

        menu.handleRecipeTransfer(player, payload.ingredients(), payload.maxTransfer());
    }
}