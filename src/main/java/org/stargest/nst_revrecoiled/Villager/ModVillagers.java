package org.stargest.nst_revrecoiled.Villager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.util.ModItems;

/**
 * Registers the revolvermaker villager profession, its associated Point of Interest,
 * and all trade offers across five career levels.
 *
 * Key features:
 * - Custom POI tied to the Assembly Table block
 * - Revolvermaker profession uses Assembly Table as workstation
 * - Plays armorer work sound for immersion
 * - Trade progression from basic materials (level 1) to diamond revolvers (level 5)
 *
 * Trade table summary:
 *   Level 1 — Gunpowder → Emerald; Emerald → Stone Bullets
 *   Level 2 — Emerald → Cobblestone Revolver
 *   Level 3 — Emerald → Iron Revolver; Lapis → Emerald; Emerald → Iron Bullets
 *   Level 4 — Emerald → Golden Revolver; Emerald → Golden Bullets
 *   Level 5 — Diamond → Emerald; Emerald → Diamond Revolver; Emerald → Diamond Bullets
 */
@EventBusSubscriber(modid = NstRevRecoiled.MOD_ID)
public class ModVillagers {

    /**
     * Registry key for the Assembly Table POI.
     * Used to match the profession to its workstation via predicate.
     */
    public static final ResourceKey<PoiType> ASSEMBLY_TABLE_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                    ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "assembly_table_poi"));

    /**
     * Registry key for the revolvermaker profession.
     */
    public static final ResourceKey<VillagerProfession> REVOLVERMAKER_KEY =
            ResourceKey.create(Registries.VILLAGER_PROFESSION,
                    ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "revolvermaker"));

    /**
     * Registers all associated trade offers for the revolvermaker profession.
     * Called automatically via the VillagerTradesEvent bus.
     *
     * Trade offers follow the vanilla tiered progression pattern — each level
     * unlocks higher-tier revolvers and matching ammunition. Resource buy-back
     * trades (gunpowder, lapis, diamond → emerald) are included at levels 1, 3,
     * and 5 to give the player a consistent material sink at every tier.
     */
    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        if (!BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()).equals(REVOLVERMAKER_KEY.location())) return;

        var trades = event.getTrades();

        // Level 1 — Novice
        trades.get(1).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.GUNPOWDER, 32), new ItemStack(Items.EMERALD, 1), 7, 2, 0.05f));
        trades.get(1).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(ModItems.STONE_BULLET.get(), 8), 12, 2, 0.05f));

        // Level 2 — Apprentice
        trades.get(2).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 7), new ItemStack(ModItems.COBBLESTONE_REVOLVER.get(), 1), 7, 5, 0.05f));

        // Level 3 — Journeyman
        trades.get(3).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 12), new ItemStack(ModItems.IRON_REVOLVER.get(), 1), 7, 10, 0.05f));
        trades.get(3).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.LAPIS_LAZULI, 25), new ItemStack(Items.EMERALD, 1), 7, 2, 0.04f));
        trades.get(3).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 3), new ItemStack(ModItems.IRON_BULLET.get(), 6), 12, 5, 0.05f));

        // Level 4 — Expert
        trades.get(4).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 20), new ItemStack(ModItems.GOLDEN_REVOLVER.get(), 1), 5, 15, 0.05f));
        trades.get(4).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 4), new ItemStack(ModItems.GOLDEN_BULLET.get(), 4), 8, 10, 0.05f));

        // Level 5 — Master
        trades.get(5).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.DIAMOND, 6), new ItemStack(Items.EMERALD, 1), 7, 2, 0.4f));
        trades.get(5).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 40), new ItemStack(ModItems.DIAMOND_REVOLVER.get(), 1), 3, 25, 0.05f));
        trades.get(5).add((entity, rng) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 12), new ItemStack(ModItems.DIAMOND_BULLET.get(), 6), 5, 20, 0.05f));
    }
}
