package org.stargest.nst_revrecoiled.util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Blocks.GunnerTableBlock;
import org.stargest.nst_revrecoiled.Main;

public class ModBlocks {
    public static Block GUNNER_TABLE;

    public static void init(){
        Identifier gunman_table_ID = Identifier.of(Main.MOD_ID, "gunner_table");
        RegistryKey<Block> gunman_table_Key = RegistryKey.of(RegistryKeys.BLOCK, gunman_table_ID);

        GUNNER_TABLE = Blocks.register(
                gunman_table_Key,
                GunnerTableBlock::new,
                AbstractBlock.Settings.create().strength(3.0f).requiresTool().sounds(BlockSoundGroup.WOOD));
        Items.register(GUNNER_TABLE);
    }
}
