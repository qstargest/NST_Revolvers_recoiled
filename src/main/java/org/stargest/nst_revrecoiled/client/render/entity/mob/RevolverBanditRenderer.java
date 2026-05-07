package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.client.util.ModEntityModelLayers;

/**
 * Renderer for the Revolver Bandit entity.
 * Uses a custom model and render state to handle revolver rendering
 * and specialized arm poses.
 */
public class RevolverBanditRenderer extends MobRenderer<
        RevolverBanditEntity, RevolverBanditModel> {

    /** Texture path mirrors vanilla pillager. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "textures/entity/mob/pillager.png");

    public RevolverBanditRenderer(EntityRendererProvider.Context context) {
        super(context,
                new RevolverBanditModel(context.bakeLayer(ModEntityModelLayers.REVOLVER_BANDIT)),
                0.5f); // shadow radius

        // Custom feature renderer for 3D revolver items
        this.addLayer(new RevolverBanditHeldItemFeatureRenderer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull RevolverBanditEntity bandit) {
        return TEXTURE;
    }
}
