package org.stargest.nst_revrecoiled.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;

/**
 * The Assembly Table block — a custom crafting station for revolvers and bullets.
 * Opens AssemblyTableScreen when right-clicked by a player.
 *
 * Block registration is handled by ModBlocks;
 * the screen handler type is registered in ModScreenHandlers.
 *
 * A ScreenHandlerContext carrying the block's world position is passed to the handler
 * on open so that AssemblyTableScreenHandler.canUse() can enforce block-proximity
 * validation and close the screen if the player moves too far away.
 */
public class AssemblyTableBlock extends Block {

    /** Codec used by Minecraft's block state serialisation system. */
    public static final MapCodec<AssemblyTableBlock> CODEC = createCodec(AssemblyTableBlock::new);

    public AssemblyTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    /**
     * Opens the Assembly Table GUI when a player right-clicks the block.
     * Only executes on the logical server; the client returns SUCCESS immediately
     * to avoid interaction delay.
     *
     * Passes ScreenHandlerContext.create(world, pos) to the handler so the server can
     * enforce block-proximity checks via canUse() while the screen is open.
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new AssemblyTableScreenHandler(
                        syncId, inv, ScreenHandlerContext.create(world, pos)),
                Text.translatable("block.nst_revrecoiled.assembly_table")
        ));

        return ActionResult.CONSUME;
    }
}
