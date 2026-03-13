package org.stargest.nst_revrecoiled.Villager;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.util.ModBlocks;
import org.stargest.nst_revrecoiled.util.ModItems;

/**
 * Registers the revolvermaker villager profession, its associated Point of Interest,
 * and all trade offers across five career levels.
 *
 * Key features:
 * - Custom POI tied to the Assembly Table block
 * - Revolvermaker profession uses Assembly Table as workstation
 * - Plays armorer work sound for immersion
 * - POI block states registered automatically via PointOfInterestHelper
 * - Trade progression from basic materials (level 1) to diamond revolvers (level 5)
 *
 * Registration order matters: POI is registered by PointOfInterestHelper at field
 * initialization time, so only the profession and trades need explicit registration in init().
 * Keeping POI registration implicit avoids double-registration issues.
 *
 * Trade table summary:
 *   Level 1 — Gunpowder → Emerald; Emerald → Stone Bullets
 *   Level 2 — Emerald → Cobblestone Revolver
 *   Level 3 — Emerald → Iron Revolver; Lapis → Emerald; Emerald → Iron Bullets
 *   Level 4 — Emerald → Golden Revolver; Emerald → Golden Bullets
 *   Level 5 — Diamond → Emerald; Emerald → Diamond Revolver; Emerald → Diamond Bullets
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
     * Registers the revolvermaker profession and all associated trade offers.
     * POI is already registered at class load time via PointOfInterestHelper;
     * only the profession and trades require explicit registration here.
     * Called from the mod's main initializer.
     *
     * Trade offers follow the vanilla tiered progression pattern — each level
     * unlocks higher-tier revolvers and matching ammunition. Resource buy-back
     * trades (gunpowder, lapis, diamond → emerald) are included at levels 1, 3,
     * and 5 to give the player a consistent material sink at every tier.
     */
    public static void init() {
        Registry.register(
                Registries.VILLAGER_PROFESSION,
                REVOLVERMAKER_KEY,
                REVOLVERMAKER
        );

        // --- Level 1: Novice — basic materials and stone ammunition ---
        // Gunpowder buy-back: 32 gunpowder → 1 emerald (7 uses, 2 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 1, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.GUNPOWDER, 32),
                        new ItemStack(Items.EMERALD, 1), 7, 2, 0.05f)));
        // Stone bullets: 1 emerald → 8 stone bullets (12 uses, 2 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 1, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 1),
                        new ItemStack(ModItems.STONE_BULLET, 8), 12, 2, 0.05f)));

        // --- Level 2: Apprentice — entry-level revolver ---
        // Cobblestone revolver: 7 emeralds → 1 cobblestone revolver (7 uses, 5 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 2, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 7),
                        new ItemStack(ModItems.COBBLESTONE_REVOLVER, 1), 7, 5, 0.05f)));

        // --- Level 3: Journeyman — iron tier revolver and ammunition ---
        // Iron revolver: 12 emeralds → 1 iron revolver (7 uses, 10 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 3, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 12),
                        new ItemStack(ModItems.IRON_REVOLVER, 1), 7, 10, 0.05f)));
        // Lapis buy-back: 25 lapis lazuli → 1 emerald (7 uses, 2 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 3, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.LAPIS_LAZULI, 25),
                        new ItemStack(Items.EMERALD, 1), 7, 2, 0.04f)));
        // Iron bullets: 3 emeralds → 6 iron bullets (12 uses, 5 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 3, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 3),
                        new ItemStack(ModItems.IRON_BULLET, 6), 12, 5, 0.05f)));

        // --- Level 4: Expert — golden tier revolver and ammunition ---
        // Golden revolver: 20 emeralds → 1 golden revolver (5 uses, 15 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 4, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 20),
                        new ItemStack(ModItems.GOLDEN_REVOLVER, 1), 5, 15, 0.05f)));
        // Golden bullets: 4 emeralds → 4 golden bullets (8 uses, 10 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 4, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 4),
                        new ItemStack(ModItems.GOLDEN_BULLET, 4), 8, 10, 0.05f)));

        // --- Level 5: Master — diamond tier revolver and ammunition ---
        // Diamond buy-back: 6 diamonds → 1 emerald (7 uses, 2 XP, high price multiplier)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 5, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.DIAMOND, 6),
                        new ItemStack(Items.EMERALD, 1), 7, 2, 0.4f)));
        // Diamond revolver: 40 emeralds → 1 diamond revolver (3 uses, 25 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 5, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 40),
                        new ItemStack(ModItems.DIAMOND_REVOLVER, 1), 3, 25, 0.05f)));
        // Diamond bullets: 12 emeralds → 6 diamond bullets (5 uses, 20 XP)
        TradeOfferHelper.registerVillagerOffers(REVOLVERMAKER, 5, factories -> factories.add((entity, random) ->
                new TradeOffer(new TradedItem(Items.EMERALD, 12),
                        new ItemStack(ModItems.DIAMOND_BULLET, 6), 5, 20, 0.05f)));
    }
}
