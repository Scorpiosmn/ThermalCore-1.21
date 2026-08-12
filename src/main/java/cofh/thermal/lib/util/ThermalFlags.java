package cofh.thermal.lib.util;

import static cofh.lib.util.FlagManager.getFlag;
import static cofh.lib.util.FlagManager.setFlag;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;

public class ThermalFlags {

    private ThermalFlags() {

    }

    public static String FLAG_BEEKEEPER_ARMOR = ID_THERMAL + ":beekeeper_armor";
    public static String FLAG_DIVING_ARMOR = ID_THERMAL + ":diving_armor";
    public static String FLAG_HAZMAT_ARMOR = ID_THERMAL + ":hazmat_armor";

    public static String FLAG_AREA_AUGMENTS = ID_THERMAL + ":area_augments";
    public static String FLAG_CREATIVE_AUGMENTS = ID_THERMAL + ":creative_augments";
    public static String FLAG_DYNAMO_AUGMENTS = ID_THERMAL + ":dynamo_augments";
    public static String FLAG_FILTER_AUGMENTS = ID_THERMAL + ":filter_augments";
    public static String FLAG_MACHINE_AUGMENTS = ID_THERMAL + ":machine_augments";
    public static String FLAG_POTION_AUGMENTS = ID_THERMAL + ":potion_augments";
    public static String FLAG_STORAGE_AUGMENTS = ID_THERMAL + ":storage_augments";
    public static String FLAG_UPGRADE_AUGMENTS = ID_THERMAL + ":upgrade_augments";

    public static String FLAG_CREATIVE_STORAGE_AUGMENTS = ID_THERMAL + ":creative_storage_augments";
    public static String FLAG_CREATIVE_MACHINE_AUGMENTS = ID_THERMAL + ":creative_machine_augments";

    public static String FLAG_RS_CONTROL_AUGMENT = ID_THERMAL + ":rs_control_augment";
    public static String FLAG_SIDE_CONFIG_AUGMENT = ID_THERMAL + ":side_config_augment";
    public static String FLAG_XP_STORAGE_AUGMENT = ID_THERMAL + ":xp_storage_augment";

    public static String FLAG_TOOL_COMPONENTS = ID_THERMAL + ":tool_components";

    public static String FLAG_COINS = ID_THERMAL + ":coins";
    public static String FLAG_PLATES = ID_THERMAL + ":plates";

    public static String FLAG_BASIC_EXPLOSIVES = ID_THERMAL + ":basic_explosives";
    public static String FLAG_PHYTOGRO_EXPLOSIVES = ID_THERMAL + ":phytogro_explosives";
    public static String FLAG_ELEMENTAL_EXPLOSIVES = ID_THERMAL + ":elemental_explosives";
    public static String FLAG_NUCLEAR_EXPLOSIVES = ID_THERMAL + ":nuclear_explosives";

    public static String FLAG_RESOURCE_APATITE = ID_THERMAL + ":apatite";
    public static String FLAG_RESOURCE_CINNABAR = ID_THERMAL + ":cinnabar";
    public static String FLAG_RESOURCE_NITER = ID_THERMAL + ":niter";
    public static String FLAG_RESOURCE_SULFUR = ID_THERMAL + ":sulfur";

    public static String FLAG_RESOURCE_TIN = ID_THERMAL + ":tin";
    public static String FLAG_RESOURCE_LEAD = ID_THERMAL + ":lead";
    public static String FLAG_RESOURCE_SILVER = ID_THERMAL + ":silver";
    public static String FLAG_RESOURCE_NICKEL = ID_THERMAL + ":nickel";

    public static String FLAG_RESOURCE_ALUMINUM = ID_THERMAL + ":aluminum";
    public static String FLAG_RESOURCE_URANIUM = ID_THERMAL + ":uranium";

    public static String FLAG_RESOURCE_RUBY = ID_THERMAL + ":ruby";
    public static String FLAG_RESOURCE_SAPPHIRE = ID_THERMAL + ":sapphire";

    public static String FLAG_RESOURCE_OIL = ID_THERMAL + ":oil";

    public static String FLAG_RESOURCE_RUBBERWOOD = ID_THERMAL + ":rubberwood";

    public static String FLAG_RESOURCE_BRONZE = ID_THERMAL + ":bronze";
    public static String FLAG_RESOURCE_ELECTRUM = ID_THERMAL + ":electrum";
    public static String FLAG_RESOURCE_INVAR = ID_THERMAL + ":invar";
    public static String FLAG_RESOURCE_CONSTANTAN = ID_THERMAL + ":constantan";

    public static String FLAG_RESOURCE_STEEL = ID_THERMAL + ":steel";
    public static String FLAG_RESOURCE_ROSE_GOLD = ID_THERMAL + ":rose_gold";

    public static String FLAG_MOB_BASALZ = ID_THERMAL + ":basalz";
    public static String FLAG_MOB_BLITZ = ID_THERMAL + ":blitz";
    public static String FLAG_MOB_BLIZZ = ID_THERMAL + ":blizz";

    static {
        setFlag(FLAG_RESOURCE_BRONZE, getFlag(FLAG_RESOURCE_TIN));
        setFlag(FLAG_RESOURCE_ELECTRUM, getFlag(FLAG_RESOURCE_SILVER));
        setFlag(FLAG_RESOURCE_INVAR, getFlag(FLAG_RESOURCE_NICKEL));
        setFlag(FLAG_RESOURCE_CONSTANTAN, getFlag(FLAG_RESOURCE_NICKEL));

        setFlag(FLAG_CREATIVE_STORAGE_AUGMENTS, () -> getFlag(FLAG_STORAGE_AUGMENTS).get() && getFlag(FLAG_CREATIVE_AUGMENTS).get());
        setFlag(FLAG_CREATIVE_MACHINE_AUGMENTS, () -> getFlag(FLAG_MACHINE_AUGMENTS).get() && getFlag(FLAG_CREATIVE_AUGMENTS).get());
    }

}
