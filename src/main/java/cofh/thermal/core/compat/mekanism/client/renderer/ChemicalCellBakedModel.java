package cofh.thermal.core.compat.mekanism.client.renderer;

import cofh.core.client.renderer.model.ModelUtils;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.client.renderer.block.model.RetexturedBakedQuad;
import cofh.lib.util.helpers.MathHelper;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

import static cofh.thermal.core.client.ThermalTextures.CELL_CONFIG_BOTH;
import static cofh.thermal.core.client.ThermalTextures.CELL_CONFIG_INPUT;
import static cofh.thermal.core.client.ThermalTextures.CELL_CONFIG_NONE;
import static cofh.thermal.core.client.ThermalTextures.CELL_CONFIG_OUTPUT;
import static cofh.thermal.core.client.ThermalTextures.FLUID_CELL_LEVELS;
import static cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity.CHEMICAL;

/** Dynamic side configuration and chemical underlay for the Chemical Cell. */
public class ChemicalCellBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {

    private static final Map<List<Integer>, BakedQuad> FACE_QUAD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, BakedQuad[]> SIDE_QUAD_CACHE = new ConcurrentHashMap<>();

    public ChemicalCellBakedModel(BakedModel originalModel) {

        super(originalModel);
    }

    public static void clearCache() {

        FACE_QUAD_CACHE.clear();
        SIDE_QUAD_CACHE.clear();
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource random, @Nonnull ModelData modelData, @Nullable RenderType renderType) {

        LinkedList<BakedQuad> quads = new LinkedList<>(originalModel.getQuads(state, side, random, modelData, renderType));
        if (side == null || quads.isEmpty()) {
            return quads;
        }
        BakedQuad baseQuad = quads.getFirst();

        Direction facing = modelData.get(ModelUtils.FACING);
        Integer level = modelData.get(ModelUtils.LEVEL);
        if (side == facing && level != null && quads.size() > 1) {
            BakedQuad faceQuad = FACE_QUAD_CACHE.computeIfAbsent(Arrays.asList(facing.get3DDataValue(), level),
                    key -> new RetexturedBakedQuad(quads.get(1), FLUID_CELL_LEVELS[MathHelper.clamp(level, 0, 8)]));
            quads.add(faceQuad);
        }

        byte[] sideConfig = modelData.get(ModelUtils.SIDES);
        if (sideConfig != null) {
            int configHash = Arrays.hashCode(sideConfig);
            BakedQuad[] cached = SIDE_QUAD_CACHE.computeIfAbsent(configHash, key -> new BakedQuad[6]);
            int index = side.get3DDataValue();
            if (cached[index] == null) {
                cached[index] = new RetexturedBakedQuad(baseQuad, getConfigTexture(sideConfig[index]));
            }
            quads.add(cached[index]);
        }
        ChemicalStack chemical = modelData.get(CHEMICAL);
        if (chemical != null && !chemical.isEmpty() && quads.size() > 1) {
            BakedQuad underlay = quads.get(1);
            quads.offerFirst(new RetexturedBakedQuad(RenderHelper.mulColor(underlay, chemical.getChemicalTint()), RenderHelper.getTexture(chemical.getChemical().getIcon())));
        }
        return quads;
    }

    private TextureAtlasSprite getConfigTexture(byte side) {

        return switch (side) {
            case 1 -> CELL_CONFIG_INPUT;
            case 2 -> CELL_CONFIG_OUTPUT;
            case 3 -> CELL_CONFIG_BOTH;
            default -> CELL_CONFIG_NONE;
        };
    }

}
