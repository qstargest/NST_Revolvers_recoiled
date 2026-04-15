package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
 *
 * The required tier is expressed as a PickaxeTier interface rather than the built-in
 * Vanilla enum, allowing addon mods to pass custom tiers with different valid pickaxe
 * sets and translation keys without modifying this class.
 *
 * Adapted for Forge 1.20.1.
 */
public class BulletAssemblyRecipe extends AssemblyRecipe {

    private final PickaxeTier requiredTier;

    public BulletAssemblyRecipe(ResourceLocation id, Item result, String translationKey,
                                List<Ingredient> ingredients, PickaxeTier requiredTier) {
        super(id, result, translationKey, ingredients);
        this.requiredTier = requiredTier;
    }

    public PickaxeTier getRequiredTier() { return requiredTier; }

    public List<Item> getValidPickaxes() {
        return requiredTier.getValidPickaxes();
    }

    public String getTierTranslationKey() {
        return requiredTier.getTranslationKey();
    }

    public boolean isValidPickaxe(Item item) {
        return requiredTier.getValidPickaxes().contains(item);
    }

    public int findBestPickaxeSlot(Inventory inventory) {
        int bestSlot = -1;
        int bestDurability = 0;
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && isValidPickaxe(stack.getItem())) {
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dur > bestDurability) {
                    bestDurability = dur;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public int getPickaxeRemainingDurability(Inventory inventory) {
        int slot = findBestPickaxeSlot(inventory);
        if (slot < 0) return 0;
        ItemStack pick = inventory.items.get(slot);
        return pick.getMaxDamage() - pick.getDamageValue();
    }

    public int getMaxCraftable(Inventory inventory) {
        int maxByMats = Integer.MAX_VALUE;
        for (Ingredient ing : getIngredients()) {
            int have = countInInventory(inventory, ing.item());
            if (ing.count() <= 0) continue;
            maxByMats = Math.min(maxByMats, have / ing.count());
        }
        int durability = getPickaxeRemainingDurability(inventory);
        return Math.min(maxByMats == Integer.MAX_VALUE ? 0 : maxByMats, durability);
    }

    public boolean canCraftBullets(Inventory inventory, int count) {
        return count > 0 && getMaxCraftable(inventory) >= count;
    }

    public boolean craftBullets(Inventory inventory, int count) {
        if (!canCraftBullets(inventory, count)) return false;

        for (Ingredient ing : getIngredients()) {
            removeFromInventory(inventory, ing.item(), ing.count() * count);
        }

        int slot = findBestPickaxeSlot(inventory);
        if (slot >= 0) {
            ItemStack pick = inventory.items.get(slot);
            int newDamage = pick.getDamageValue() + count;
            if (newDamage >= pick.getMaxDamage()) {
                inventory.items.set(slot, ItemStack.EMPTY);
                Level level = inventory.player.level();
                level.playSound(null,
                        inventory.player.getX(),
                        inventory.player.getY(),
                        inventory.player.getZ(),
                        SoundEvents.ITEM_BREAK,
                        SoundSource.PLAYERS,
                        0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                pick.setDamageValue(newDamage);
            }
        }

        int maxStack  = getResult().getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack result = new ItemStack(getResult(), batch);
            if (!inventory.add(result)) {
                inventory.player.drop(result, false);
            }
            remaining -= batch;
        }

        return true;
    }
}
