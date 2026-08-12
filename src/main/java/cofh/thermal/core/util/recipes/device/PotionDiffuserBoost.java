package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.util.managers.device.PotionDiffuserManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.POTION_DIFFUSER_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.POTION_DIFFUSER_BOOST;

public class PotionDiffuserBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected int amplifier;
    protected float durationMod;
    protected int cycles;

    public PotionDiffuserBoost(Ingredient inputItem, int amplifier, float durationMod, int cycles) {

        this.ingredient = inputItem;
        this.amplifier = amplifier;
        this.durationMod = durationMod;
        this.cycles = cycles;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return POTION_DIFFUSER_BOOST_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {

        return POTION_DIFFUSER_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public int getAmplifier() {

        return amplifier;
    }

    public float getDurationMod() {

        return durationMod;
    }

    public int getCycles() {

        return cycles;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<PotionDiffuserBoost> {

        public static final Codec<PotionDiffuserBoost> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                        Codec.INT.optionalFieldOf(AMPLIFIER, 0).forGetter(recipe -> recipe.amplifier),
                        Codec.FLOAT.optionalFieldOf(DURATION_MOD, 0.0F).forGetter(recipe -> recipe.durationMod),
                        Codec.INT.optionalFieldOf(CYCLES, PotionDiffuserManager.instance().getDefaultEnergy()).forGetter(recipe -> recipe.cycles)
                ).apply(builder, PotionDiffuserBoost::new)
        );

        @Override
        public Codec<PotionDiffuserBoost> codec() {

            return CODEC;
        }

        //        @Override
        //        public PotionDiffuserBoost fromJson(ResourceLocation recipeId, JsonObject json) {
        //
        //            Ingredient ingredient;
        //            int amplifier = 0;
        //            float durationMod = 0.0F;
        //            int cycles = PotionDiffuserManager.instance().getDefaultEnergy();
        //
        //            /* INPUT */
        //            ingredient = parseIngredient(json.get(INGREDIENT));
        //
        //            if (json.has(AMPLIFIER)) {
        //                amplifier = json.get(AMPLIFIER).getAsInt();
        //            }
        //            if (json.has(DURATION_MOD)) {
        //                durationMod = json.get(DURATION_MOD).getAsFloat();
        //            }
        //            if (json.has(CYCLES)) {
        //                cycles = json.get(CYCLES).getAsInt();
        //            }
        //            return new PotionDiffuserBoost(recipeId, ingredient, amplifier, durationMod, cycles);
        //        }

        @Nullable
        @Override
        public PotionDiffuserBoost fromNetwork(FriendlyByteBuf buffer) {

            Ingredient ingredient = Ingredient.fromNetwork(buffer);

            int amplifier = buffer.readInt();
            float durationMod = buffer.readFloat();
            int cycles = buffer.readInt();

            return new PotionDiffuserBoost(ingredient, amplifier, durationMod, cycles);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PotionDiffuserBoost recipe) {

            recipe.ingredient.toNetwork(buffer);

            buffer.writeInt(recipe.amplifier);
            buffer.writeFloat(recipe.durationMod);
            buffer.writeInt(recipe.cycles);
        }

    }
    // endregion
}
