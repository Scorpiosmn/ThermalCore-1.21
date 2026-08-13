package cofh.thermal.core.common.item;

import cofh.core.common.item.ItemCoFH;
import cofh.core.util.ProxyUtils;
import cofh.lib.api.IConveyableData;
import cofh.lib.api.control.ISecurable;
import cofh.lib.api.item.IPlacementItem;
import cofh.lib.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.canLocalize;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static net.minecraft.ChatFormatting.DARK_GRAY;
import static net.minecraft.ChatFormatting.GRAY;

public class RedprintItem extends ItemCoFH implements IPlacementItem {

    public RedprintItem(Properties builder) {

        super(builder);

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.withDefaultNamespace("has_data"), ((stack, world, entity, seed) -> stack.has(DataComponents.CUSTOM_DATA) ? 1F : 0F));
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        CompoundTag conveyableData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (conveyableData.isEmpty()) {
            tooltip.add(getTextComponent("info.thermal.redprint.use").withStyle(GRAY));
        } else {
            tooltip.add(getTextComponent("info.thermal.redprint.use.contents").withStyle(GRAY));
            tooltip.add(getTextComponent("info.thermal.redprint.use.sneak").withStyle(DARK_GRAY));

            tooltip.add(getTextComponent("info.thermal.redprint.contents"));
            for (String type : conveyableData.getAllKeys()) {
                if (!canLocalize("info.thermal.redprint.data." + type)) {
                    tooltip.add(getTextComponent("info.thermal.redprint.unknown")
                            .withStyle(DARK_GRAY));
                }
                tooltip.add(Component.literal(" - ")
                        .append(getTextComponent("info.thermal.redprint.data." + type)
                                .withStyle(GRAY))
                );
            }
        }
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
        } else {
            stack.remove(DataComponents.RARITY);
        }
    }

    protected void updateDataState(ItemStack stack, CompoundTag tag) {

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            stack.remove(DataComponents.RARITY);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
        }
    }

    protected boolean useDelegate(ItemStack stack, UseOnContext context) {

        Level world = context.getLevel();
        Player player = context.getPlayer();

        if (player == null || Utils.isClientWorld(world)) {
            return false;
        }
        if (player.isSecondaryUseActive() && context.getHand() == InteractionHand.MAIN_HAND) {
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.3F);
                updateDataState(stack, new CompoundTag());
            }
            return true;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof ISecurable && !((ISecurable) tile).canAccess(player)) {
            return false;
        }
        if (tile instanceof IConveyableData conveyableTile) {
            if (!stack.has(DataComponents.CUSTOM_DATA) && context.getHand() == InteractionHand.MAIN_HAND) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                conveyableTile.writeConveyableData(player, tag);
                tile.setChanged();
                if (tag.isEmpty()) {
                    updateDataState(stack, new CompoundTag());
                    return false;
                } else {
                    updateDataState(stack, tag);
                    player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.7F);
                }
            } else if (stack.has(DataComponents.CUSTOM_DATA)) {
                conveyableTile.readConveyableData(player, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
                player.level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 0.8F);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        return player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack) && useDelegate(stack, context) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 0.3F);
            }
            updateDataState(stack, new CompoundTag());
        }
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }

    // region IPlacementItem
    @Override
    public boolean onBlockPlacement(ItemStack stack, UseOnContext context) {

        return useDelegate(stack, context);
    }
    // endregion
}
