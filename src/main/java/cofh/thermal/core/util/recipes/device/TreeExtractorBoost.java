package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.util.managers.device.TreeExtractorManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.TREE_EXTRACTOR_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_BOOST;

public class TreeExtractorBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected float outputMod;
    protected int cycles;

    public TreeExtractorBoost(Ingredient inputItem, float outputMod, int cycles) {

        this.ingredient = inputItem;
        this.outputMod = outputMod;
        this.cycles = cycles;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return TREE_EXTRACTOR_BOOST_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {

        return TREE_EXTRACTOR_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public float getOutputMod() {

        return outputMod;
    }

    public int getCycles() {

        return cycles;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<TreeExtractorBoost> {

        public static final Codec<TreeExtractorBoost> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                        Codec.FLOAT.optionalFieldOf(OUTPUT_MOD, 1.0F).forGetter(recipe -> recipe.outputMod),
                        Codec.INT.optionalFieldOf(CYCLES, TreeExtractorManager.instance().getDefaultEnergy()).forGetter(recipe -> recipe.cycles)
                ).apply(builder, TreeExtractorBoost::new)
        );

        @Override
        public Codec<TreeExtractorBoost> codec() {

            return CODEC;
        }

        //        @Override
        //        public TreeExtractorBoost fromJson(ResourceLocation recipeId, JsonObject json) {
        //
        //            Ingredient ingredient;
        //            float outputMod = 1.0F;
        //            int cycles = TreeExtractorManager.instance().getDefaultEnergy();
        //
        //            /* INPUT */
        //            ingredient = parseIngredient(json.get(INGREDIENT));
        //
        //            if (json.has(OUTPUT)) {
        //                outputMod = json.get(OUTPUT).getAsFloat();
        //            } else if (json.has(OUTPUT_MOD)) {
        //                outputMod = json.get(OUTPUT_MOD).getAsFloat();
        //            }
        //            if (json.has(CYCLES)) {
        //                cycles = json.get(CYCLES).getAsInt();
        //            }
        //            return new TreeExtractorBoost(recipeId, ingredient, outputMod, cycles);
        //        }

        @Nullable
        @Override
        public TreeExtractorBoost fromNetwork(FriendlyByteBuf buffer) {

            Ingredient ingredient = Ingredient.fromNetwork(buffer);

            float outputMod = buffer.readFloat();
            int cycles = buffer.readInt();

            return new TreeExtractorBoost(ingredient, outputMod, cycles);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TreeExtractorBoost recipe) {

            recipe.ingredient.toNetwork(buffer);

            buffer.writeFloat(recipe.outputMod);
            buffer.writeInt(recipe.cycles);
        }

    }
    // endregion
}
