package org.stargest.nst_revrecoiled.mixins.client;

import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.stargest.nst_revrecoiled.client.render.entity.zombie_villager.ZombieVillagerRenderStateAccess;

/**
 * Injects the revolvermaker profession flag into ZombieVillagerEntityRenderState.
 * Implements ZombieVillagerRenderStateAccess to expose the field to the renderer
 * and the clothing feature renderer mixin without reflection.
 *
 * Held item is not stored here because ZombieVillagerEntityRenderer does not
 * add a held item feature renderer — zombie villagers don't display trade items.
 */
@Mixin(ZombieVillagerRenderState.class)
public class ZombieVillagerRenderStateMixin implements ZombieVillagerRenderStateAccess {

    @Unique
    private boolean nst$isRevolvermaker = false;

    @Override
    public boolean nst$isRevolvermaker() {
        return nst$isRevolvermaker;
    }

    @Override
    public void nst$setIsRevolvermaker(boolean value) {
        this.nst$isRevolvermaker = value;
    }
}
