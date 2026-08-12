package cofh.thermal.lib.util.recipes;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.ThermalCore;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public abstract class ThermalFuel extends SerializableRecipe {

    public final List<Ingredient> inputItems = new ArrayList<>();
    public final List<FluidIngredient> inputFluids = new ArrayList<>();

    public int energy;

    protected ThermalFuel(int energy, List<Ingredient> inputItems, List<FluidIngredient> inputFluids) {

        if ((inputItems == null || inputItems.isEmpty()) && (inputFluids == null || inputFluids.isEmpty())) {
            ThermalCore.LOG.warn("Invalid Thermal Series fuel! Please check your datapacks!");
        }
        this.energy = energy;

        if (inputItems != null) {
            this.inputItems.addAll(inputItems);
        }
        if (inputFluids != null) {
            this.inputFluids.addAll(inputFluids);
        }
        trim();
    }

    private void trim() {

        ((ArrayList<Ingredient>) this.inputItems).trimToSize();
        ((ArrayList<FluidIngredient>) this.inputFluids).trimToSize();
    }

    // region GETTERS
    public List<Ingredient> getInputItems() {

        return inputItems;
    }

    public List<FluidIngredient> getInputFluids() {

        return inputFluids;
    }

    public int getEnergy() {

        return energy;
    }
    // endregion
}
