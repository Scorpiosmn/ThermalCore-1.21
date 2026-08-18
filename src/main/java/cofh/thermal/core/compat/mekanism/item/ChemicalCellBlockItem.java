package cofh.thermal.core.compat.mekanism.item;

import cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity;
import cofh.thermal.lib.common.item.BlockItemAugmentable;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.Utils.BUILTIN_ACCESS;
import static cofh.lib.util.helpers.StringHelper.format;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.lib.util.helpers.StringHelper.localize;

/** Item form of a chemical cell, including its stored chemical in the Shift tooltip. */
public class ChemicalCellBlockItem extends BlockItemAugmentable {

    public ChemicalCellBlockItem(Block blockIn, Properties builder) {

        super(blockIn, builder);

        setEnchantability(5);
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {

        HolderLookup.Provider access = level == null ? BUILTIN_ACCESS : level.registryAccess();
        CompoundTag blockTag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        ChemicalStack chemical = ChemicalStack.parseOptional(access, blockTag.getCompound(ChemicalCellBlockEntity.TAG_CHEMICAL));
        long capacity = blockTag.contains("Capacity") ? Math.max(0L, blockTag.getLong("Capacity"))
                : ChemicalCellBlockEntity.BASE_CAPACITY;
        if (!chemical.isEmpty()) {
            tooltip.add(Component.translatable(chemical.getTranslationKey()));
        }
        tooltip.add(getTextComponent(localize("info.cofh.amount") + ": " + format(chemical.getAmount()) + " / "
                + format(capacity) + " " + localize("info.cofh.unit_mb")));
    }

}
