package org.stargest.nst_revrecoiled.client.render.revolvers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Custom GeckoLib item renderer for Revolver items.
 * Handles specialized transformations for different render perspectives,
 * ensuring accurate model alignment and correct left-hand mirroring.
 *
 * <p>Key features:
 * <ul>
 *   <li>Vertical offset correction for third-person perspectives.</li>
 *   <li>Horizontal mirroring (negative X scaling) for left-hand rendering.</li>
 *   <li>Dynamic backface culling management to prevent visual artifacts during mirroring.</li>
 * </ul>
 *
 * @param <T> The revolver item type this renderer handles
 */
public class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {

    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Called before rendering the model to apply custom transformations and render states.
     *
     * <p>Transformations applied:
     * <ul>
     *   <li>Adjusts the vertical position slightly downwards for third-person views
     *       to accurately align the revolver model with the player's hand.</li>
     *   <li>Applies horizontal mirroring (negative X scale) for left-hand rendering
     *       to ensure the model faces the correct direction.</li>
     *   <li>Disables backface culling when mirroring to prevent the model from appearing
     *       inside-out due to inverted surface normals.</li>
     * </ul>
     */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {

        if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(0, -0.22, 0);
        }

        if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.translate(0.5, 0, 0);
            poseStack.scale(-1, 1, 1);
            poseStack.translate(-0.5, 0, 0);
            RenderSystem.disableCull();
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /**
     * Called after rendering the model to restore global render states.
     * Re-enables backface culling if it was disabled during {@link #preRender} for left-hand mirroring.
     */
    @Override
    public void postRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer,
                           boolean isReRender, float partialTick, int packedLight,
                           int packedOverlay, int colour) {

        if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            RenderSystem.enableCull();
        }

        super.postRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
