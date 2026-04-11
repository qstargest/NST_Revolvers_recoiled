package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.world.item.ItemStack;

/**
 * Mixin accessor interface that extends VillagerEntityRenderState with
 * revolvermaker profession state and held item data.
 *
 * Implemented by VillagerEntityRenderStateMixin via @Mixin + @Unique injection.
 * The nst$ prefix follows the mod's namespace convention to avoid collisions
 * with other mods targeting the same render state class.
 *
 * This interface decouples render-time data propagation from entity lookups:
 * ModVillagerRenderer writes both fields once per frame in updateRenderState(),
 * and downstream consumers (getTexture, feature renderers) read them cheaply
 * without touching VillagerEntity or VillagerData.
 *
 * The held item is stored here rather than re-queried in the feature renderer
 * because feature renderers only receive the render state, not the entity.
*/
public interface VillagerRenderStateAccess {
     /**
     * Returns {@code true} if this render state belongs to a revolvermaker villager.
     */
    boolean nst$isRevolvermaker();
     /**
     * Sets the revolvermaker flag on this render state.
     * Called by ModVillagerRenderer.updateRenderState() each frame.
     *
     * @param value {@code true} if the villager is a revolvermaker
     */
    void nst$setIsRevolvermaker(boolean value);
     /**
     * Stores the villager's main hand item stack into the render state.
     * Called by ModVillagerRenderer.updateRenderState() each frame so that
     * ModVillagerHeldItemFeatureRenderer can inspect the item type at render time.
     *
     * @param stack The item stack currently held in the villager's main hand
     */
    void nst$setHeldItem(ItemStack stack);
     /**
     * Returns the item stack stored in this render state's main hand slot.
     * Used by ModVillagerHeldItemFeatureRenderer to detect revolvers and
     * switch to 2D flat rendering instead of the GeckoLib 3D model.
     */
    ItemStack nst$getHeldItem();
}