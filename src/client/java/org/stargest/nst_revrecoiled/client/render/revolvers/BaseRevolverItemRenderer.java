package org.stargest.nst_revrecoiled.client.render.revolvers;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
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
 * Key features:
 * - Vertical offset correction for third-person perspectives.
 * - Horizontal mirroring (negative X scaling) for left-hand rendering.
 * - Dynamic backface culling management to prevent visual artifacts during mirroring.
 *
 * @param <T> The revolver item type this renderer handles
 */
public class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T>{

    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Called before rendering the model to apply custom transformations and render states.
     *
     * Transformations applied:
     * - Adjusts the vertical position slightly downwards for third-person views
     *   to accurately align the revolver model with the player's hand.
     * - Applies horizontal mirroring (negative X scale) for left-hand rendering
     *   (both first and third person) to ensure the model faces the correct direction.
     * - Disables backface culling when mirroring to prevent the model from appearing
     *   inside-out due to inverted surface normals.
     *
     * @param matrixStack    The transformation matrix stack
     * @param animatable    The revolver item being rendered
     * @param model         The baked GeckoLib model
     * @param bufferSource  The vertex buffer source
     * @param buffer        The vertex consumer
     * @param isReRender    Whether this is a recursive render pass
     * @param partialTick   The partial tick time for animations
     * @param packedLight   Packed light coordinates
     * @param packedOverlay Packed overlay coordinates
     * @param colour        The base render color
     */
    @Override
    public void preRender(MatrixStack matrixStack, BaseRevolverItem animatable, BakedGeoModel model,
                          @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND ||
                this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            matrixStack.translate(0, -0.22, 0);
        }

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            matrixStack.translate(0.5, 0, 0);
            matrixStack.scale(-1, 1, 1);
            matrixStack.translate(-0.5, 0, 0);
            RenderSystem.disableCull();
        }

        super.preRender(matrixStack, (T) animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /**
     * Called after rendering the model to restore global render states.
     * Re-enables backface culling if it was disabled during the {@link #preRender} pass
     * for left-hand mirroring.
     *
     * @param matrixStack    The transformation matrix stack
     * @param animatable    The revolver item that was rendered
     * @param model         The baked GeckoLib model
     * @param bufferSource  The vertex buffer source
     * @param buffer        The vertex consumer
     * @param isReRender    Whether this is a recursive render pass
     * @param partialTick   The partial tick time for animations
     * @param packedLight   Packed light coordinates
     * @param packedOverlay Packed overlay coordinates
     * @param colour        The base render color
     */
    @Override
    public void postRender(MatrixStack matrixStack, BaseRevolverItem animatable, BakedGeoModel model,
                           VertexConsumerProvider bufferSource, VertexConsumer buffer,
                           boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            RenderSystem.enableCull();
        }

        super.postRender(matrixStack, (T) animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
