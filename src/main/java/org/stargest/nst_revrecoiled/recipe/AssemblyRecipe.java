package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Describes a single crafting recipe for the Assembly Table.
 * Holds the result item, a localisation key, and an ordered list of
 * Ingredient requirements. Contains all crafting and inventory-query logic
 * executed on the server side.
 *
 * Instances are declared and registered in AssemblyRecipes.
 * Bullet-specific recipes extend this class via BulletAssemblyRecipe.
 *
 * Recipes are identified by a namespaced Identifier rather than a plain string,
 * enabling addon mods to register recipes under their own namespace without
 * risking collisions with base-mod or other addon recipe names.
 */
public class AssemblyRecipe {

    /** A pair of an item and a required quantity representing a single crafting ingredient. */
    public record Ingredient(Item item, int count) {}

    /** Unique namespaced identifier, e.g. Identifier.of("nst_revrecoiled", "cobblestone_revolver"). */
    private final Identifier id;
    /** Item produced when the recipe is successfully crafted. */
    private final Item result;
    /** Translation key used as the GUI label, e.g. "item.nst_revrecoiled.cobblestone_revolver". */
    private final String translationKey;
    /** Ordered, immutable list of required materials. */
    private final List<Ingredient> ingredients;

    /**
     * Creates a new recipe.
     *
     * @param id             unique namespaced identifier
     * @param result         item given to the player on craft
     * @param translationKey i18n key for the GUI label
     * @param ingredients    required materials (copied defensively)
     */
    public AssemblyRecipe(Identifier id, Item result, String translationKey,
                          List<Ingredient> ingredients) {
        this.id             = id;
        this.result         = result;
        this.translationKey = translationKey;
        this.ingredients    = List.copyOf(ingredients);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return unique namespaced identifier of this recipe */
    public Identifier       getId()             { return id;             }
    /** @return item produced by this recipe */
    public Item             getResult()         { return result;         }
    /** @return i18n translation key for the GUI label */
    public String           getTranslationKey() { return translationKey; }
    
    /** @return list of required ingredients, fetched from configuration if available. */
    public List<Ingredient> getIngredients() {
        List<org.stargest.nst_revrecoiled.util.ModConfig.IngredientConfig> configIngs = 
                org.stargest.nst_revrecoiled.util.ModConfig.get().recipes.get(id.toString());
        
        if (configIngs != null) {
            return configIngs.stream()
                .map(ci -> new Ingredient(
                    net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(ci.item)),
                    ci.count
                )).toList();
        }
        return ingredients;
    }

    // -------------------------------------------------------------------------
    // Craft logic (server-side)
    // -------------------------------------------------------------------------

    /**
     * @return true if the player's inventory contains all required materials
     */
    public boolean canCraft(PlayerInventory inventory) {
        for (Ingredient ing : getIngredients()) {
            if (countInInventory(inventory, ing.item()) < ing.count()) return false;
        }
        return true;
    }

    /**
     * Executes the craft: consumes all ingredients then adds the result to the inventory.
     * If the inventory is full, the result item is dropped at the player's feet.
     *
     * @return true if the craft succeeded
     */
    public boolean craft(PlayerInventory inventory) {
        List<Ingredient> currentIngredients = getIngredients();
        if (!canCraft(inventory)) return false;

        for (Ingredient ing : currentIngredients) {
            removeFromInventory(inventory, ing.item(), ing.count());
        }

        ItemStack resultStack = new ItemStack(result);
        if (!inventory.insertStack(resultStack)) {
            inventory.player.dropItem(resultStack, false);
        }
        return true;
    }

    /**
     * Counts how many of the given item are in the player's full inventory
     * (main slots + hotbar, indices 0–35 in inventory.main).
     *
     * @param inventory the player's inventory
     * @param item      item type to count
     * @return total count across all slots
     */
    public int countInInventory(PlayerInventory inventory, Item item) {
        int total = 0;
        for (ItemStack stack : inventory.main) {
            if (stack.isOf(item)) total += stack.getCount();
        }
        return total;
    }

    /**
     * Removes count items of the given type from the player's inventory,
     * spreading the removal across multiple stacks if necessary.
     *
     * @param inventory inventory to remove items from
     * @param item      item type to remove
     * @param count     total quantity to remove
     */
    protected void removeFromInventory(PlayerInventory inventory, Item item, int count) {
        int remaining = count;
        for (ItemStack stack : inventory.main) {
            if (remaining <= 0) break;
            if (stack.isOf(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }
}
