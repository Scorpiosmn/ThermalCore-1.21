package cofh.thermal.lib.compat.jei;

import cofh.thermal.lib.util.recipes.ThermalCatalyst;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import static cofh.lib.util.helpers.StringHelper.DF0;
import static cofh.lib.util.helpers.StringHelper.localize;

public abstract class ThermalCatalystCategory<T extends RecipeHolder<? extends ThermalCatalyst>> implements IRecipeCategory<T> {

    protected final RecipeType<T> type;
    protected IDrawable background;
    protected IDrawable icon;
    protected Component name;

    protected IDrawableStatic slot;

    public ThermalCatalystCategory(IGuiHelper guiHelper, ItemStack icon, RecipeType<T> type) {

        this.type = type;
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);

        background = guiHelper.drawableBuilder(Drawables.JEI_TEXTURE, 26, 11, 140, 62)
                .addPadding(0, 0, 16, 8)
                .build();
        slot = Drawables.getDrawables(guiHelper).getSlot(Drawables.SLOT);
    }

    // region IRecipeCategory
    @Override
    public Component getTitle() {

        return name;
    }

    @Override
    public IDrawable getBackground() {

        return background;
    }

    @Override
    public IDrawable getIcon() {

        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 17, 23)
                .addIngredients(recipe.value().getIngredient());
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {

        slot.draw(guiGraphics, 16, 22);

        Minecraft minecraft = Minecraft.getInstance();

        String primaryMod = localize("info.thermal.primary_mod") + ": " + recipe.value().getPrimaryMod() + "x";
        guiGraphics.drawString(minecraft.font, primaryMod, 44, 8, 0xFF606060, false);

        String secondaryMod = localize("info.thermal.secondary_mod") + ": " + recipe.value().getSecondaryMod() + "x";
        guiGraphics.drawString(minecraft.font, secondaryMod, 44, 20, 0xFF606060, false);

        String energyMod = localize("info.thermal.energy_mod") + ": " + recipe.value().getEnergyMod() + "x";
        guiGraphics.drawString(minecraft.font, energyMod, 44, 32, 0xFF606060, false);

        String useChance = localize("info.thermal.use_chance") + ": " + DF0.format(recipe.value().getUseChance() * 100) + "%";
        guiGraphics.drawString(minecraft.font, useChance, 44, 44, 0xFF606060, false);
    }
    // endregion
}
