package cofh.thermal.lib.util.recipes;

import cofh.lib.util.recipes.SerializableRecipe;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * This class really just serves as a way to ride on Mojang's automated recipe syncing and datapack functionality.
 * Nothing in Thermal actually uses any of this for logic whatsoever. It's part of a shim layer, nothing more.
 */
public abstract class ThermalCatalyst extends SerializableRecipe {

    public final Ingredient ingredient;

    public final float primaryMod;
    public final float secondaryMod;
    public final float energyMod;
    public final float minChance;
    public final float useChance;

    protected ThermalCatalyst(Ingredient ingredient, float primaryMod, float secondaryMod, float energyMod, float minChance, float useChance) {

        this.ingredient = ingredient;

        this.primaryMod = primaryMod;
        this.secondaryMod = secondaryMod;
        this.energyMod = energyMod;
        this.minChance = minChance;
        this.useChance = useChance;
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public float getPrimaryMod() {

        return primaryMod;
    }

    public float getSecondaryMod() {

        return secondaryMod;
    }

    public float getEnergyMod() {

        return energyMod;
    }

    public float getMinChance() {

        return minChance;
    }

    public float getUseChance() {

        return useChance;
    }
    // endregion
}
