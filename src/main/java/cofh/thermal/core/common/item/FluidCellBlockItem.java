package cofh.thermal.core.common.item;

import cofh.core.util.helpers.AugmentDataHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.item.IFluidContainerItem;
import cofh.lib.common.fluid.FluidStorageCoFH;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermal.core.common.block.entity.storage.FluidCellBlockEntity;
import cofh.thermal.core.init.registries.TCoreBlockEntities;
import cofh.thermal.lib.common.item.BlockItemAugmentable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentMax;
import static cofh.core.util.helpers.FluidHelper.addPotionTooltip;
import static cofh.lib.api.ContainerType.FLUID;
import static cofh.lib.util.Utils.BUILTIN_ACCESS;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.*;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class FluidCellBlockItem extends BlockItemAugmentable implements IFluidContainerItem {

    public FluidCellBlockItem(Block blockIn, Properties builder) {

        super(blockIn, builder);

        setEnchantability(5);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(StringHelper.getFluidName(fluid));
        }
        tooltip.add(isCreative(stack, FLUID)
                ? getTextComponent("info.cofh.infinite").withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.ITALIC)
                : getTextComponent(localize("info.cofh.amount") + ": " + format(fluid.getAmount()) + " / " + format(getCapacity(stack)) + " " + localize("info.cofh.unit_mb")));

        if (FluidHelper.hasPotionTag(fluid)) {
            tooltip.add(getEmptyLine());
            tooltip.add(getTextComponent(localize("info.cofh.effects") + ":"));
            addPotionTooltip(fluid, tooltip);
        }
    }

    protected void setAttributesFromAugment(CompoundTag properties, CompoundTag augmentData) {

        setAttributeFromAugmentMax(properties, augmentData, TAG_AUGMENT_BASE_MOD);
        setAttributeFromAugmentMax(properties, augmentData, TAG_AUGMENT_FLUID_STORAGE);
        setAttributeFromAugmentMax(properties, augmentData, TAG_AUGMENT_FLUID_CREATIVE);
    }

    //    @Override
    //    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
    //
    //        return new FluidContainerItemWrapper(stack, this);
    //    }

    // region IFluidContainerItem
    @Override
    public CompoundTag getOrCreateTankTag(ItemStack container) {

        CompoundTag blockTag = container.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        ListTag tanks = blockTag.getList(TAG_TANK_INV, TAG_COMPOUND);
        if (tanks.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putByte(TAG_TANK, (byte) 0);
            new FluidStorageCoFH(FluidCellBlockEntity.BASE_CAPACITY).write(BUILTIN_ACCESS, tag);
            tanks.add(tag);
            blockTag.put(TAG_TANK_INV, tanks);
            setBlockEntityData(container, TCoreBlockEntities.FLUID_CELL_TILE.get(), blockTag);
        }
        return tanks.getCompound(0);
    }

    @Override
    public void saveTankTag(ItemStack container, CompoundTag tag) {

        CompoundTag blockTag = container.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        ListTag tanks = blockTag.getList(TAG_TANK_INV, TAG_COMPOUND);
        if (tanks.isEmpty()) {
            tanks.add(tag);
        } else {
            tanks.set(0, tag);
        }
        blockTag.put(TAG_TANK_INV, tanks);
        setBlockEntityData(container, TCoreBlockEntities.FLUID_CELL_TILE.get(), blockTag);
    }

    @Override
    public FluidStack getFluid(ItemStack container) {

        CompoundTag tag = getOrCreateTankTag(container);
        if (!tag.contains("id")) {
            return FluidStack.EMPTY;
        }
        return FluidStack.parseOptional(BUILTIN_ACCESS, tag);
    }

    @Override
    public int getCapacity(ItemStack container) {

        CompoundTag tag = getOrCreateTankTag(container);
        if (tag == null) {
            return 0;
        }
        float base = getPropertyWithDefault(container, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(container, TAG_AUGMENT_FLUID_STORAGE, 1.0F);
        return getMaxStored(container, Math.round(tag.getInt(TAG_CAPACITY) * mod * base));
    }

    @Override
    public int fill(ItemStack container, FluidStack resource, FluidAction action) {

        CompoundTag containerTag = getOrCreateTankTag(container);
        if (resource.isEmpty() || !isFluidValid(container, resource)) {
            return 0;
        }
        FluidStorageCoFH tank = new FluidStorageCoFH(FluidCellBlockEntity.BASE_CAPACITY).setCapacity(getCapacity(container)).read(BUILTIN_ACCESS, containerTag);
        if (isCreative(container, FLUID)) {
            if (action.execute()) {
                tank.setFluidStack(resource.copyWithAmount(tank.getCapacity()));
                tank.write(BUILTIN_ACCESS, containerTag);
                saveTankTag(container, containerTag);
            }
            return resource.getAmount();
        }
        int ret = tank.fill(resource, action);
        tank.write(BUILTIN_ACCESS, containerTag);
        if (action.execute()) {
            saveTankTag(container, containerTag);
        }
        return ret;
    }

    @Override
    public FluidStack drain(ItemStack container, int maxDrain, FluidAction action) {

        CompoundTag containerTag = getOrCreateTankTag(container);
        FluidStorageCoFH tank = new FluidStorageCoFH(FluidCellBlockEntity.BASE_CAPACITY).setCapacity(getCapacity(container)).read(BUILTIN_ACCESS, containerTag);
        if (isCreative(container, FLUID)) {
            return tank.getFluidStack().copyWithAmount(maxDrain);
        }
        FluidStack ret = tank.drain(maxDrain, action);
        tank.write(BUILTIN_ACCESS, containerTag);
        if (action.execute()) {
            saveTankTag(container, containerTag);
        }
        return ret;
    }
    // endregion

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        CompoundTag properties = new CompoundTag();
        for (ItemStack augment : augments) {
            CompoundTag augmentData = AugmentDataHelper.getAugmentData(augment);
            if (augmentData == null) {
                continue;
            }
            setAttributesFromAugment(properties, augmentData);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, container, tag -> tag.put(TAG_PROPERTIES, properties));
        int fluidExcess = getFluidAmount(container) - getCapacity(container);
        if (fluidExcess > 0) {
            drain(container, fluidExcess, EXECUTE);
        }
    }
    // endregion
}
