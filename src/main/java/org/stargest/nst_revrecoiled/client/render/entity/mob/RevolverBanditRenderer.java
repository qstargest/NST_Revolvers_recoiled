package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.client.util.ModEntityModelLayers;

/**
 * Renderer for the Revolver Bandit entity.
 * Uses a custom model and state management to handle 3D revolver rendering
 * and specialized arm poses.
 */
public class RevolverBanditRenderer extends MobEntityRenderer<
        RevolverBanditEntity,
        RevolverBanditModel> {

    /** Texture path mirrors vanilla pillager. */
    private static final Identifier TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/mob/pillager.png");

    public RevolverBanditRenderer(EntityRendererFactory.Context context) {
        super(context,
                new RevolverBanditModel(context.getPart(ModEntityModelLayers.REVOLVER_BANDIT)),
                0.5f); // shadow radius

        // Use custom feature renderer to handle 3D revolvers and bypass 1.21.x ItemRenderState complexity
        this.addFeature(new RevolverBanditHeldItemFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(RevolverBanditEntity bandit) {
        return TEXTURE;
    }
}
