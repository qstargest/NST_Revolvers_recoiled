package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

/**
 * Extends AssemblyRecipe with bullet-specific crafting rules.
 *
 * Bullet crafting additionally requires a pickaxe of an appropriate tier in the
 * player's inventory. Each bullet crafted consumes one durability point from the
 * best available pickaxe; the pickaxe is destroyed when its durability reaches zero,
 * playing a break sound at the player's position.
 *
 * Unlike revolver recipes, bullet recipes support batch crafting via craftBullets(),
 * which scales both material and durability costs linearly by count.
 * Results are distributed across multiple stacks when count exceeds the item's max stack size.
 */
public class BulletAssemblyRecipe extends AssemblyRecipe {

    // -------------------------------------------------------------------------
    // Required pickaxe tier
    // -------------------------------------------------------------------------

    /**
     * Minimum pickaxe tier required to craft this bullet type.
     *   ANY        — stone bullet:        any pickaxe accepted
     *   STONE_PLUS — iron bullet:         stone pickaxe or better
     *   IRON_PLUS  — gold/diamond bullet: iron pickaxe or better
     */
    public enum PickaxeTier {
        ANY,        // stone bullet  — any pickaxe
        STONE_PLUS, // iron bullet   — stone pickaxe or better
        IRON_PLUS   // gold/diamond  — iron pickaxe or better
    }

    // Ordered weakest to strongest; used for valid-pickaxe checks and tooltip display
    private static final List<Item> TIER_ANY = List.of(
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
    );
    private static final List<Item> TIER_STONE_PLUS = List.of(
            Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
    );
    private static final List<Item> TIER_IRON_PLUS = List.of(
            Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
    );

    /** The minimum pickaxe tier needed to craft this bullet type. */
    private final PickaxeTier requiredTier;

    /**
     * Creates a new bullet recipe.
     *
     * @param id             unique identifier
     * @param result         bullet item produced by this recipe
     * @param translationKey i18n key for the GUI label
     * @param ingredients    required materials
     * @param requiredTier   minimum pickaxe tier required to craft
     */
    public BulletAssemblyRecipe(String id, Item result, String translationKey,
                                List<Ingredient> ingredients, PickaxeTier requiredTier) {
        super(id, result, translationKey, ingredients);
        this.requiredTier = requiredTier;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return the minimum pickaxe tier required to craft this bullet */
    public PickaxeTier getRequiredTier() { return requiredTier; }

    /**
     * Returns the list of pickaxe items that satisfy the required tier,
     * ordered from weakest to strongest.
     *
     * @return immutable list of valid pickaxe items
     */
    public List<Item> getValidPickaxes() {
        return switch (requiredTier) {
            case ANY        -> TIER_ANY;
            case STONE_PLUS -> TIER_STONE_PLUS;
            case IRON_PLUS  -> TIER_IRON_PLUS;
        };
    }

    /**
     * Returns the translation key for the minimum-tier requirement label shown in the GUI.
     * Displayed in red when no valid pickaxe is present in the player's inventory.
     *
     * @return i18n key describing the pickaxe tier requirement
     */
    public String getTierTranslationKey() {
        return switch (requiredTier) {
            case ANY        -> "gui.nst_revrecoiled.pickaxe_tier_any";
            case STONE_PLUS -> "gui.nst_revrecoiled.pickaxe_tier_stone";
            case IRON_PLUS  -> "gui.nst_revrecoiled.pickaxe_tier_iron";
        };
    }

    /**
     * @param item item to check
     * @return true if the item satisfies this recipe's pickaxe tier requirement
     */
    public boolean isValidPickaxe(Item item) {
        return getValidPickaxes().contains(item);
    }

    // -------------------------------------------------------------------------
    // Pickaxe search
    // -------------------------------------------------------------------------

    /**
     * Finds the slot of the best matching pickaxe in the player's inventory,
     * where "best" is defined as the highest remaining durability.
     * Called once per frame from AssemblyTableScreen.refreshInventoryCache()
     * to avoid redundant scans during the render pass.
     *
     * @param inventory the player's inventory
     * @return slot index of the best pickaxe, or -1 if none is found
     */
    public int findBestPickaxeSlot(PlayerInventory inventory) {
        int bestSlot = -1;
        int bestDurability = 0;
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && isValidPickaxe(stack.getItem())) {
                int dur = stack.getMaxDamage() - stack.getDamage();
                if (dur > bestDurability) {
                    bestDurability = dur;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    /**
     * Returns the remaining durability of the best matching pickaxe.
     *
     * @param inventory the player's inventory
     * @return remaining durability (maxDamage - damage), or 0 if no pickaxe is found
     */
    public int getPickaxeRemainingDurability(PlayerInventory inventory) {
        int slot = findBestPickaxeSlot(inventory);
        if (slot < 0) return 0;
        ItemStack pick = inventory.main.get(slot);
        return pick.getMaxDamage() - pick.getDamage();
    }

    /**
     * Calculates the maximum number of bullets craftable right now, limited by
     * available materials and remaining pickaxe durability (1 point per bullet).
     *
     * @param inventory the player's inventory
     * @return maximum craftable quantity (>=0)
     */
    public int getMaxCraftable(PlayerInventory inventory) {
        // Limit by materials: crafting N bullets requires N * ingredient.count() of each type
        int maxByMats = Integer.MAX_VALUE;
        for (Ingredient ing : getIngredients()) {
            int have = countInInventory(inventory, ing.item());
            if (ing.count() <= 0) continue;
            maxByMats = Math.min(maxByMats, have / ing.count());
        }
        // Limit by pickaxe durability: 1 durability point consumed per bullet
        int durability = getPickaxeRemainingDurability(inventory);
        return Math.min(maxByMats == Integer.MAX_VALUE ? 0 : maxByMats, durability);
    }

    /**
     * @param inventory the player's inventory
     * @param count     desired quantity
     * @return true if exactly count bullets can be crafted right now
     */
    public boolean canCraftBullets(PlayerInventory inventory, int count) {
        return count > 0 && getMaxCraftable(inventory) >= count;
    }

    // -------------------------------------------------------------------------
    // Crafting (server-side only)
    // -------------------------------------------------------------------------

    /**
     * Crafts count bullets in a single operation:
     * 1. Removes count * ingredient.count() of each material from the inventory.
     * 2. Reduces the best pickaxe's durability by count; destroys it if durability
     *    reaches zero and plays ENTITY_ITEM_BREAK at the player's position.
     * 3. Distributes result bullets across stacks of up to getResult().getMaxCount()
     *    each, inserting into the inventory and dropping any overflow at the player's feet.
     *
     * Distributing across multiple stacks prevents silently truncating large batch
     * crafts for items whose max stack size is smaller than count.
     *
     * @param inventory the player's inventory
     * @param count     number of bullets to craft
     * @return true if the craft succeeded
     */
    public boolean craftBullets(PlayerInventory inventory, int count) {
        if (!canCraftBullets(inventory, count)) return false;

        // Consume materials
        for (Ingredient ing : getIngredients()) {
            removeFromInventory(inventory, ing.item(), ing.count() * count);
        }

        // Damage or destroy the pickaxe (1 durability per bullet)
        int slot = findBestPickaxeSlot(inventory);
        if (slot >= 0) {
            ItemStack pick = inventory.main.get(slot);
            int newDamage = pick.getDamage() + count;
            if (newDamage >= pick.getMaxDamage()) {
                inventory.main.set(slot, ItemStack.EMPTY);
                // Play item-break sound so the player has audio feedback when the pickaxe breaks
                World world = inventory.player.getWorld();
                world.playSound(null,
                        inventory.player.getX(),
                        inventory.player.getY(),
                        inventory.player.getZ(),
                        SoundEvents.ENTITY_ITEM_BREAK,
                        SoundCategory.PLAYERS,
                        0.8f, 0.8f + world.random.nextFloat() * 0.4f);
            } else {
                pick.setDamage(newDamage);
            }
        }

        // Distribute bullets across valid stack sizes; drop any that don't fit in the inventory
        int maxStack  = getResult().getMaxCount();
        int remaining = count;
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack result = new ItemStack(getResult(), batch);
            if (!inventory.insertStack(result)) {
                inventory.player.dropItem(result, false);
            }
            remaining -= batch;
        }

        return true;
    }
}
