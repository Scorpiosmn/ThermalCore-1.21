package cofh.thermal.core.compat.mekanism.block.entity;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.AugmentDataHelper;
import cofh.lib.api.block.entity.ITickableTile;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.core.compat.mekanism.inventory.ChemicalCellMenu;
import cofh.thermal.lib.common.block.entity.StorageCellBlockEntity;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static cofh.core.client.renderer.model.ModelUtils.FACING;
import static cofh.core.client.renderer.model.ModelUtils.LEVEL;
import static cofh.core.client.renderer.model.ModelUtils.SIDES;
import static cofh.core.util.helpers.AugmentableHelper.getAttributeModWithDefault;
import static cofh.lib.api.StorageGroup.ACCESSIBLE;
import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_BASE_MOD;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_FLUID_STORAGE;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_TYPE_FLUID;
import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_TYPE_UPGRADE;
import static cofh.thermal.core.common.config.ThermalCoreConfig.storageAugments;
import static cofh.thermal.core.compat.mekanism.MekanismCompat.CHEMICAL_CELL_TILE;
import static cofh.thermal.core.compat.mekanism.MekanismCompat.CHEMICAL_HANDLER;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;

/**
 * Mekanism chemical counterpart to the Fluid Cell. This class is only loaded when Mekanism is present.
 */
public class ChemicalCellBlockEntity extends StorageCellBlockEntity implements ITickableTile.IServerTickable {

    public static final int BASE_CAPACITY = BUCKET_VOLUME * 20;
    public static final ModelProperty<ChemicalStack> CHEMICAL = new ModelProperty<>();
    public static final BiPredicate<ItemStack, List<ItemStack>> AUG_VALIDATOR = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID);

    public static final String TAG_CHEMICAL = "Chemical";

    private final ChemicalStorage chemicalStorage = new ChemicalStorage();
    private ChemicalStack renderChemical = ChemicalStack.EMPTY;

    private IChemicalHandler inputChemicalCap;
    private IChemicalHandler outputChemicalCap;

    public ChemicalCellBlockEntity(BlockPos pos, BlockState state) {

        super(CHEMICAL_CELL_TILE.get(), pos, state);

        amountInput = BUCKET_VOLUME;
        amountOutput = BUCKET_VOLUME;

        transferControl.initControl(false, true);

        addAugmentSlots(storageAugments);
        initHandlers();
    }

    @Override
    public void tickServer() {

        if (redstoneControl.getState()) {
            transferOut();
            transferIn();
        }
        if (Utils.timeCheck() || !ChemicalStack.isSameChemical(renderChemical, chemicalStorage.getChemical())) {
            updateTrackers(true);
        }
    }

    @Override
    public int getLightValue() {

        return 0;
    }

    private void transferIn() {

        if (!transferControl.getTransferIn() || amountInput <= 0 || chemicalStorage.getSpace() <= 0) {
            return;
        }
        for (int i = inputTracker; i < 6 && chemicalStorage.getSpace() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isInput()) {
                attemptTransferIn(Direction.from3DDataValue(i));
            }
        }
        for (int i = 0; i < inputTracker && chemicalStorage.getSpace() > 0; ++i) {
            if (reconfigControl.getSideConfig(i).isInput()) {
                attemptTransferIn(Direction.from3DDataValue(i));
            }
        }
        inputTracker = (inputTracker + 1) % 6;
    }

    private void transferOut() {

        if (!transferControl.getTransferOut() || amountOutput <= 0 || chemicalStorage.isEmpty()) {
            return;
        }
        for (int i = outputTracker; i < 6 && !chemicalStorage.isEmpty(); ++i) {
            if (reconfigControl.getSideConfig(i).isOutput()) {
                attemptTransferOut(Direction.from3DDataValue(i));
            }
        }
        for (int i = 0; i < outputTracker && !chemicalStorage.isEmpty(); ++i) {
            if (reconfigControl.getSideConfig(i).isOutput()) {
                attemptTransferOut(Direction.from3DDataValue(i));
            }
        }
        outputTracker = (outputTracker + 1) % 6;
    }

    private void attemptTransferIn(Direction side) {

        BlockEntity adjacent = BlockHelper.getAdjacentTileEntity(this, side);
        if (adjacent == null) {
            return;
        }
        IChemicalHandler handler = level.getCapability(CHEMICAL_HANDLER, adjacent.getBlockPos(), adjacent.getBlockState(), adjacent, side.getOpposite());
        if (handler == null) {
            return;
        }
        long remainingInput = Math.max(0L, Math.min((long) amountInput, chemicalStorage.getSpace()));
        for (int tank = 0; tank < handler.getChemicalTanks() && remainingInput > 0; ++tank) {
            long limit = Math.min(remainingInput, chemicalStorage.getSpace());
            if (limit <= 0) {
                break;
            }
            ChemicalStack simulated = handler.extractChemical(tank, limit, Action.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }
            ChemicalStack remainder = chemicalStorage.insertChemical(0, simulated, Action.EXECUTE);
            long filled = simulated.getAmount() - remainder.getAmount();
            if (filled <= 0) {
                continue;
            }
            ChemicalStack extracted = handler.extractChemical(tank, filled, Action.EXECUTE);
            long taken = ChemicalStack.isSameChemical(extracted, simulated)
                    ? Math.min(extracted.getAmount(), filled)
                    : 0;
            if (taken < filled) {
                chemicalStorage.extractChemical(0, filled - taken, Action.EXECUTE);
            }
            remainingInput = Math.max(0L, remainingInput - taken);
        }
    }

    private void attemptTransferOut(Direction side) {

        BlockEntity adjacent = BlockHelper.getAdjacentTileEntity(this, side);
        if (adjacent == null) {
            return;
        }
        IChemicalHandler handler = level.getCapability(CHEMICAL_HANDLER, adjacent.getBlockPos(), adjacent.getBlockState(), adjacent, side.getOpposite());
        if (handler == null) {
            return;
        }
        ChemicalStack offered = chemicalStorage.getChemical().copyWithAmount(Math.min(amountOutput, chemicalStorage.getAmount()));
        ChemicalStack remainder = handler.insertChemical(offered, Action.EXECUTE);
        long transferred = offered.getAmount() - remainder.getAmount();
        if (transferred > 0) {
            chemicalStorage.extractChemical(0, transferred, Action.EXECUTE);
        }
    }

    @Override
    public int getMaxInput() {

        return (int) Math.min(Integer.MAX_VALUE, chemicalStorage.getCapacity() / 4);
    }

    @Override
    public int getMaxOutput() {

        return (int) Math.min(Integer.MAX_VALUE, chemicalStorage.getCapacity() / 4);
    }

    public ChemicalStack getChemical() {

        return chemicalStorage.getChemical();
    }

    public long getChemicalCapacity() {

        return chemicalStorage.getCapacity();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new ChemicalCellMenu(id, level, worldPosition, inventory, player);
    }

    @Nonnull
    @Override
    public ModelData getModelData() {

        return ModelData.builder()
                .with(SIDES, reconfigControl().getRawSideConfig())
                .with(FACING, reconfigControl.getFacing())
                .with(LEVEL, levelTracker)
                .with(CHEMICAL, renderChemical)
                .build();
    }

    @Override
    protected void updateTrackers(boolean send) {

        ChemicalStack current = chemicalStorage.getChemical();
        boolean chemicalChanged = !ChemicalStack.isSameChemical(renderChemical, current);
        renderChemical = current.isEmpty() ? ChemicalStack.EMPTY : current.copy();

        long capacity = Math.max(1L, chemicalStorage.getCapacity());
        int comparator = current.isEmpty() ? 0
                : Math.clamp(1 + (int) ((double) current.getAmount() * 14.0D / capacity), 1, 15);
        if (comparator != compareTracker) {
            compareTracker = comparator;
            if (send) {
                setChanged();
            }
        }
        int level = current.isEmpty() ? 0
                : Math.clamp(1 + (int) ((double) current.getAmount() * 8.0D / capacity), 1, 8);
        if (level != levelTracker || chemicalChanged) {
            levelTracker = level;
            if (send) {
                TileStatePacket.sendToClient(this);
            }
        }
    }

    @Override
    protected Predicate<ItemStack> augValidator() {

        return item -> AugmentDataHelper.hasAugmentData(item) && AUG_VALIDATOR.test(item, getAugmentsAsList());
    }

    @Override
    protected void finalizeAttributes(net.minecraft.world.item.enchantment.ItemEnchantments enchantments) {

        boolean maxIn = amountInput == getMaxInput();
        boolean maxOut = amountOutput == getMaxOutput();

        float holding = getHoldingMod(enchantments);
        float base = getAttributeModWithDefault(augmentNBT, TAG_AUGMENT_BASE_MOD, 1.0F);
        float storage = holding * base * getAttributeModWithDefault(augmentNBT, TAG_AUGMENT_FLUID_STORAGE, 1.0F);
        chemicalStorage.setCapacity(Math.round(BASE_CAPACITY * storage));

        super.finalizeAttributes(enchantments);

        if (maxIn) {
            amountInput = getMaxInput();
        }
        if (maxOut) {
            amountOutput = getMaxOutput();
        }

        amountInput = MathHelper.clamp(amountInput, 0, getMaxInput());
        amountOutput = MathHelper.clamp(amountOutput, 0, getMaxOutput());
        updateTrackers(false);
    }

    public IChemicalHandler getChemicalHandlerCapability(@Nullable Direction side) {

        if (side == null) {
            return chemicalStorage;
        }
        return switch (reconfigControl.getSideConfig(side)) {
            case SIDE_NONE -> null;
            case SIDE_INPUT -> inputChemicalCap;
            case SIDE_OUTPUT -> outputChemicalCap;
            default -> chemicalStorage;
        };
    }

    @Override
    protected void updateHandlers() {

        inputChemicalCap = new RestrictedChemicalHandler(chemicalStorage, true, false);
        outputChemicalCap = new RestrictedChemicalHandler(chemicalStorage, false, true);
    }

    @Override
    public ItemStack createItemStackTag(ItemStack stack) {

        ItemStack result = super.createItemStackTag(stack);
        CompoundTag nbt = result.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        chemicalStorage.write(level == null ? Utils.BUILTIN_ACCESS : level.registryAccess(), nbt);
        BlockItem.setBlockEntityData(result, getType(), nbt);
        return result;
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {

        super.loadAdditional(nbt, provider);
        chemicalStorage.read(provider, nbt);
        updateTrackers(false);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {

        super.saveAdditional(nbt, provider);
        chemicalStorage.write(provider, nbt);
    }

    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        super.getGuiPacket(buffer);
        buffer.writeLong(chemicalStorage.getCapacity());
        ChemicalStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, chemicalStorage.getChemical());
        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        super.handleGuiPacket(buffer);
        chemicalStorage.setCapacity(buffer.readLong());
        chemicalStorage.setChemical(ChemicalStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
        updateTrackers(false);
    }

    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        super.getStatePacket(buffer);
        ChemicalStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, chemicalStorage.getChemical());
        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        super.handleStatePacket(buffer);
        chemicalStorage.setChemical(ChemicalStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
        updateTrackers(false);
        updateClientRender();
    }

    private final class ChemicalStorage implements IChemicalHandler {

        private long capacity = BASE_CAPACITY;
        private ChemicalStack chemical = ChemicalStack.EMPTY;

        private long getCapacity() {

            return capacity;
        }

        private long getAmount() {

            return chemical.getAmount();
        }

        private long getSpace() {

            return capacity - chemical.getAmount();
        }

        private boolean isEmpty() {

            return chemical.isEmpty();
        }

        private ChemicalStack getChemical() {

            return chemical;
        }

        private void setCapacity(long capacity) {

            this.capacity = Math.max(0, capacity);
            if (!chemical.isEmpty() && chemical.getAmount() > this.capacity) {
                chemical = chemical.copyWithAmount(this.capacity);
                onChemicalChanged();
            }
        }

        private void setChemical(ChemicalStack chemical) {

            this.chemical = chemical.isEmpty() ? ChemicalStack.EMPTY : chemical.copyWithAmount(Math.min(chemical.getAmount(), capacity));
        }

        private void read(HolderLookup.Provider provider, CompoundTag nbt) {
            if (nbt.contains("Capacity")) {
                setCapacity(nbt.getLong("Capacity"));
            }
            setChemical(ChemicalStack.parseOptional(provider, nbt.getCompound(TAG_CHEMICAL)));
        }

        private void write(HolderLookup.Provider provider, CompoundTag nbt) {
            nbt.putLong("Capacity", capacity);
            if (!chemical.isEmpty()) {
                nbt.put(TAG_CHEMICAL, chemical.saveOptional(provider));
            }
        }

        @Override
        public int getChemicalTanks() {

            return 1;
        }

        @Override
        public ChemicalStack getChemicalInTank(int tank) {

            return tank == 0 ? chemical.copy() : ChemicalStack.EMPTY;
        }

        @Override
        public void setChemicalInTank(int tank, ChemicalStack stack) {

            if (tank == 0) {
                setChemical(stack);
                onChemicalChanged();
            }
        }

        @Override
        public long getChemicalTankCapacity(int tank) {

            return tank == 0 ? capacity : 0;
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {

            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {

            if (tank != 0 || stack.isEmpty() || !chemical.isEmpty() && !ChemicalStack.isSameChemical(chemical, stack)) {
                return stack;
            }
            long accepted = Math.min(getSpace(), stack.getAmount());
            if (accepted <= 0) {
                return stack;
            }
            if (action.execute()) {
                chemical = chemical.isEmpty() ? stack.copyWithAmount(accepted) : chemical.copyWithAmount(chemical.getAmount() + accepted);
                onChemicalChanged();
            }
            return accepted == stack.getAmount() ? ChemicalStack.EMPTY : stack.copyWithAmount(stack.getAmount() - accepted);
        }

        @Override
        public ChemicalStack extractChemical(int tank, long amount, Action action) {

            if (tank != 0 || amount <= 0 || chemical.isEmpty()) {
                return ChemicalStack.EMPTY;
            }
            long extracted = Math.min(amount, chemical.getAmount());
            ChemicalStack result = chemical.copyWithAmount(extracted);
            if (action.execute()) {
                chemical = extracted == chemical.getAmount() ? ChemicalStack.EMPTY : chemical.copyWithAmount(chemical.getAmount() - extracted);
                onChemicalChanged();
            }
            return result;
        }
    }

    private static final class RestrictedChemicalHandler implements IChemicalHandler {

        private final IChemicalHandler wrapped;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private RestrictedChemicalHandler(IChemicalHandler wrapped, boolean allowInsert, boolean allowExtract) {

            this.wrapped = wrapped;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override public int getChemicalTanks() { return wrapped.getChemicalTanks(); }
        @Override public ChemicalStack getChemicalInTank(int tank) { return wrapped.getChemicalInTank(tank); }
        @Override public void setChemicalInTank(int tank, ChemicalStack stack) { wrapped.setChemicalInTank(tank, stack); }
        @Override public long getChemicalTankCapacity(int tank) { return wrapped.getChemicalTankCapacity(tank); }
        @Override public boolean isValid(int tank, ChemicalStack stack) { return allowInsert && wrapped.isValid(tank, stack); }
        @Override public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) { return allowInsert ? wrapped.insertChemical(tank, stack, action) : stack; }
        @Override public ChemicalStack extractChemical(int tank, long amount, Action action) { return allowExtract ? wrapped.extractChemical(tank, amount, action) : ChemicalStack.EMPTY; }
    }

    private void onChemicalChanged() {

        setChanged();
    }

}
