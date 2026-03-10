package org.stargest.nst_revrecoiled.util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Blocks.AssemblyTableBlock;
import org.stargest.nst_revrecoiled.Main;

public class ModBlocks {
    public static Block ASSEMBLY_TABLE;

    public static void init(){
        Identifier assembly_table_ID = Identifier.of(Main.MOD_ID, "assembly_table");
        RegistryKey<Block> assembly_table_Key = RegistryKey.of(RegistryKeys.BLOCK, assembly_table_ID);

        ASSEMBLY_TABLE = Blocks.register(
                assembly_table_Key,
                AssemblyTableBlock::new,
                AbstractBlock.Settings.create().strength(3.0f).requiresTool().sounds(BlockSoundGroup.WOOD));
        Items.register(ASSEMBLY_TABLE);
    }
}
