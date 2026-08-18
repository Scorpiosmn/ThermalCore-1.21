package cofh.thermal.core.compat.mekanism.client;

import cofh.core.client.model.SimpleModel;
import cofh.thermal.core.compat.mekanism.client.renderer.ChemicalCellBakedModel;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static cofh.thermal.core.compat.mekanism.MekanismCompat.CHEMICAL_CELL_CONTAINER;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

/** Client-only Mekanism integration, loaded only after the Mekanism presence check. */
public final class MekanismClientCompat {

    private MekanismClientCompat() {

    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {

        event.register(CHEMICAL_CELL_CONTAINER.get(), ChemicalCellScreen::new);
    }

    public static void registerModels(RegisterGeometryLoaders event) {

        event.register(fromNamespaceAndPath(ID_THERMAL, "chemical_cell"), new SimpleModel.Loader(ChemicalCellBakedModel::new));
    }

    public static void clearModelCache() {

        ChemicalCellBakedModel.clearCache();
    }

}
