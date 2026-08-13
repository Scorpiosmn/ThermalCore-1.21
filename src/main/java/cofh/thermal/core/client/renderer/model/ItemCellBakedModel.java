package cofh.thermal.core.client.renderer.model;

import cofh.core.client.renderer.model.ModelUtils;
import cofh.lib.api.item.IInventoryContainerItem;
import cofh.lib.client.renderer.block.model.RetexturedBakedQuad;
import cofh.lib.util.crafting.ComparableItemStack;
import cofh.lib.util.helpers.MathHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cofh.lib.util.constants.NBTTags.TAG_BLOCK_ENTITY;
import static cofh.lib.util.constants.NBTTags.TAG_SIDES;
import static cofh.thermal.core.client.ThermalTextures.*;
import static cofh.thermal.lib.util.Constants.DEFAULT_CELL_SIDES_RAW;
import static net.minecraft.core.Direction.*;

public class ItemCellBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {

    private static final Map<List<Integer>, BakedQuad> FACE_QUAD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, BakedQuad[]> SIDE_QUAD_CACHE = new ConcurrentHashMap<>();

    private static final Map<Integer, BakedQuad[]> ITEM_QUAD_CACHE = new ConcurrentHashMap<>();
    private static final Map<List<Integer>, BakedModel> MODEL_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {

        FACE_QUAD_CACHE.clear();
        SIDE_QUAD_CACHE.clear();

        ITEM_QUAD_CACHE.clear();
        MODEL_CACHE.clear();
    }

    public ItemCellBakedModel(BakedModel originalModel) {

        super(originalModel);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {

        LinkedList<BakedQuad> quads = new LinkedList<>(originalModel.getQuads(state, side, rand, extraData, renderType));
        if (side == null || quads.isEmpty()) {
            return quads;
        }
        BakedQuad baseQuad = quads.get(0);
        int sideIndex = side.get3DDataValue();

        // FACE
        Direction face = extraData.get(ModelUtils.FACING);
        if (side == face) {
            Integer level = extraData.get(ModelUtils.LEVEL);
            if (level == null) {
                // This shouldn't happen, but playing it safe.
                return quads;
            }
            BakedQuad faceQuad = FACE_QUAD_CACHE.computeIfAbsent(Arrays.asList(face.get3DDataValue(), level),
                    k -> new RetexturedBakedQuad(baseQuad, getLevelTexture(level)));
            quads.add(faceQuad);
        }

        // SIDES
        byte[] sideConfigRaw = extraData.get(ModelUtils.SIDES);
        if (sideConfigRaw == null) {
            // This shouldn't happen, but playing it safe.
            return quads;
        }
        int configHash = Arrays.hashCode(sideConfigRaw);
        BakedQuad[] cachedSideQuads = SIDE_QUAD_CACHE.computeIfAbsent(configHash, k -> new BakedQuad[6]);
        BakedQuad sideQuad = cachedSideQuads[sideIndex];
        if (sideQuad == null) {
            synchronized (cachedSideQuads) {
                sideQuad = cachedSideQuads[sideIndex];
                if (sideQuad == null) {
                    sideQuad = new RetexturedBakedQuad(baseQuad, getConfigTexture(sideConfigRaw[sideIndex]));
                    cachedSideQuads[sideIndex] = sideQuad;
                }
            }
        }
        quads.add(sideQuad);

        return quads;
    }

    @Override
    public ItemOverrides getOverrides() {

        return overrideList;
    }

    private final ItemOverrides overrideList = new ItemOverrides() {

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn, int seed) {

            CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
            byte[] sideConfigRaw = getSideConfigRaw(tag);
            int itemHash = new ComparableItemStack(stack).hashCode();
            int level = getLevel(stack);
            int configHash = Arrays.hashCode(sideConfigRaw);

            BakedModel ret = MODEL_CACHE.get(Arrays.asList(itemHash, level, configHash));
            if (ret == null) {
                ModelUtils.WrappedBakedModelBuilder builder = new ModelUtils.WrappedBakedModelBuilder(model);

                // FACE
                builder.addFaceQuad(NORTH, new RetexturedBakedQuad(builder.getQuads(NORTH).get(0), getLevelTexture(level)));

                // SIDES
                BakedQuad[] cachedQuads = ITEM_QUAD_CACHE.computeIfAbsent(configHash, k -> new BakedQuad[6]);
                synchronized (cachedQuads) {
                    if (cachedQuads[0] == null) {
                        cachedQuads[0] = new RetexturedBakedQuad(builder.getQuads(DOWN).get(0), getConfigTexture(sideConfigRaw[0]));
                        cachedQuads[1] = new RetexturedBakedQuad(builder.getQuads(UP).get(0), getConfigTexture(sideConfigRaw[1]));
                        cachedQuads[2] = new RetexturedBakedQuad(builder.getQuads(NORTH).get(0), getConfigTexture(sideConfigRaw[2]));
                        cachedQuads[3] = new RetexturedBakedQuad(builder.getQuads(SOUTH).get(0), getConfigTexture(sideConfigRaw[3]));
                        cachedQuads[4] = new RetexturedBakedQuad(builder.getQuads(WEST).get(0), getConfigTexture(sideConfigRaw[4]));
                        cachedQuads[5] = new RetexturedBakedQuad(builder.getQuads(EAST).get(0), getConfigTexture(sideConfigRaw[5]));
                    }
                }
                builder.addFaceQuad(DOWN, cachedQuads[0]);
                builder.addFaceQuad(UP, cachedQuads[1]);
                builder.addFaceQuad(NORTH, cachedQuads[2]);
                builder.addFaceQuad(SOUTH, cachedQuads[3]);
                builder.addFaceQuad(WEST, cachedQuads[4]);
                builder.addFaceQuad(EAST, cachedQuads[5]);

                ret = builder.build();
                BakedModel prev = MODEL_CACHE.putIfAbsent(Arrays.asList(itemHash, level, configHash), ret);
                if (prev != null) {
                    ret = prev;
                }
            }
            return ret;
        }
    };

    // region HELPERS
    private TextureAtlasSprite getConfigTexture(byte side) {

        switch (side) {
            case 1:
                return CELL_CONFIG_INPUT;
            case 2:
                return CELL_CONFIG_OUTPUT;
            case 3:
                return CELL_CONFIG_BOTH;
            default:
                return CELL_CONFIG_NONE;
        }
    }

    private TextureAtlasSprite getLevelTexture(int level) {

        // Creative returned as 9
        if (level > 8) {
            return ITEM_CELL_LEVEL_8_C;
        }
        return ITEM_CELL_LEVELS[MathHelper.clamp(level, 0, 8)];
    }

    private byte[] getSideConfigRaw(CompoundTag tag) {

        if (tag == null) {
            return DEFAULT_CELL_SIDES_RAW;
        }
        byte[] ret = tag.getByteArray(TAG_SIDES);
        return ret.length == 0 ? DEFAULT_CELL_SIDES_RAW : ret;
    }

    private int getLevel(ItemStack stack) {

        Item item = stack.getItem();
        //        if (item instanceof ICoFHItem && ((ICoFHItem) item).isCreative(stack, ITEM)) {
        //            return 9;
        //        }
        if (item instanceof IInventoryContainerItem && ((IInventoryContainerItem) item).getItemAmount(stack, 0) > 0) {
            return 1 + Math.min(((IInventoryContainerItem) item).getScaledItemsStored(stack, 0, 8), 7);
        }
        return 0;
    }
    // endregion
}
