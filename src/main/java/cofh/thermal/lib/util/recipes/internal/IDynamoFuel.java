package cofh.thermal.lib.util.recipes.internal;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public interface IDynamoFuel {

    List<ItemStack> getInputItems();

    List<FluidStack> getInputFluids();

    int getEnergy();

}
