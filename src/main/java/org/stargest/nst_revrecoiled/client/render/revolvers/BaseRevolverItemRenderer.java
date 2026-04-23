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
 * For GUI and FIXED contexts, it renders a high-quality 2D flat model using
 * Handles specialized transformations for different render perspectives,
 * specifically adjusting the third-person held position.
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Called before rendering the model to apply custom transformations.
     * Adjusts the vertical position slightly downwards for third-person views
     * to accurately align the model with the player's hand.
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
