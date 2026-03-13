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

/**
 * Registers the revolvermaker villager profession and its associated Point of Interest.
 *
 * Key features:
 * - Custom POI tied to the Assembly Table block
 * - Revolvermaker profession uses Assembly Table as workstation
 * - Plays armorer work sound for immersion
 * - POI block states registered automatically via PointOfInterestHelper
 *
 * Registration order matters: POI is registered by PointOfInterestHelper at field
 * initialization time, so only the profession needs explicit registration in init().
 * Keeping POI registration implicit avoids double-registration issues.
 */
public class ModVillagers {

    // -------------------------------------------------------------------------
    // Point of Interest
    // -------------------------------------------------------------------------

    /**
     * Registry key for the Assembly Table POI.
     * Used to match the profession to its workstation via predicate.
     */
    public static final RegistryKey<PointOfInterestType> ASSEMBLY_TABLE_POI_KEY =
            RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE,
                    Identifier.of(Main.MOD_ID, "assembly_table_poi"));

    /**
     * Point of Interest for the Assembly Table block.
     * Allows only one villager to claim the table at a time (ticket count = 1).
     * PointOfInterestHelper automatically registers all BlockStates of the block
     * and updates BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE — no manual state enumeration needed.
     */
    public static final PointOfInterestType ASSEMBLY_TABLE_POI =
            PointOfInterestHelper.register(
                    Identifier.of(Main.MOD_ID, "assembly_table_poi"),
                    1,  // Ticket count — max villagers using this POI simultaneously
                    1,  // Search distance in chunks
                    ModBlocks.ASSEMBLY_TABLE
            );

    // -------------------------------------------------------------------------
    // Profession
    // -------------------------------------------------------------------------

    /**
     * Registry key for the revolvermaker profession.
     */
    public static final RegistryKey<VillagerProfession> REVOLVERMAKER_KEY =
            RegistryKey.of(RegistryKeys.VILLAGER_PROFESSION,
                    Identifier.of(Main.MOD_ID, "revolvermaker"));

    /**
     * Revolvermaker villager profession.
     * Associates with the Assembly Table POI for both acquisition and work destination.
     * Empty item and block sets mean no vanilla gather or secondary work behavior.
     * Uses armorer work sound since revolvers are closest to smithed goods.
     */
    public static final VillagerProfession REVOLVERMAKER = new VillagerProfession(
            "revolvermaker",
            entry -> entry.matchesKey(ASSEMBLY_TABLE_POI_KEY),
            entry -> entry.matchesKey(ASSEMBLY_TABLE_POI_KEY),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.ENTITY_VILLAGER_WORK_ARMORER
    );

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers the revolvermaker profession into the villager profession registry.
     * POI is already registered at class load time via PointOfInterestHelper;
     * only the profession requires explicit registration here.
     * Called from the mod's main initializer.
     */
    public static void init() {
        Registry.register(
                Registries.VILLAGER_PROFESSION,
                REVOLVERMAKER_KEY,
                REVOLVERMAKER
        );
    }
}
