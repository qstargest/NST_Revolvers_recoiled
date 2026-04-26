package org.stargest.nst_revrecoiled.client.render.revolvers;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Base renderer for all revolvers.
 * Standard GeckoLib renderer that handles the 2D icon / 3D model switch.
 * For GUI and FIXED contexts, model swapping is handled externally via RevolverModelMixin.
 * Handles specialized transformations for different render perspectives,
 * specifically adjusting the third-person held position and left-hand mirroring.
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Called before rendering the model to apply custom transformations and render states.
     *
     * Transformations applied:
     * - Adjusts the vertical position slightly downwards for third-person views
     *   to accurately align the revolver model with the player's hand.
     * - Applies horizontal mirroring for left-hand rendering (both first and third person)
     *   to ensure the model faces the correct direction.
     * - Uses a position-only mirroring technique (scaling only the position matrix)
     *   to preserve the original normal matrix, maintaining correct lighting and
     *   avoiding visual artifacts without needing to modify the culling state.
     *
     * @param matrixStack    The transformation matrix stack
     * @param animatable     The revolver item being rendered
     * @param model          The baked GeckoLib model
     * @param bufferSource   The vertex buffer source
     * @param buffer         The vertex consumer
     * @param isReRender     Whether this is a recursive render pass
     * @param partialTick    The partial tick time for animations
     * @param packedLight    Packed light coordinates
     * @param packedOverlay  Packed overlay coordinates
     * @param red            Red color component
     * @param green          Green color component
     * @param blue           Blue color component
     * @param alpha          Alpha transparency component
     */
    @Override
    public void preRender(MatrixStack matrixStack, BaseRevolverItem animatable, BakedGeoModel model,
                          @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND ||
                this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            matrixStack.translate(0, -0.22, 0);
        }

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            // Mirror geometry around the item centre (X = 0.5 in model space).
            // Translations do not affect the normal matrix, and we deliberately scale
            // only the position matrix so the normal matrix is preserved unchanged.
            matrixStack.translate(0.5, 0, 0);
            matrixStack.peek().getPositionMatrix().scale(-1, 1, 1);
            matrixStack.translate(-0.5, 0, 0);
        }

        super.preRender(matrixStack, (T) animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode displayContext, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        // Redundant icon rendering logic removed:
        // Model swapping is now handled by RevolverModelMixin in ItemRenderer.renderItem
        // for GUI and FIXED contexts.
        super.render(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
