package cofh.thermal.core.compat.mekanism.client;

import cofh.core.client.gui.IGuiAccess;
import cofh.core.client.gui.element.ElementBase;
import cofh.core.client.gui.element.ElementButton;
import cofh.core.client.gui.element.ElementTexture;
import cofh.core.common.network.packet.server.TileConfigPacket;
import cofh.core.util.helpers.GuiHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity;
import cofh.thermal.core.compat.mekanism.inventory.ChemicalCellMenu;
import cofh.thermal.lib.client.gui.StorageCellScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static cofh.core.util.helpers.GuiHelper.*;
import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;
import static cofh.lib.util.helpers.StringHelper.format;

public class ChemicalCellScreen extends StorageCellScreen<ChemicalCellMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.parse(ID_THERMAL + ":textures/gui/container/fluid_cell.png");
    private static final String TEX_INCREMENT = ID_COFH_CORE + ":textures/gui/elements/button_increment.png";
    private static final String TEX_DECREMENT = ID_COFH_CORE + ":textures/gui/elements/button_decrement.png";

    public ChemicalCellScreen(ChemicalCellMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, menu.tile, title);
        texture = TEXTURE;
        info = generatePanelInfo("info.thermal.chemical_cell");
        name = "chemical_cell";
    }

    @Override
    public void init() {

        super.init();

        addElement(new ElementTexture(this, 24, 16).setSize(20, 20).setTexture(INFO_INPUT, 20, 20));
        addElement(new ElementTexture(this, 132, 16).setSize(20, 20).setTexture(INFO_OUTPUT, 20, 20));

        ChemicalStorageElement storage = new ChemicalStorageElement(this, 80, 22, menu.tile);
        storage.setSize(18, 42);
        addElement(storage);

        addButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        String input = format(menu.tile.amountInput);
        String output = format(menu.tile.amountOutput);

        guiGraphics.drawString(font, input, getCenteredOffset(input, 34), 42, 0x404040, false);
        guiGraphics.drawString(font, output, getCenteredOffset(output, 142), 42, 0x404040, false);

        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    private void addButtons() {

        addElement(amountButton(19, true, false));
        addElement(amountButton(35, true, true));
        addElement(amountButton(127, false, false));
        addElement(amountButton(143, false, true));
    }

    private ElementBase amountButton(int x, boolean input, boolean increase) {

        return new ElementButton(this, x, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton) + (increase ? 0.1F : -0.1F);
                playClickSound(pitch);

                int current = input ? menu.tile.amountInput : menu.tile.amountOutput;
                if (input) {
                    menu.tile.amountInput += increase ? change : -change;
                } else {
                    menu.tile.amountOutput += increase ? change : -change;
                }
                TileConfigPacket.sendToServer(menu.tile);
                if (input) {
                    menu.tile.amountInput = current;
                } else {
                    menu.tile.amountOutput = current;
                }
                return true;
            }
        }
                .setTooltipFactory(increase ? GuiHelper::createIncControlTooltip : GuiHelper::createDecControlTooltip)
                .setSize(14, 14)
                .setTexture(increase ? TEX_INCREMENT : TEX_DECREMENT, 42, 14)
                .setEnabled(() -> {
                    int value = input ? menu.tile.amountInput : menu.tile.amountOutput;
                    int max = input ? menu.tile.getMaxInput() : menu.tile.getMaxOutput();
                    return increase ? value < max : value > 0;
                });
    }

    private static final class ChemicalStorageElement extends ElementBase {

        private static final ResourceLocation STORAGE_TEXTURE = ResourceLocation.parse(ID_COFH_CORE + ":textures/gui/elements/storage_fluid_medium.png");
        private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.parse(ID_COFH_CORE + ":textures/gui/elements/overlay_fluid_medium.png");

        private final ChemicalCellBlockEntity tile;

        private ChemicalStorageElement(IGuiAccess gui, int x, int y, ChemicalCellBlockEntity tile) {

            super(gui, x, y);
            this.tile = tile;
            texW = 32;
            texH = 64;
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {

            RenderHelper.setPosTexShader();
            RenderHelper.setShaderTexture0(STORAGE_TEXTURE);
            drawTexturedModalRect(guiGraphics.pose(), posX(), posY(), 0, 0, width, height);

            ChemicalStack chemical = tile.getChemical();
            if (!chemical.isEmpty()) {
                int resourceWidth = width - 2;
                int resourceHeight = height - 2;
                int fill = Math.max(1, Math.min(resourceHeight, (int) Math.round((double) chemical.getAmount() * resourceHeight / tile.getChemicalCapacity())));
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(chemical.getChemical().getIcon());
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                RenderHelper.setPosTexShader();
                RenderHelper.setBlockTextureSheet();
                RenderHelper.setShaderColorFromInt(chemical.getChemicalTint());
                RenderHelper.drawTiledTexture(guiGraphics, posX() + 1, posY() + 1 + resourceHeight - fill, sprite, resourceWidth, fill);
                RenderHelper.resetShaderColor();
            }

            RenderHelper.setPosTexShader();
            RenderHelper.setShaderTexture0(OVERLAY_TEXTURE);
            drawTexturedModalRect(guiGraphics.pose(), posX(), posY(), 0, 0, width, height);
        }

        @Override
        public void addTooltip(List<Component> tooltip, int mouseX, int mouseY) {

            ChemicalStack chemical = tile.getChemical();
            if (!chemical.isEmpty()) {
                tooltip.add(Component.translatable(chemical.getTranslationKey()));
            }
            tooltip.add(Component.literal(format(chemical.getAmount()) + " / " + format(tile.getChemicalCapacity()) + " mB"));
        }
    }

}
