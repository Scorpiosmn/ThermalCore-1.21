package cofh.thermal.core.compat.jei;

import cofh.thermal.core.util.recipes.device.RockGenMapping;
import cofh.thermal.core.util.recipes.device.TreeExtractorMapping;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;

import static cofh.thermal.core.init.registries.TCoreRecipeTypes.ROCK_GEN_MAPPING;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_MAPPING;

public class TCoreJeiRecipeTypes {

    private TCoreJeiRecipeTypes() {

    }

    public static final RecipeType<RecipeHolder<TreeExtractorMapping>> TREE_EXTRACTOR_TYPE = RecipeType.createFromVanilla(TREE_EXTRACTOR_MAPPING.get());
    public static final RecipeType<RecipeHolder<RockGenMapping>> ROCK_GEN_TYPE = RecipeType.createFromVanilla(ROCK_GEN_MAPPING.get());

}
