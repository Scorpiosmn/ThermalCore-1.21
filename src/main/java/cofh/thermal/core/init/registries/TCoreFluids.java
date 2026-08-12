package cofh.thermal.core.init.registries;

import cofh.thermal.core.common.fluid.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

import static cofh.core.util.helpers.FluidHelper.BOTTLE_DRAIN_MAP;
import static cofh.core.util.helpers.FluidHelper.BOTTLE_FILL_MAP;
import static cofh.lib.util.Constants.BOTTLE_VOLUME;
import static cofh.thermal.core.ThermalCore.ITEMS;

public class TCoreFluids {

    private TCoreFluids() {

    }

    public static void register() {

        REDSTONE_FLUID = RedstoneFluid.instance().still();
        GLOWSTONE_FLUID = GlowstoneFluid.instance().still();
        ENDER_FLUID = EnderFluid.instance().still();

        SAP_FLUID = SapFluid.instance().still();
        SYRUP_FLUID = SyrupFluid.instance().still();
        RESIN_FLUID = ResinFluid.instance().still();
        TREE_OIL_FLUID = TreeOilFluid.instance().still();
        LATEX_FLUID = LatexFluid.instance().still();

        CREOSOTE_FLUID = CreosoteFluid.instance().still();
        CRUDE_OIL_FLUID = CrudeOilFluid.instance().still();
        HEAVY_OIL_FLUID = HeavyOilFluid.instance().still();
        LIGHT_OIL_FLUID = LightOilFluid.instance().still();
        REFINED_FUEL_FLUID = RefinedFuelFluid.instance().still();
    }

    public static void setup() {

        BOTTLE_DRAIN_MAP.put(ITEMS.get("syrup_bottle"), (stack -> new FluidStack(SYRUP_FLUID.get(), BOTTLE_VOLUME)));

        BOTTLE_FILL_MAP.put(fluid -> fluid.getFluid().equals(SYRUP_FLUID.get()), fluid -> new ItemStack(ITEMS.get("syrup_bottle")));

        FireBlock fire = (FireBlock) Blocks.FIRE;
        fire.setFlammable(CrudeOilFluid.instance().block().get(), 5, 10);
    }

    public static Supplier<BaseFlowingFluid> REDSTONE_FLUID;
    public static Supplier<BaseFlowingFluid> GLOWSTONE_FLUID;
    public static Supplier<BaseFlowingFluid> ENDER_FLUID;

    public static Supplier<BaseFlowingFluid> SAP_FLUID;
    public static Supplier<BaseFlowingFluid> SYRUP_FLUID;
    public static Supplier<BaseFlowingFluid> RESIN_FLUID;
    public static Supplier<BaseFlowingFluid> TREE_OIL_FLUID;
    public static Supplier<BaseFlowingFluid> LATEX_FLUID;

    public static Supplier<BaseFlowingFluid> CREOSOTE_FLUID;
    public static Supplier<BaseFlowingFluid> CRUDE_OIL_FLUID;
    public static Supplier<BaseFlowingFluid> HEAVY_OIL_FLUID;
    public static Supplier<BaseFlowingFluid> LIGHT_OIL_FLUID;
    public static Supplier<BaseFlowingFluid> REFINED_FUEL_FLUID;

}
