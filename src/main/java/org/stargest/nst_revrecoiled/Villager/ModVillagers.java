package org.stargest.nst_revrecoiled.Villager;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.util.ModBlocks;

public class ModVillagers {

    // --- Point of Interest ---

    public static final RegistryKey<PointOfInterestType> ASSEMBLY_TABLE_POI_KEY =
            RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE,
                    Identifier.of(Main.MOD_ID, "assembly_table_poi"));

    // PointOfInterestHelper itself updates BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE,
    public static final PointOfInterestType ASSEMBLY_TABLE_POI =
            PointOfInterestHelper.register(
                    Identifier.of(Main.MOD_ID, "assembly_table_poi"),
                    1,  // Ticket count - how many villagers are using the POI at the same time
                    1,  // Search distance
                    ModBlocks.ASSEMBLY_TABLE  // all BlockStates of the block will be added automatically
            );

    // --- Profession ---

    public static final RegistryKey<VillagerProfession> REVOLVERMAKER_KEY =
            RegistryKey.of(RegistryKeys.VILLAGER_PROFESSION,
                    Identifier.of(Main.MOD_ID, "revolvermaker"));

    public static final VillagerProfession REVOLVERMAKER = new VillagerProfession(
            "revolvermaker",
            entry -> entry.matchesKey(ASSEMBLY_TABLE_POI_KEY),
            entry -> entry.matchesKey(ASSEMBLY_TABLE_POI_KEY),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.ENTITY_VILLAGER_WORK_ARMORER
    );

    // --- Registration ---

    public static void init() {
        // POI is already registered via PointOfInterestHelper above,
        // We're only registering the profession
        Registry.register(
                Registries.VILLAGER_PROFESSION,
                REVOLVERMAKER_KEY,
                REVOLVERMAKER
        );
    }
}
