package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import javax.annotation.Nullable;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.FISHER_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.FISHER_BOOST;

public class FisherBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected final ResourceLocation lootTable;
    protected final float outputMod;
    protected final float useChance;

    public FisherBoost(Ingredient inputItem, ResourceLocation lootTable, float outputMod, float useChance) {

        this.ingredient = inputItem;
        this.lootTable = lootTable;
        this.outputMod = outputMod;
        this.useChance = useChance;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return FISHER_BOOST_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {

        return FISHER_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public ResourceLocation getLootTable() {

        return lootTable;
    }

    public float getOutputMod() {

        return outputMod;
    }

    public float getUseChance() {

        return useChance;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<FisherBoost> {

        public static final Codec<FisherBoost> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                        ResourceLocation.CODEC.optionalFieldOf(LOOT_TABLE, BuiltInLootTables.FISHING_FISH).forGetter(recipe -> recipe.lootTable),
                        Codec.FLOAT.optionalFieldOf(OUTPUT_MOD, 1.0F).forGetter(recipe -> recipe.outputMod),
                        Codec.FLOAT.optionalFieldOf(USE_CHANCE, 1.0F).forGetter(recipe -> recipe.useChance)
                ).apply(builder, FisherBoost::new)
        );

        @Override
        public Codec<FisherBoost> codec() {

            return CODEC;
        }

        //        @Override
        //        public FisherBoost fromJson(ResourceLocation recipeId, JsonObject json) {
        //
        //            Ingredient ingredient;
        //            ResourceLocation lootTable = BuiltInLootTables.FISHING_FISH;
        //            float outputMod = 1.0F;
        //            float useChance = 1.0F;
        //
        //            /* INPUT */
        //            ingredient = parseIngredient(json.get(INGREDIENT));
        //
        //            if (json.has(LOOT_TABLE)) {
        //                String lootTableString = json.get(LOOT_TABLE).getAsString();
        //                lootTable = ResourceLocation.tryParse(lootTableString);
        //            }
        //            if (json.has(OUTPUT)) {
        //                outputMod = json.get(OUTPUT).getAsFloat();
        //            } else if (json.has(OUTPUT_MOD)) {
        //                outputMod = json.get(OUTPUT_MOD).getAsFloat();
        //            }
        //            if (json.has(USE_CHANCE)) {
        //                useChance = json.get(USE_CHANCE).getAsFloat();
        //            }
        //            return new FisherBoost(ingredient, lootTable, outputMod, useChance);
        //        }

        @Nullable
        @Override
        public FisherBoost fromNetwork(FriendlyByteBuf buffer) {

            Ingredient ingredient = Ingredient.fromNetwork(buffer);

            ResourceLocation lootTable = buffer.readResourceLocation();
            float outputMod = buffer.readFloat();
            float useChance = buffer.readFloat();

            return new FisherBoost(ingredient, lootTable, outputMod, useChance);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FisherBoost recipe) {

            recipe.ingredient.toNetwork(buffer);

            buffer.writeResourceLocation(recipe.lootTable);
            buffer.writeFloat(recipe.outputMod);
            buffer.writeFloat(recipe.useChance);
        }

    }
    // endregion
}
