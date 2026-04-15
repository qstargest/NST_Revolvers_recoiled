package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Defines the minimum pickaxe tier required to craft a bullet type at the Assembly Table.
 *
 * Implemented as an interface with static constant instances rather than an enum,
 * so addon mods can define custom tiers by implementing this interface directly
 * without modifying the base mod.
 *
 * Each tier carries an ordered list of valid pickaxe items (weakest to strongest)
 * and a translation key for the GUI status line shown when no valid pickaxe is present.
 * The first item in the list is used as the fallback icon in the Assembly Table GUI.
 *
 * Built-in tiers:
 *   ANY        — stone bullet:        any pickaxe accepted
 *   STONE_PLUS — iron bullet:         stone pickaxe or better
 *   IRON_PLUS  — gold/diamond bullet: iron pickaxe or better
 *
 * Adapted for Forge 1.20.1.
 */
public interface PickaxeTier {

    List<Item> getValidPickaxes();
    String getTranslationKey();

    PickaxeTier ANY = new PickaxeTier() {
        private static final List<Item> PICKAXES = List.of(
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
        );

        @Override
        public List<Item> getValidPickaxes() { return PICKAXES; }

        @Override
        public String getTranslationKey() {
            return "gui.nst_revrecoiled.pickaxe_tier_any";
        }

        @Override
        public String toString() { return "PickaxeTier.ANY"; }
    };

    PickaxeTier STONE_PLUS = new PickaxeTier() {
        private static final List<Item> PICKAXES = List.of(
                Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
        );

        @Override
        public List<Item> getValidPickaxes() { return PICKAXES; }

        @Override
        public String getTranslationKey() {
            return "gui.nst_revrecoiled.pickaxe_tier_stone";
        }

        @Override
        public String toString() { return "PickaxeTier.STONE_PLUS"; }
    };

    PickaxeTier IRON_PLUS = new PickaxeTier() {
        private static final List<Item> PICKAXES = List.of(
                Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE,
                Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
        );

        @Override
        public List<Item> getValidPickaxes() { return PICKAXES; }

        @Override
        public String getTranslationKey() {
            return "gui.nst_revrecoiled.pickaxe_tier_iron";
        }

        @Override
        public String toString() { return "PickaxeTier.IRON_PLUS"; }
    };
}
