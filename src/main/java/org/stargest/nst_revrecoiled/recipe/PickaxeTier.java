package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

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
 * Addon usage example:
 *   PickaxeTier DIAMOND_ONLY = new PickaxeTier() {
 *       public List<Item> getValidPickaxes() { return List.of(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE); }
 *       public String getTranslationKey()    { return "gui.myaddon.pickaxe_tier_diamond"; }
 *   };
 */
public interface PickaxeTier {

    /**
     * Returns the list of pickaxe items that satisfy this tier requirement,
     * ordered from weakest to strongest.
     * Used by BulletAssemblyRecipe to validate the player's inventory
     * and by AssemblyTableScreen to display the fallback icon (index 0)
     * when no valid pickaxe is present.
     *
     * @return immutable list of valid pickaxe items
     */
    List<Item> getValidPickaxes();

    /**
     * Returns the i18n translation key for the tier requirement label
     * shown in the Assembly Table GUI when no valid pickaxe is present.
     * Displayed in red.
     *
     * @return translation key, e.g. "gui.nst_revrecoiled.pickaxe_tier_any"
     */
    String getTranslationKey();

    // -------------------------------------------------------------------------
    // Built-in tiers
    // -------------------------------------------------------------------------

    /** Any pickaxe is accepted. Used for stone bullets. */
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

    /** Stone pickaxe or better. Used for iron bullets. */
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

    /** Iron pickaxe or better. Used for gold and diamond bullets. */
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
