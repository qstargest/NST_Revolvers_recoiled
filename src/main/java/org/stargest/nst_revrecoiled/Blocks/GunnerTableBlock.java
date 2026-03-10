package org.stargest.nst_revrecoiled.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GunnerTableBlock extends Block {

    public static final MapCodec<GunnerTableBlock> CODEC = createCodec(GunnerTableBlock::new);

    public GunnerTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    // --------------------------------------------------------
    // Right-click to open GUI (ScreenHandler) here in the future
    // --------------------------------------------------------
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        // TODO: open GUI
        // player.openHandledScreen(...);

        return ActionResult.CONSUME;
    }
}
