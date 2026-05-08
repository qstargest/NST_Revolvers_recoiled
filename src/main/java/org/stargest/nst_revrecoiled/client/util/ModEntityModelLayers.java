package org.stargest.nst_revrecoiled.client.util;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * Defines custom entity model layer identifiers.
 * Registration of layer definitions is done via
 * EntityRenderersEvent.RegisterLayerDefinitions in org.stargest.nst_revrecoiled.client.ClientEvents.
 */
public final class ModEntityModelLayers {

    /** Model layer for the Revolver Bandit entity. */
    public static final ModelLayerLocation REVOLVER_BANDIT =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "revolver_bandit"),
                    "main");

    private ModEntityModelLayers() {}
}
