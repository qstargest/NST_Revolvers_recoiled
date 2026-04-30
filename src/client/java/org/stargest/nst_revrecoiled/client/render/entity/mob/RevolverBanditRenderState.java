package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.ItemStack;


/**
 * Render state for the Revolver Bandit.
 * Passes essential data from the entity to the model and feature renderers,
 * including entity ID for animation caching and the currently held item.
 */
public class RevolverBanditRenderState extends BipedEntityRenderState {
    /** The entity's numeric ID, used for state caching in PlayerArmPose. */
    public int id;
    
    /** Held item stack, read by RevolverBanditHeldItemFeatureRenderer to support 3D models. */
    public ItemStack heldItem = ItemStack.EMPTY;
}
