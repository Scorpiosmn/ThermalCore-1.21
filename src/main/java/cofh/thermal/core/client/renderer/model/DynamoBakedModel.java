package cofh.thermal.core.client.renderer.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Adjust this when Dynamos have more model needs
public class DynamoBakedModel extends UnderlayBakedModel implements IDynamicBakedModel {

    private static final Map<Integer, BakedQuad[]> COIL_QUAD_CACHE = new ConcurrentHashMap<>();

    private static final Map<Integer, BakedQuad[]> ITEM_QUAD_CACHE = new ConcurrentHashMap<>();
    private static final Map<List<Integer>, BakedModel> MODEL_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {

        COIL_QUAD_CACHE.clear();

        ITEM_QUAD_CACHE.clear();
        MODEL_CACHE.clear();
    }

    public DynamoBakedModel(BakedModel originalModel) {

        super(originalModel);
    }

    // TODO: More coil types; block and item quad creation.
}
