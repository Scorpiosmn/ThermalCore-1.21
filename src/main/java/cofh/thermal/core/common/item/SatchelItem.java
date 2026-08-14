package cofh.thermal.core.common.item;

import cofh.core.common.item.IMultiModeItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.filter.EmptyFilter;
import cofh.core.util.filter.FilterRegistry;
import cofh.core.util.filter.IFilter;
import cofh.core.util.filter.IFilterableItem;
import cofh.core.util.helpers.FilterHelper;
import cofh.core.util.helpers.InventoryHelper;
import cofh.lib.api.item.IColorableItem;
import cofh.lib.api.item.ISecurableItem;
import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.lib.common.inventory.SimpleItemInv;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.MathHelper;
import cofh.lib.util.helpers.SecurityHelper;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.core.common.inventory.storage.SatchelMenu;
import cofh.thermal.lib.common.item.InventoryContainerItemAugmentable;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentString;
import static cofh.lib.util.Utils.BUILTIN_ACCESS;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;

public class SatchelItem extends InventoryContainerItemAugmentable implements IColorableItem, IFilterableItem, IMultiModeItem, ISecurableItem, MenuProvider {

    protected static final Set<Item> BANNED_ITEMS = new ObjectOpenHashSet<>();

    public static void setBannedItems(Collection<String> itemLocs) {

        synchronized (BANNED_ITEMS) {
            BANNED_ITEMS.clear();

            for (String loc : itemLocs) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loc));
                if (item != null) {
                    BANNED_ITEMS.add(item);
                }
            }
        }
    }

    protected static final WeakHashMap<ItemStack, IFilter> FILTERS = new WeakHashMap<>(MAP_CAPACITY);

    public SatchelItem(Properties builder, int slots) {

        super(builder, slots);

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.withDefaultNamespace("color"), (stack, world, entity, seed) -> (stack.has(DataComponents.DYED_COLOR) ? 1F : 0));
        ProxyUtils.registerColorable(this);

        numSlots = () -> ThermalCoreConfig.storageAugments;
        augValidator = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FILTER);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.satchel.use").withStyle(ChatFormatting.GRAY));
        if (FilterHelper.hasFilter(stack)) {
            tooltip.add(getTextComponent("info.thermal.satchel.use.sneak").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(getTextComponent("info.thermal.satchel.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        addModeChangeTooltip(this, stack, worldIn, tooltip, flagIn);

        super.tooltipDelegate(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        ItemStack stack = playerIn.getItemInHand(handIn);
        return useDelegate(stack, playerIn, handIn) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }

    // region IColorableItem
    @Override
    public int getColor(ItemStack item, int colorIndex) {

        return colorIndex == 0 ? DyedItemColor.getOrDefault(item, 0xFFFFFFFF) : 0xFFFFFFFF;
    }
    // endregion

    // region HELPERS
    public static boolean onItemPickup(Player player, ItemEntity eventItem, ItemStack container) {

        SatchelItem satchelItem = (SatchelItem) container.getItem();
        if (satchelItem.getMode(container) <= 0 || !satchelItem.canPlayerAccess(container, player)) {
            return false;
        }
        int count = eventItem.getItem().getCount();

        if (satchelItem.getFilter(container).valid(eventItem.getItem())) {
            dropExtraItems(container, player);

            SimpleItemInv containerInv = satchelItem.getContainerInventory(container);
            ItemStack remainder = InventoryHelper.insertStackIntoInventory(containerInv, eventItem.getItem(), false);
            eventItem.getItem().setCount(remainder.getCount());

            if (eventItem.getItem().getCount() != count) {
                container.setPopTime(5);
                player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((MathHelper.RANDOM.nextFloat() - MathHelper.RANDOM.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                CompoundTag customData = container.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                CompoundTag inventoryTag = customData.getCompound(TAG_ITEM_INV);
                containerInv.write(BUILTIN_ACCESS, inventoryTag);
                customData.put(TAG_ITEM_INV, inventoryTag);
                container.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
                satchelItem.onContainerInventoryChanged(container);
            }
        }
        return eventItem.getItem().getCount() != count;
    }

    public static void dropExtraItems(ItemStack container, Player player) {

        if (container.getItem() instanceof SatchelItem satchel) {
            CompoundTag nbt = satchel.getOrCreateInvTag(container);
            int numSlots = satchel.getContainerSlots(container);

            ListTag list = nbt.getList(TAG_ITEM_INV, TAG_COMPOUND);
            for (int i = list.size(); i > 0; --i) {
                CompoundTag slotTag = list.getCompound(i);
                int slot = slotTag.getByte(TAG_SLOT);
                if (slot >= numSlots) {
                    Utils.dropItemStackIntoWorldWithRandomness(ItemStorageCoFH.loadItemStack(BUILTIN_ACCESS, slotTag), player.level(), player.position());
                } else {
                    return; // This optimization breaks out of the loop early, since slots are always tagged in ascending order.
                }
            }
        }
    }

    protected boolean useDelegate(ItemStack stack, Player player, InteractionHand hand) {

        if (Utils.isFakePlayer(player) || hand == InteractionHand.OFF_HAND) {
            return false;
        }
        if (player instanceof ServerPlayer) {
            if (!canPlayerAccess(stack, player)) {
                ProxyUtils.setOverlayMessage(player, Component.translatable("info.cofh.secure_warning", SecurityHelper.getOwnerName(stack)));
                return false;
            } else if (SecurityHelper.attemptClaimItem(stack, player)) {
                ProxyUtils.setOverlayMessage(player, Component.translatable("info.cofh.secure_item"));
                return false;
            }
            dropExtraItems(stack, player);
            if (player.isSecondaryUseActive()) {
                return openFilterGui((ServerPlayer) player, stack);
            }
            openGui((ServerPlayer) player, stack);
        }
        return true;
    }

    @Override
    protected SimpleItemInv readInventoryFromNBT(ItemStack container) {

        CompoundTag containerTag = getOrCreateInvTag(container);
        int numSlots = getContainerSlots(container);
        ArrayList<ItemStorageCoFH> invSlots = new ArrayList<>(numSlots);
        for (int i = 0; i < numSlots; ++i) {
            invSlots.add(new ItemStorageCoFH());
        }
        SimpleItemInv inventory = new SimpleItemInv(invSlots) {

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {

                if (slot < 0 || slot >= getSlots()) {
                    return false;
                }
                return !BANNED_ITEMS.contains(stack.getItem());
            }
        };
        inventory.read(BUILTIN_ACCESS, containerTag);
        return inventory;
    }

    @Override
    protected void setAttributesFromAugment(CompoundTag properties, CompoundTag augmentData) {

        setAttributeFromAugmentString(properties, augmentData, TAG_FILTER_TYPE);

        super.setAttributesFromAugment(properties, augmentData);
    }
    // endregion

    // region MenuProvider
    @Override
    public Component getDisplayName() {

        return Component.translatable("item.thermal.satchel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new SatchelMenu(i, inventory, player);
    }
    // endregion

    // region IFilterableItem
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

        player.level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
        ProxyUtils.setOverlayMessage(player, Component.translatable("info.thermal.satchel.mode." + getMode(stack)));
    }
    // endregion
}
