package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.stargest.nst_revrecoiled.client.render.entity.villager.VillagerRenderStateAccess;

/**
 * Injects the revolvermaker profession flag and held item field into
 * VillagerEntityRenderState. Implements VillagerRenderStateAccess to expose
 * both fields to the renderer and feature renderer without requiring
 * reflection or external maps.
 *
 * Both fields are added directly to the render state object, keeping
 * all per-villager render data co-located in a single place.
 * The nst$ prefix guards against field name collisions with other mods.
 */
@Mixin(VillagerEntityRenderState.class)
public class VillagerEntityRenderStateMixin implements VillagerRenderStateAccess {

        /**
         * Profession flag injected into the render state.
         * Defaults to {@code false}; set each frame by ModVillagerRenderer.updateRenderState().
         */
        @Unique
        private boolean nst$isRevolvermaker = false;

        /**
         * Held item injected into the render state.
         * Defaults to empty; set each frame by ModVillagerRenderer.updateRenderState().
         * Read by ModVillagerHeldItemFeatureRenderer to detect revolvers at render time.
         */
        @Unique
        private ItemStack heldItem = ItemStack.EMPTY;

        @Override
        public boolean nst$isRevolvermaker() { return nst$isRevolvermaker; }

        @Override
        public void nst$setIsRevolvermaker(boolean value) { this.nst$isRevolvermaker = value; }

        @Override
        public void nst$setHeldItem(ItemStack s) { this.heldItem = s; }

        @Override
        public ItemStack nst$getHeldItem() { return this.heldItem; }
}
