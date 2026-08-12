package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

import static cofh.lib.util.Utils.getRegistryName;
import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.HIVE_EXTRACTOR_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.HIVE_EXTRACTOR_MAPPING;

public class HiveExtractorMapping extends SerializableRecipe {

    protected final Block hive;
    protected final ItemStack item;
    protected final FluidStack fluid;

    public HiveExtractorMapping(Block hive, ItemStack item, FluidStack fluid) {

        this.hive = hive;
        this.item = item;
        this.fluid = fluid;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return HIVE_EXTRACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {

        return HIVE_EXTRACTOR_MAPPING.get();
    }

    // region GETTERS
    public Block getHive() {

        return hive;
    }

    public ItemStack getItem() {

        return item;
    }

    public FluidStack getFluid() {

        return fluid;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<HiveExtractorMapping> {

        public static final Codec<HiveExtractorMapping> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                        Block.CODEC.fieldOf(HIVE).forGetter(recipe -> recipe.hive),
                        ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf(ITEM).forGetter(recipe -> recipe.item),
                        FluidStack.CODEC.fieldOf(FLUID).forGetter(recipe -> recipe.fluid)
                ).apply(builder, HiveExtractorMapping::new)
        );

        @Override
        public Codec<HiveExtractorMapping> codec() {

            return CODEC;
        }

        //        @Override
        //        public HiveExtractorMapping fromJson(ResourceLocation recipeId, JsonObject json) {
        //
        //            Block hive = Blocks.AIR;
        //            ItemStack item = ItemStack.EMPTY;
        //            FluidStack fluid = FluidStack.EMPTY;
        //
        //            if (json.has(HIVE)) {
        //                hive = parseBlock(json.get(HIVE));
        //            }
        //            if (json.has(ITEM)) {
        //                item = parseItemStack(json.get(ITEM));
        //            }
        //            if (json.has(FLUID)) {
        //                fluid = parseFluidStack(json.get(FLUID));
        //            }
        //            return new HiveExtractorMapping(recipeId, hive, item, fluid);
        //        }

        @Nullable
        @Override
        public HiveExtractorMapping fromNetwork(FriendlyByteBuf buffer) {

            Block hive = BuiltInRegistries.BLOCK.get(buffer.readResourceLocation());
            ItemStack item = buffer.readItem();
            FluidStack fluid = buffer.readFluidStack();

            return new HiveExtractorMapping(hive, item, fluid);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, HiveExtractorMapping recipe) {

            buffer.writeResourceLocation(getRegistryName(recipe.hive));
            buffer.writeItem(recipe.item);
            buffer.writeFluidStack(recipe.fluid);
        }

    }
    // endregion
}
