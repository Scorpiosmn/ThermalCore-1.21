package cofh.thermal.lib.common.item;

import cofh.core.common.item.EnergyContainerItem;
import cofh.core.common.item.IAugmentableItem;
import cofh.core.util.helpers.AugmentDataHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntSupplier;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentMax;
import static cofh.lib.api.ContainerType.ENERGY;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.lib.util.ThermalAugmentRules.setAttributeFromAugmentMultiply;

public abstract class EnergyContainerItemAugmentable extends EnergyContainerItem implements IAugmentableItem {

    protected IntSupplier numSlots = () -> 0;
    protected BiPredicate<ItemStack, List<ItemStack>> augValidator = (e, f) -> true;

    public EnergyContainerItemAugmentable(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);
    }

    public EnergyContainerItemAugmentable setNumSlots(IntSupplier numSlots) {

        this.numSlots = numSlots;
        return this;
    }

    public EnergyContainerItemAugmentable setAugValidator(BiPredicate<ItemStack, List<ItemStack>> augValidator) {

        this.augValidator = augValidator;
        return this;
    }

    public void setEnergyPerUse(int energyPerUse) {
        // Do nothing by default.
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {

        return Math.round(super.getEnchantmentValue(stack) * getBaseMod(stack));
    }

    protected float getBaseMod(ItemStack stack) {

        return getPropertyWithDefault(stack, TAG_AUGMENT_BASE_MOD, 1.0F);
    }

    protected void setAttributesFromAugment(CompoundTag properties, CompoundTag augmentData) {

        setAttributeFromAugmentMultiply(properties, augmentData, TAG_AUGMENT_BASE_MOD);
        setAttributeFromAugmentMultiply(properties, augmentData, TAG_AUGMENT_RF_STORAGE);
        setAttributeFromAugmentMultiply(properties, augmentData, TAG_AUGMENT_RF_XFER);
        setAttributeFromAugmentMax(properties, augmentData, TAG_AUGMENT_RF_CREATIVE);
    }

    // region IEnergyContainerItem
    @Override
    public int getExtract(ItemStack container) {

        float base = getPropertyWithDefault(container, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(container, TAG_AUGMENT_RF_XFER, 1.0F);
        return Math.round(extract * mod * base);
    }

    @Override
    public int getReceive(ItemStack container) {

        float base = getPropertyWithDefault(container, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(container, TAG_AUGMENT_RF_XFER, 1.0F);
        return Math.round(receive * mod * base);
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {

        float base = getPropertyWithDefault(container, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(container, TAG_AUGMENT_RF_STORAGE, 1.0F);
        return getMaxStored(container, Math.round(maxEnergy * mod * base));
    }
    // endregion

    // region IAugmentableItem
    @Override
    public int getAugmentSlots(ItemStack augmentable) {

        return numSlots.getAsInt();
    }

    @Override
    public boolean validAugment(ItemStack augmentable, ItemStack augment, List<ItemStack> augments) {

        return augValidator.test(augment, augments);
    }

    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        CompoundTag nbt = container.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag properties = new CompoundTag();
        for (ItemStack augment : augments) {
            CompoundTag augmentData = AugmentDataHelper.getAugmentData(augment);
            if (augmentData == null) {
                continue;
            }
            setAttributesFromAugment(properties, augmentData);
        }
        nbt.put(TAG_PROPERTIES, properties);
        container.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        if (isCreative(container, ENERGY)) {
            setEnergyStored(container, getMaxEnergyStored(container));
        } else {
            int energyExcess = getEnergyStored(container) - getMaxEnergyStored(container);
            if (energyExcess > 0) {
                setEnergyStored(container, getMaxEnergyStored(container));
            }
        }
    }
    // endregion
}
