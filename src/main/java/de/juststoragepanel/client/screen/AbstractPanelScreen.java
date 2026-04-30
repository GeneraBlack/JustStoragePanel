package de.juststoragepanel.client.screen;

import de.juststoragepanel.menu.AbstractPanelMenu;
import de.juststoragepanel.menu.NetworkDisplaySlot;
import de.juststoragepanel.network.PanelSearchPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class AbstractPanelScreen<T extends AbstractPanelMenu> extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {
    private static final int DISPLAY_PANEL_WIDTH = 170;
    private static final int DISPLAY_PANEL_HEIGHT = 30 + AbstractPanelMenu.DISPLAY_ROWS * 18;

    private Button previousPageButton;
    private Button nextPageButton;
    private EditBox searchBox;

    protected AbstractPanelScreen(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = imageHeight - 92;
    }

    @Override
    protected void init() {
        super.init();
        this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(0))
                .bounds(this.leftPos + this.imageWidth - 54, this.topPos + 6, 20, 20)
                .build());
        this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                .bounds(this.leftPos + this.imageWidth - 30, this.topPos + 6, 20, 20)
                .build());
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, this.leftPos + 12, this.topPos + 22, DISPLAY_PANEL_WIDTH - 16, 12, Component.translatable("screen.juststoragepanel.search")));
        this.searchBox.setMaxLength(AbstractPanelMenu.SEARCH_MAX_LENGTH);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFF2F2F2);
        this.searchBox.setTextColorUneditable(0xFF9AA7B4);
        this.searchBox.setHint(Component.translatable("screen.juststoragepanel.search_hint"));
        this.searchBox.setValue(this.menu.getSearchQuery());
        this.searchBox.setResponder(this::onSearchChanged);
        this.updatePageButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updatePageButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0.0D && this.menu.hasPreviousPage()) {
            this.changePage(0);
            return true;
        }

        if (scrollY < 0.0D && this.menu.hasNextPage()) {
            this.changePage(1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemStack, Slot slot, String countString) {
        String overlayCount = countString;
        if (overlayCount == null && slot instanceof NetworkDisplaySlot && !itemStack.isEmpty()) {
            int totalCount = this.menu.getDisplayCount(slot.index);
            if (totalCount > 1) {
                overlayCount = this.formatCount(totalCount);
            }
        }

        super.renderSlotContents(guiGraphics, itemStack, slot, overlayCount);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        this.drawPanel(guiGraphics, x, y, this.imageWidth, this.imageHeight, 0xFF21262D, 0xFF2F3640, 0xFF151A20);
        this.drawPanel(guiGraphics, x + 6, y + 16, DISPLAY_PANEL_WIDTH, DISPLAY_PANEL_HEIGHT, 0xFF161B22, 0xFF262C35, 0xFF0D1117);
        this.drawPanel(guiGraphics, x + 10, y + 20, DISPLAY_PANEL_WIDTH - 8, 16, 0xFF0F141B, 0xFF2A3440, 0xFF131B24);
        this.drawSlotGrid(guiGraphics, x + AbstractPanelMenu.DISPLAY_X, y + AbstractPanelMenu.DISPLAY_Y, AbstractPanelMenu.DISPLAY_COLUMNS, AbstractPanelMenu.DISPLAY_ROWS);
        this.drawPlayerInventory(guiGraphics, x + 6, y + AbstractPanelMenu.PLAYER_INVENTORY_Y - 6);
        this.renderExtraBackground(guiGraphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 10, 8, 0xFFF4F1DE, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFD9D2B6, false);

        Component pageText = Component.literal(this.menu.getCurrentPage() + " / " + this.menu.getMaxPage());
        int pageX = this.imageWidth - 94 - this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, pageX, 11, 0xFFE07A5F, false);
    }

    protected abstract void renderExtraBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY);

    private void changePage(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updatePageButtons() {
        if (this.previousPageButton != null) {
            this.previousPageButton.active = this.menu.hasPreviousPage();
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.menu.hasNextPage();
        }
    }

    private void onSearchChanged(String query) {
        PacketDistributor.sendToServer(new PanelSearchPayload(this.menu.containerId, query));
    }

    private String formatCount(int count) {
        if (count < 1_000) {
            return Integer.toString(count);
        }

        if (count < 10_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1_000.0D);
        }

        if (count < 1_000_000) {
            return (count / 1_000) + "k";
        }

        if (count < 10_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0D);
        }

        return (count / 1_000_000) + "M";
    }

    private void drawPlayerInventory(GuiGraphics guiGraphics, int x, int y) {
        this.drawPanel(guiGraphics, x, y, 170, 76, 0xFF171D24, 0xFF262D36, 0xFF0E141A);
        this.drawSlotGrid(guiGraphics, x + 4, y + 4, 9, 3);
        this.drawSlotGrid(guiGraphics, x + 4, y + 62, 9, 1);
    }

    protected final void drawSlotGrid(GuiGraphics guiGraphics, int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                this.drawSlot(guiGraphics, x + column * 18, y + row * 18);
            }
        }
    }

    protected final void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF0B0F14);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1E2530);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF131922);
    }

    protected final void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int outerColor, int borderColor, int innerColor) {
        guiGraphics.fill(x, y, x + width, y + height, outerColor);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, borderColor);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, innerColor);
    }
}