package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.stargest.nst_revrecoiled.client.render.entity.villager.VillagerRenderStateAccess;

/**
 * Injects the revolvermaker profession flag into VillagerEntityRenderState.
 * Implements VillagerRenderStateAccess to expose the flag to the renderer
 * and feature renderer mixin without requiring reflection or extra maps.
 *
 * The @Unique field is added directly to the render state object, keeping
 * profession state co-located with the rest of the render data.
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

        @Override
        public boolean nst$isRevolvermaker() { return nst$isRevolvermaker; }

        @Override
        public void nst$setIsRevolvermaker(boolean value) { this.nst$isRevolvermaker = value; }
}
