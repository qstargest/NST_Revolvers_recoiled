package org.stargest.nst_revrecoiled.client.render.entity.villager;

/**
 * Mixin accessor interface that extends VillagerEntityRenderState with
 * a revolvermaker profession flag.
 *
 * Implemented by VillagerEntityRenderStateMixin via @Mixin + @Unique injection.
 * The nst$ prefix follows the mod's namespace convention to avoid collisions
 * with other mods targeting the same render state class.
 *
 * This interface decouples profession state propagation from entity lookups:
 * ModVillagerRenderer writes the flag once per frame in updateRenderState(),
 * and downstream consumers (getTexture, feature renderers) read it cheaply
 * without touching VillagerEntity or VillagerData.
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
}
