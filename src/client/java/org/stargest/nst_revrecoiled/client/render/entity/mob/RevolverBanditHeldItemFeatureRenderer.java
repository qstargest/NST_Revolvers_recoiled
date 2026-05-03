package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;

/**
 * Feature renderer for items held by the Revolver Bandit.
 * In 1.21.4, vanilla's ArmedEntityRenderState refactor makes direct item tracking
 * complex. This custom feature renderer reads an ItemStack directly from our
 * RevolverBanditRenderState, ensuring compatibility with GeckoLib's 3D models.
 */
public class RevolverBanditHeldItemFeatureRenderer extends FeatureRenderer<RevolverBanditEntity, RevolverBanditModel> {

    public RevolverBanditHeldItemFeatureRenderer(FeatureRendererContext<RevolverBanditEntity, RevolverBanditModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light,
                       RevolverBanditEntity entity,
                       float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {

        ItemStack stack = entity.getMainHandStack();
        if (stack.isEmpty()) return;

        matrices.push();

        // 1. Position the item in the right hand
        // We call the model's setArmAngle to ensure the item follows the animated arm
        this.getContextModel().setArmAngle(Arm.RIGHT, matrices);

        // 2. Adjust for third-person held item positioning
        // Biped arms are rotated, so we need to compensate to make the weapon point forward
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // Translation offsets to place the handle in the palm (Matches vanilla HeldItemFeatureRenderer)
        matrices.translate(0.0625f, 0.125f, -0.625f);

        // 3. Render the item
        // GeckoLib items will automatically trigger their 3D renderer here
        // because we use THIRD_PERSON_RIGHT_HAND mode.
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                null,
                stack,
                ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,
                false,
                matrices,
                vertexConsumers,
                null,
                light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                0
        );

        matrices.pop();
    }
}
