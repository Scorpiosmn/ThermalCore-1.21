package cofh.thermal.core.common.block.entity;

import cofh.thermal.lib.common.block.entity.AugmentableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnchantBenchBlockEntity extends AugmentableBlockEntity {

    public EnchantBenchBlockEntity(BlockPos pos, BlockState state) {

        super(null, pos, state);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {

        return null;
    }

}
