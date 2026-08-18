package cofh.thermal.core.init.data.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;

/** Raw JSON provider for optional chemical-cell data; no Mekanism classes are loaded. */
public class TCoreChemicalCompatDataProvider implements DataProvider {
    private final PackOutput.PathProvider loot, recipes;
    public TCoreChemicalCompatDataProvider(PackOutput output) { loot = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table"); recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe"); }
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        tasks.add(DataProvider.saveStable(cache, dropTable(), loot.json(ResourceLocation.fromNamespaceAndPath(ID_THERMAL, "blocks/chemical_cell"))));
        tasks.add(DataProvider.saveStable(cache, recipe(), recipes.json(ResourceLocation.fromNamespaceAndPath(ID_THERMAL, "chemical_cell"))));
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }
    public String getName() { return "Thermal Core Chemical Compat Data"; }
    private static JsonObject dropTable() {
        JsonObject f = new JsonObject(); f.addProperty("function", "cofh_core:nbt_sync"); JsonArray fs = new JsonArray(); fs.add(f); JsonObject e = new JsonObject(); e.addProperty("type", "minecraft:item"); e.add("functions", fs); e.addProperty("name", ID_THERMAL + ":chemical_cell");
        JsonObject c = new JsonObject(); c.addProperty("condition", "minecraft:survives_explosion"); JsonArray cs = new JsonArray(); cs.add(c); JsonArray es = new JsonArray(); es.add(e); JsonObject p = new JsonObject(); p.addProperty("bonus_rolls", 0.0); p.add("conditions", cs); p.add("entries", es); p.addProperty("rolls", 1.0); JsonArray ps = new JsonArray(); ps.add(p); JsonObject t = new JsonObject(); t.addProperty("type", "minecraft:block"); t.add("pools", ps); t.addProperty("random_sequence", ID_THERMAL + ":blocks/chemical_cell"); return t;
    }
    private static JsonObject recipe() {
        JsonArray cs = new JsonArray(); for (String id : new String[]{"mekanism", "thermal_dynamics"}) { JsonObject c = new JsonObject(); c.addProperty("type", "neoforge:mod_loaded"); c.addProperty("modid", id); cs.add(c); }
        JsonObject key = new JsonObject(); key.add("C", item(ID_THERMAL + ":chemical_duct")); key.add("I", tag("c:ingots/iron")); key.add("P", item(ID_THERMAL + ":redstone_servo")); key.add("R", item(ID_THERMAL + ":cured_rubber")); key.add("X", tag(ID_THERMAL + ":glass/hardened")); JsonArray p = new JsonArray(); p.add("RXR"); p.add("ICI"); p.add("RPR"); JsonObject result = new JsonObject(); result.addProperty("count", 1); result.addProperty("id", ID_THERMAL + ":chemical_cell"); JsonObject r = new JsonObject(); r.add("neoforge:conditions", cs); r.addProperty("type", "minecraft:crafting_shaped"); r.addProperty("category", "misc"); r.add("key", key); r.add("pattern", p); r.add("result", result); return r;
    }
    private static JsonObject tag(String id) { JsonObject o = new JsonObject(); o.addProperty("tag", id); return o; }
    private static JsonObject item(String id) { JsonObject o = new JsonObject(); o.addProperty("item", id); return o; }
}
