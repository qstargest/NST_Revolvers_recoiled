package org.stargest.nst_revrecoiled.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;

/**
 * The Assembly Table block — a custom crafting station for revolvers and bullets.
 * Opens AssemblyTableScreen when right-clicked by a player.
 *
 * Adapted for Forge 1.20.1 (Removed MapCodec/codec).
 */
public class AssemblyTableBlock extends Block {

    public AssemblyTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Opens the Assembly Table GUI when a player right-clicks the block.
     * Passes a MenuProvider to the handler so the server can
     * enforce block-proximity checks via stillValid() while the screen is open.
     */
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new AssemblyTableScreenHandler(
                        syncId, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.nst_revrecoiled.assembly_table")
        ));

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
