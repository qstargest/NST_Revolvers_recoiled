package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.stargest.nst_revrecoiled.util.ModConfig;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Base class for recipes crafted at the Assembly Table.
 * Defines the result item, GUI translation key, and ingredients.
 *
 * Ingredients are handled structurally (List<Ingredient>) rather than via standard
 * Minecraft Recipe system, because the Assembly Table implements a custom
 * pseudo-crafting mechanic where the player doesn't place items in a grid,
 * but instead clicks a recipe to consume items directly from their inventory.
 *
 * Provides canCraft() and craft() methods that check and consume from a standard
 * PlayerInventory. Extending classes (like BulletAssemblyRecipe) can add custom
 * requirements (e.g., pickaxe tier) and batching logic.
 */
public class AssemblyRecipe {

    public record Ingredient(Item item, int count) {}

    private final ResourceLocation id;
    private final Item result;
    private final String translationKey;
    private final List<Ingredient> ingredients;

    public AssemblyRecipe(ResourceLocation id, Item result, String translationKey,
                          List<Ingredient> ingredients) {
        this.id             = id;
        this.result         = result;
        this.translationKey = translationKey;
        this.ingredients    = List.copyOf(ingredients);
    }

    public ResourceLocation getId()             { return id;             }
    public Item             getResult()         { return result;         }
    public String           getTranslationKey() { return translationKey; }
    
    public List<Ingredient> getIngredients() {
        List<ModConfig.IngredientConfig> configIngs = 
                ModConfig.get().recipes.get(id.toString());
        
        if (configIngs != null) {
            return configIngs.stream()
                .map(ci -> new Ingredient(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(ci.item)),
                    ci.count
                )).toList();
        }
        return ingredients;
    }

    public boolean canCraft(Inventory inventory) {
        for (Ingredient ing : getIngredients()) {
            if (countInInventory(inventory, ing.item()) < ing.count()) return false;
        }
        return true;
    }

    public boolean craft(Inventory inventory) {
        List<Ingredient> currentIngredients = getIngredients();
        if (!canCraft(inventory)) return false;

        for (Ingredient ing : currentIngredients) {
            removeFromInventory(inventory, ing.item(), ing.count());
        }

        ItemStack resultStack = new ItemStack(result);
        if (!inventory.add(resultStack)) {
            inventory.player.drop(resultStack, false);
        }
        return true;
    }

    public int countInInventory(Inventory inventory, Item item) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    protected void removeFromInventory(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (ItemStack stack : inventory.items) {
            if (remaining <= 0) break;
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
