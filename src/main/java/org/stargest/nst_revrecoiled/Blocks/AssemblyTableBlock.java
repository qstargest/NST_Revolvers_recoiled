package org.stargest.nst_revrecoiled.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleMenuProvider;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;

/**
 * The Assembly Table block — a custom crafting station for revolvers and bullets.
 * Opens AssemblyTableScreen when right-clicked by a player.
 *
 * Block registration is handled by ModBlocks;
 * the screen handler type is registered in ModScreenHandlers.
 *
 * A MenuProvider carrying the block's world position is passed to the handler
 * on open so that AssemblyTableScreenHandler.stillValid() can enforce block-proximity
 * validation and close the screen if the player moves too far away.
 */
public class AssemblyTableBlock extends Block {

    /** Codec used by Minecraft's block state serialisation system. */
    public static final MapCodec<AssemblyTableBlock> CODEC = simpleCodec(AssemblyTableBlock::new);

    public AssemblyTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * Opens the Assembly Table GUI when a player right-clicks the block.
     * Only executes on the logical server; the client returns SUCCESS immediately
     * to avoid interaction delay.
     *
     * Passes a MenuProvider to the handler so the server can
     * enforce block-proximity checks via stillValid() while the screen is open.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new AssemblyTableScreenHandler(
                        syncId, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.nst_revrecoiled.assembly_table")
        ));

        return InteractionResult.CONSUME;
    }
}
