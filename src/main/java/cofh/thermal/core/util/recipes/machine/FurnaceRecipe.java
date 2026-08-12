package cofh.thermal.core.util.recipes.machine;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.core.ThermalCore;
import cofh.thermal.core.util.managers.machine.FurnaceRecipeManager;
import cofh.thermal.lib.util.recipes.ThermalRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static cofh.lib.util.Constants.BASE_CHANCE_LOCKED;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.FURNACE_RECIPE_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.FURNACE_RECIPE;

public class FurnaceRecipe extends ThermalRecipe {

    public FurnaceRecipe(int energy, float experience, @Nullable List<Ingredient> inputItems, @Nullable List<FluidIngredient> inputFluids, @Nullable List<ItemStack> outputItems, @Nullable List<Float> outputItemChances, @Nullable List<FluidStack> outputFluids) {

        super(energy, experience, inputItems, inputFluids, outputItems, outputItemChances, outputFluids);

        if (this.energy <= 0) {
            int defaultEnergy = FurnaceRecipeManager.instance().getDefaultEnergy();
            ThermalCore.LOG.warn("Energy value for a Redstone Furnace recipe was out of allowable range and has been set to a default value of " + defaultEnergy + ".");
            this.energy = defaultEnergy;
        }
    }

    public FurnaceRecipe(int energy, float experience, AbstractCookingRecipe recipe) {

        this(energy, experience, recipe.getIngredients(), Collections.emptyList(), Collections.singletonList(recipe.result), Collections.singletonList(BASE_CHANCE_LOCKED), Collections.emptyList());
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {

        return FURNACE_RECIPE_SERIALIZER.get();
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {

        return FURNACE_RECIPE.get();
    }

}
