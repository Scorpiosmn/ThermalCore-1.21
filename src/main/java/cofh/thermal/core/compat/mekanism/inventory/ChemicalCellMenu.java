package cofh.thermal.core.compat.mekanism.inventory;

import cofh.core.common.inventory.BlockEntityCoFHMenu;
import cofh.lib.common.inventory.wrapper.InvWrapperCoFH;
import cofh.thermal.core.compat.mekanism.block.entity.ChemicalCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.core.compat.mekanism.MekanismCompat.CHEMICAL_CELL_CONTAINER;

public class ChemicalCellMenu extends BlockEntityCoFHMenu {

    public final ChemicalCellBlockEntity tile;

    public ChemicalCellMenu(int windowId, Level world, BlockPos pos, Inventory inventory, Player player) {

        super(CHEMICAL_CELL_CONTAINER.get(), windowId, world, pos, inventory, player);
        tile = (ChemicalCellBlockEntity) world.getBlockEntity(pos);
        bindAugmentSlots(new InvWrapperCoFH(tile.getItemInv()), 0, tile.augSize());
        bindPlayerInventory(inventory);
    }

}
