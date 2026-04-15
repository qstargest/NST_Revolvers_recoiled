package org.stargest.nst_revrecoiled.util;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Blocks.AssemblyTableBlock;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for all custom blocks in the mod.
 * Uses DeferredRegister for clean registration in Forge.
 *
 * Includes the Assembly Table — the mod's primary crafting station.
 * Adapted for Forge 1.20.1.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    public static final RegistryObject<AssemblyTableBlock> ASSEMBLY_TABLE = BLOCKS.register(
            "assembly_table",
            () -> new AssemblyTableBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD))
    );

    // BlockItem so it can appear in inventory
    public static final RegistryObject<BlockItem> ASSEMBLY_TABLE_ITEM =
            ITEMS.register("assembly_table", () -> new BlockItem(ASSEMBLY_TABLE.get(), new Item.Properties()));
}
