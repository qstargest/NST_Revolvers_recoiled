package org.stargest.nst_revrecoiled.client.render.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;

/**
 * Feature renderer for items held by the Revolver Bandit.
 * Reads the ItemStack directly from RevolverBanditRenderState for compatibility
 * with GeckoLib's 3D model pipeline.
 */
public class RevolverBanditHeldItemFeatureRenderer
        extends RenderLayer<RevolverBanditEntity, RevolverBanditModel> {

    public RevolverBanditHeldItemFeatureRenderer(
            LivingEntityRenderer<RevolverBanditEntity, RevolverBanditModel> context) {
        super(context);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light,
                       RevolverBanditEntity state, float var5, float var6, float var7, float var8, float var9, float var10) {

        ItemStack stack = state.getMainHandItem();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        // Translate to the right arm's position in model space
        getParentModel().rightArm.translateAndRotate(poseStack);

        // Compensate for biped arm rotation to make weapon point forward
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // Position the handle in the palm (matches vanilla HeldItemRenderer offsets)
        poseStack.translate(0.0625f, 0.125f, -0.625f);

        // Render item — GeckoLib items use their own renderer via GeoRenderProvider
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                null,
                0
        );

        poseStack.popPose();
    }
}