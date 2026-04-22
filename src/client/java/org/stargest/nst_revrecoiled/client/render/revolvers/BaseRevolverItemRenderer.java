package org.stargest.nst_revrecoiled.client.render.revolvers;

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
 * specifically adjusting the third-person held position.
 *
 * @param <T> The revolver item type this renderer handles
 */
public class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T>{

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
                          int packedOverlay, int colour) {

        if (this.renderPerspective == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND ||
                this.renderPerspective == ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            matrixStack.translate(0, -0.22, 0);
        }

        super.preRender(matrixStack, (T) animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
