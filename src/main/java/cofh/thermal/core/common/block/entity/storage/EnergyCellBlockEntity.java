package cofh.thermal.core.common.block.entity.storage;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.AugmentDataHelper;
import cofh.core.util.helpers.EnergyHelper;
import cofh.lib.api.block.entity.ITickableTile;
import cofh.lib.common.energy.EnergyHandlerRestrictionWrapper;
import cofh.lib.common.energy.EnergyStorageRestrictable;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.BlockHelper;
import cofh.thermal.core.common.inventory.storage.EnergyCellMenu;
import cofh.thermal.lib.common.block.entity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

import static cofh.core.client.renderer.model.ModelUtils.*;
import static cofh.thermal.core.common.config.ThermalCoreConfig.storageAugments;
import static cofh.thermal.core.init.registries.TCoreBlockEntities.ENERGY_CELL_TILE;
import static cofh.thermal.lib.util.ThermalAugmentRules.ENERGY_STORAGE_VALIDATOR;

public class EnergyCellBlockEntity extends StorageCellBlockEntity implements ITickableTile.IServerTickable {

    public static final int BASE_CAPACITY = 1000000;
    public static final int BASE_RECV = 1000;
    public static final int BASE_SEND = 1000;

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {

        super(ENERGY_CELL_TILE.get(), pos, state);

        energyStorage = new EnergyStorageRestrictable(BASE_CAPACITY, BASE_RECV, BASE_SEND).setTransferLimits(() -> amountInput, () -> amountOutput);

        amountInput = energyStorage.getMaxReceive();
        amountOutput = energyStorage.getMaxExtract();

        transferControl.initControl(false, true);

        addAugmentSlots(storageAugments);
        initHandlers();
    }

    //    @Override
    //    public void neighborChanged(Block blockIn, BlockPos fromPos) {
    //
    //        super.neighborChanged(blockIn, fromPos);
    //
    //        // TODO: Handle caching of neighbor caps.
    //    }

    @Override
    public void tickServer() {

        if (redstoneControl.getState()) {
            transferOut();
            transferIn();
        }
        if (Utils.timeCheck()) {
            updateTrackers(true);
        }
    }

    @Override
    public int getLightValue() {

        return Math.min(levelTracker, 8);
    }

    protected void transferIn() {

        if (!transferControl.getTransferIn()) {
            return;
        }
        if (amountInput <= 0 || energyStorage.isFull()) {
            return;
        }
        for (int i = inputTracker; i < 6 && energyStorage.getSpace() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isInput()) {
                attemptTransferIn(Direction.from3DDataValue(i));
            }
        }
        for (int i = 0; i < inputTracker && energyStorage.getSpace() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isInput()) {
                attemptTransferIn(Direction.from3DDataValue(i));
            }
        }
        ++inputTracker;
        inputTracker %= 6;
    }

    protected void transferOut() {

        if (!transferControl.getTransferOut()) {
            return;
        }
        if (amountOutput <= 0 || energyStorage.isEmpty()) {
            return;
        }
        for (int i = outputTracker; i < 6 && energyStorage.getEnergyStored() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isOutput()) {
                attemptTransferOut(Direction.from3DDataValue(i));
            }
        }
        for (int i = 0; i < outputTracker && energyStorage.getEnergyStored() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isOutput()) {
                attemptTransferOut(Direction.from3DDataValue(i));
            }
        }
        ++outputTracker;
        outputTracker %= 6;
    }

    protected void attemptTransferIn(Direction side) {

        BlockEntity adjTile = BlockHelper.getAdjacentTileEntity(this, side);
        if (adjTile != null) {
            var handler = EnergyHelper.getEnergyHandlerCap(adjTile, side.getOpposite());
            if (handler != null && handler.canExtract()) {
                int maxTransfer = Math.min(amountInput, energyStorage.getSpace());
                energyStorage.modify(handler.extractEnergy(maxTransfer, false));
            }
        }
    }

    protected void attemptTransferOut(Direction side) {

        BlockEntity adjTile = BlockHelper.getAdjacentTileEntity(this, side);
        if (adjTile != null) {
            var handler = EnergyHelper.getEnergyHandlerCap(adjTile, side.getOpposite());
            if (handler != null && handler.canReceive()) {
                int maxTransfer = Math.min(amountOutput, energyStorage.getEnergyStored());
                energyStorage.modify(-handler.receiveEnergy(maxTransfer, false));
            }
        }
    }

    @Override
    protected boolean keepEnergy() {

        return true;
    }

    @Override
    public int getMaxInput() {

        return energyStorage.getMaxReceive();
    }

    @Override
    public int getMaxOutput() {

        return energyStorage.getMaxExtract();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new EnergyCellMenu(i, level, worldPosition, inventory, player);
    }

    @Nonnull
    @Override
    public ModelData getModelData() {

        return ModelData.builder()
                .with(SIDES, reconfigControl().getRawSideConfig())
                .with(FACING, reconfigControl.getFacing())
                .with(LEVEL, levelTracker)
                .build();
    }

    @Override
    protected void updateTrackers(boolean send) {

        int curScale = energyStorage.getEnergyStored() > 0 ? 1 + (int) (energyStorage.getRatio() * 14) : 0;
        if (curScale != compareTracker) {
            compareTracker = curScale;
            if (send) {
                setChanged();
            }
        }
        if (energyStorage.isCreative()) {
            curScale = 9;
        } else {
            curScale = energyStorage.getEnergyStored() > 0 ? 1 + Math.min((int) (energyStorage.getRatio() * 8), 7) : 0;
        }
        if (levelTracker != curScale) {
            levelTracker = curScale;
            if (send) {
                TileStatePacket.sendToClient(this);
            }
        }
    }

    // region AUGMENTS
    @Override
    protected Predicate<ItemStack> augValidator() {

        return item -> AugmentDataHelper.hasAugmentData(item) && ENERGY_STORAGE_VALIDATOR.test(item, getAugmentsAsList());
    }
    // endregion

    // region CAPABILITIES
    protected IEnergyStorage inputEnergyCap = null;
    protected IEnergyStorage outputEnergyCap = null;

    @Override
    protected void updateHandlers() {

        energyCap = energyStorage;
        inputEnergyCap = new EnergyHandlerRestrictionWrapper(energyStorage, true, false);
        outputEnergyCap = new EnergyHandlerRestrictionWrapper(energyStorage, false, true);
    }

    @Override
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {

        if (side == null) {
            return energyCap;
        }
        return switch (reconfigControl.getSideConfig(side)) {
            case SIDE_NONE -> null;
            case SIDE_INPUT -> inputEnergyCap;
            case SIDE_OUTPUT -> outputEnergyCap;
            default -> super.getEnergyCapability(side);
        };
    }
    // endregion
}
