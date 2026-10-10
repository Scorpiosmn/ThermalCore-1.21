package cofh.thermal.core.common.item;

import cofh.core.common.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.filter.EmptyFilter;
import cofh.core.util.filter.FilterRegistry;
import cofh.core.util.filter.IFilter;
import cofh.core.util.filter.IFilterableItem;
import cofh.core.util.helpers.FilterHelper;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.api.item.IEnergyContainerItem;
import cofh.lib.util.Utils;
import cofh.lib.util.raytracer.RayTracer;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.lib.common.item.EnergyContainerItemAugmentable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentAdd;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentString;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.core.init.registries.TCoreSounds.SOUND_MAGNET;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;

/**
 * FluxoMagnet - ported from Thermal Innovation 11.0.1 (NeoForge 1.20.4) to ThermalCore 11.0.7 (NeoForge 1.21.1).
 *
 * NOTE: the item is registered in the "thermal" namespace as thermal:flux_magnet, which is what the original
 * Thermal Innovation build did as well (see TInoItems#registerTools in the 1.20.4 sources). Keep the ID as-is so
 * existing recipes / scripts keep working.
 */
public class FluxMagnetItem extends EnergyContainerItemAugmentable implements IColorableItem, IFilterableItem, IMultiModeItem, IEnergyContainerItem {

    protected static final int MAP_CAPACITY = 128;
    protected static final WeakHashMap<ItemStack, IFilter> FILTERS = new WeakHashMap<>(MAP_CAPACITY);

    protected static final int RADIUS = 4;
    protected static final int REACH = 64;

    protected static final int MIN_AGE = 20;

    protected int energyPerItem = 25;
    protected int energyPerUse = 200;

    protected boolean obeyPickupDelay = true;

    public FluxMagnetItem(Properties builder, int maxEnergy, int maxTransfer) {

        super(builder, maxEnergy, maxTransfer);

        // 1.21.1: custom model predicates are registered the same way, but ResourceLocation construction changed
        // and "dyed" is now a data component instead of DyeableLeatherItem#hasCustomColor.
        ProxyUtils.registerItemModelProperty(this, ResourceLocation.withDefaultNamespace("color"), (stack, world, entity, seed) -> (stack.has(DataComponents.DYED_COLOR) ? 1.0F : 0));
        ProxyUtils.registerItemModelProperty(this, ResourceLocation.withDefaultNamespace("state"), (stack, world, entity, seed) -> (getEnergyStored(stack) > 0 ? 0.5F : 0) + (getMode(stack) > 0 ? 0.25F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.toolAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_RF, TAG_AUGMENT_TYPE_AREA_EFFECT, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    public void setEnergyPerUse(int energyPerUse) {

        this.energyPerUse = energyPerUse;
    }

    public void setEnergyPerItem(int energyPerItem) {

        this.energyPerItem = energyPerItem;
    }

    public void setObeyItemPickupDelay(boolean obeyPickupDelay) {

        this.obeyPickupDelay = obeyPickupDelay;
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.magnet.use").withStyle(ChatFormatting.GRAY));
        if (FilterHelper.hasFilter(stack)) {
            tooltip.add(getTextComponent("info.thermal.magnet.use.sneak").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(getTextComponent("info.thermal.magnet.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn, handIn) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        return useDelegate(stack, context.getPlayer(), context.getHand()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

        if (!Utils.timeCheckQuarter()) {
            return;
        }
        if (Utils.isClientWorld(worldIn) || Utils.isFakePlayer(entityIn) || getMode(stack) <= 0 || !(entityIn instanceof Player player)) {
            return;
        }
        if (getEnergyStored(stack) < energyPerItem && !player.getAbilities().instabuild) {
            return;
        }
        int radius = getRadius(stack);
        int radSq = radius * radius;

        AABB area = AABB.encapsulatingFullBlocks(player.blockPosition().offset(-radius, -radius, -radius), player.blockPosition().offset(1 + radius, 1 + radius, 1 + radius));
        List<ItemEntity> items = worldIn.getEntitiesOfClass(ItemEntity.class, area, EntitySelector.ENTITY_STILL_ALIVE);

        Predicate<ItemStack> filterRules = getFilter(stack).getItemRules();
        int itemCount = 0;
        for (ItemEntity item : items) {
            if (obeyPickupDelay && item.hasPickUpDelay() || item.getPersistentData().getBoolean(TAG_CONVEYOR_COMPAT) || !filterRules.test(item.getItem())) {
                continue;
            }
            if (item.getOwner() == null || !obeyPickupDelay || item.getAge() >= MIN_AGE) {
                if (item.position().distanceToSqr(player.position()) <= radSq) {
                    item.setPos(player.getX(), player.getY(), player.getZ());
                    item.setPickUpDelay(0);
                    ++itemCount;
                }
            }
        }
        if (!player.getAbilities().instabuild) {
            extractEnergy(stack, energyPerItem * itemCount, false);
        }
    }

    // region HELPERS
    protected boolean useDelegate(ItemStack stack, Player player, InteractionHand hand) {

        if (Utils.isFakePlayer(player)) {
            return false;
        }
        if (player.isSecondaryUseActive() && hand == InteractionHand.MAIN_HAND) {
            return player instanceof ServerPlayer && openFilterGui((ServerPlayer) player, stack);
        } else if (getEnergyStored(stack) >= energyPerUse || player.getAbilities().instabuild) {
            BlockHitResult traceResult = RayTracer.retrace(player, REACH);
            if (traceResult.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            int radius = getRadius(stack);
            int radSq = radius * radius;

            Level world = player.getCommandSenderWorld();
            BlockPos pos = traceResult.getBlockPos();

            AABB area = AABB.encapsulatingFullBlocks(pos.offset(-radius, -radius, -radius), pos.offset(1 + radius, 1 + radius, 1 + radius));
            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, area, EntitySelector.ENTITY_STILL_ALIVE);

            Predicate<ItemStack> filterRules = getFilter(stack).getItemRules();
            int itemCount = 0;
            for (ItemEntity item : items) {
                if (obeyPickupDelay && item.hasPickUpDelay() || item.getPersistentData().getBoolean(TAG_CONVEYOR_COMPAT) || !filterRules.test(item.getItem())) {
                    continue;
                }
                if (item.position().distanceToSqr(traceResult.getLocation()) <= radSq) {
                    item.setPos(player.getX(), player.getY(), player.getZ());
                    item.setPickUpDelay(0);
                    ++itemCount;
                }
            }
            if (!player.getAbilities().instabuild && itemCount > 0) {
                extractEnergy(stack, energyPerUse + energyPerItem * itemCount, false);
            }
            player.swing(hand);
            stack.setPopTime(5);
            player.level.playSound(null, player.blockPosition(), SOUND_MAGNET.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        return true;
    }

    protected int getRadius(ItemStack stack) {

        float base = getPropertyWithDefault(stack, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(stack, TAG_AUGMENT_RADIUS, 1.0F);
        return Math.round(RADIUS + mod * base);
    }

    @Override
    protected void setAttributesFromAugment(CompoundTag properties, CompoundTag augmentData) {

        setAttributeFromAugmentAdd(properties, augmentData, TAG_AUGMENT_RADIUS);
        setAttributeFromAugmentString(properties, augmentData, TAG_FILTER_TYPE);

        super.setAttributesFromAugment(properties, augmentData);
    }
    // endregion

    // region IColorableItem
    @Override
    public int getColor(ItemStack item, int colorIndex) {

        return colorIndex == 0 ? DyedItemColor.getOrDefault(item, 0xFFFFFFFF) : 0xFFFFFFFF;
    }
    // endregion

    // region IAugmentableItem
    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        super.updateAugmentState(container, augments);

        if (!FilterHelper.hasFilter(container)) {
            CustomData.update(DataComponents.CUSTOM_DATA, container, tag -> tag.remove(TAG_FILTER));
        }
        FILTERS.remove(container);
    }
    // endregion

    // region IFilterable
    @Override
    public IFilter getFilter(ItemStack stack) {

        String filterType = FilterHelper.getFilterType(stack);
        if (filterType.isEmpty()) {
            return EmptyFilter.INSTANCE;
        }
        IFilter ret = FILTERS.get(stack);
        if (ret != null) {
            return ret;
        }
        if (FILTERS.size() > MAP_CAPACITY) {
            FILTERS.clear();
        }
        FILTERS.put(stack, FilterRegistry.getFilter(filterType, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()));
        return FILTERS.get(stack);
    }

    @Override
    public void onFilterChanged(ItemStack stack) {

        FILTERS.remove(stack);
    }
    // endregion

    // region IMultiModeItem
    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SOUND_MAGNET.get(), SoundSource.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
        ProxyUtils.setOverlayMessage(player, Component.translatable("info.thermal.magnet.mode." + getMode(stack)));
    }
    // endregion
}
