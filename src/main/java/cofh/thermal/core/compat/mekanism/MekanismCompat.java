package cofh.thermal.core.compat.mekanism;

import cofh.core.util.filter.EmptyFilter;
import cofh.thermal.core.common.block.entity.device.DeviceNullifierBlockEntity;
import cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity;
import cofh.thermal.core.compat.mekanism.item.ChemicalCellBlockItem;
import cofh.thermal.core.compat.mekanism.inventory.ChemicalCellMenu;
import cofh.thermal.lib.common.block.StorageCellBlock;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.Utils.itemProperties;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.BLOCK_ENTITIES;
import static cofh.thermal.core.ThermalCore.CONTAINERS;
import static cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity.AUG_VALIDATOR;
import static cofh.thermal.core.init.registries.TCoreBlockEntities.DEVICE_NULLIFIER_TILE;
import static cofh.thermal.core.init.registries.ThermalCreativeTabs.devicesTab;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.lib.util.ThermalIDs.ID_CHEMICAL_CELL;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.of;

/**
 * Optional Mekanism integration. This class must only be loaded after the Mekanism mod-presence check.
 */
public final class MekanismCompat {

    private static final long CAPACITY = BUCKET_VOLUME * 64L;
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER = BlockCapability.createSided(fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalCellBlockEntity>> CHEMICAL_CELL_TILE = BLOCK_ENTITIES.register(ID_CHEMICAL_CELL,
            () -> BlockEntityType.Builder.of(ChemicalCellBlockEntity::new, BLOCKS.get(ID_CHEMICAL_CELL)).build(null));
    public static final DeferredHolder<Item, Item> CHEMICAL_CELL = devicesTab(40, registerBlock(ID_CHEMICAL_CELL,
            () -> new StorageCellBlock(of().sound(SoundType.LANTERN).strength(2.0F).noOcclusion(), ChemicalCellBlockEntity.class, CHEMICAL_CELL_TILE::get),
            () -> new ChemicalCellBlockItem(BLOCKS.get(ID_CHEMICAL_CELL), itemProperties()).setNumSlots(() -> cofh.thermal.core.common.config.ThermalCoreConfig.storageAugments).setAugValidator(AUG_VALIDATOR)));
    public static final DeferredHolder<MenuType<?>, MenuType<ChemicalCellMenu>> CHEMICAL_CELL_CONTAINER = CONTAINERS.register(ID_CHEMICAL_CELL,
            () -> IMenuTypeExtension.create((id, inventory, data) -> new ChemicalCellMenu(id, cofh.core.util.ProxyUtils.getClientWorld(), data.readBlockPos(), inventory, cofh.core.util.ProxyUtils.getClientPlayer())));

    private MekanismCompat() {

    }

    /**
     * Intentionally empty: invoking this triggers static initialization of this class, which performs
     * the DeferredRegister registrations above. Do not remove - see the ThermalCore constructor call site.
     */
    public static void register() {

    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        event.registerBlockEntity(CHEMICAL_HANDLER, DEVICE_NULLIFIER_TILE.get(), (tile, side) -> {
            DeviceNullifierBlockEntity host = (DeviceNullifierBlockEntity) tile;
            Object[] slot = host.compatCapSlot();
            IChemicalHandler handler;
            if (slot[0] instanceof IChemicalHandler cached) {
                handler = cached;
            } else {
                handler = new NullChemicalHandler(host);
                slot[0] = handler;
            }
            return handler;
        });
        event.registerBlockEntity(CHEMICAL_HANDLER, CHEMICAL_CELL_TILE.get(), (tile, side) -> ((ChemicalCellBlockEntity) tile).getChemicalHandlerCapability(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, CHEMICAL_CELL_TILE.get(),
                (tile, side) -> ((ChemicalCellBlockEntity) tile).getItemHandlerCapability(side));
    }

    private static final class NullChemicalHandler implements IChemicalHandler {

        private final DeviceNullifierBlockEntity host;

        private NullChemicalHandler(DeviceNullifierBlockEntity host) {

            this.host = host;
        }

        private boolean canVoid() {

            return host.isActive && host.getFilter() instanceof EmptyFilter;
        }

        @Override
        public int getChemicalTanks() {

            return 1;
        }

        @Override
        public ChemicalStack getChemicalInTank(int tank) {

            return ChemicalStack.EMPTY;
        }

        @Override
        public void setChemicalInTank(int tank, ChemicalStack stack) {

            // Nullifier contents are never stored.
        }

        @Override
        public long getChemicalTankCapacity(int tank) {

            return tank == 0 ? CAPACITY : 0;
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {

            return tank == 0 && !stack.isEmpty() && canVoid();
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {

            if (tank != 0 || stack.isEmpty() || !canVoid()) {
                return stack;
            }
            long accepted = Math.min(stack.getAmount(), CAPACITY);
            return accepted == stack.getAmount() ? ChemicalStack.EMPTY : stack.copyWithAmount(stack.getAmount() - accepted);
        }

        @Override
        public ChemicalStack extractChemical(int tank, long amount, Action action) {

            return ChemicalStack.EMPTY;
        }
    }

}
