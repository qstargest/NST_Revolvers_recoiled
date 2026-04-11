package org.stargest.nst_revrecoiled.util;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.Blocks.AssemblyTableBlock;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * Registry for all custom blocks in the mod.
 * Uses DeferredRegister for clean registration in NeoForge.
 *
 * Includes the Assembly Table — the mod's primary crafting station.
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NstRevRecoiled.MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NstRevRecoiled.MOD_ID);

    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE = BLOCKS.registerBlock(
            "assembly_table",
            AssemblyTableBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
    );

    // BlockItem so it can appear in inventory
    public static final DeferredItem<BlockItem> ASSEMBLY_TABLE_ITEM =
            ITEMS.registerItem("assembly_table", (props) -> new BlockItem(ASSEMBLY_TABLE.get(), props));
}
