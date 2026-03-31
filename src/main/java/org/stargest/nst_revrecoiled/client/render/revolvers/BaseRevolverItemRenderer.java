package org.stargest.nst_revrecoiled.client.render.revolvers;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Base renderer for all revolvers.
 * Standard GeckoLib renderer that handles the 2D icon / 3D model switch.
 * For GUI and FIXED contexts, it renders a high-quality 2D flat model using
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
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
