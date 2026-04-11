package org.stargest.nst_revrecoiled.client.render.entity.villager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Feature renderer for items held by villagers.
 * Fixes incorrect 3D GeckoLib model rendering when a villager holds a revolver
 * during the trade offer UI — replaces it with a standard 2D flat item render.
 *
 * Problem: GeckoLib registers a custom item renderer for BaseRevolverItem.
 * When CrossedArmsItemLayer calls ItemRenderer with ItemDisplayContext.GROUND
 * or NONE, GeckoLib intercepts the call and renders the full animated 3D model
 * instead of the flat 2D sprite.
 *
 * Fix: Detect revolver items in the render state and force
 * ItemDisplayContext.FIXED, which maps to the "fixed" display entry in
 * the item JSON model. This bypasses GeckoLib's renderer and produces the
 * correct 2D appearance matching other held items in the villager's hand.
 * All non-revolver items fall through to super.render() unchanged.
 *
 * The body transforms from CrossedArmsItemLayer.render() are replicated
 * manually via getParentModel().body.translateAndRotate() — without this
 * the item renders at the entity origin and is invisible, which is the same
 * issue as missing applyTransforms() in the Fabric port.
*/
public class ModVillagerHeldItemFeatureRenderer
        extends CrossedArmsItemLayer<VillagerRenderState, VillagerModel> {

    public ModVillagerHeldItemFeatureRenderer(
            RenderLayerParent<VillagerRenderState, VillagerModel> context) {
        super(context);
    }

    /**
     * Renders the item held by the villager.
     *
     * For revolvers: manually replicates CrossedArmsItemLayer's body transforms
     * (equivalent to VillagerHeldItemFeatureRenderer.applyTransforms() in the
     * Fabric port) and then renders with ItemDisplayContext.FIXED to obtain a
     * 2D flat sprite, bypassing GeckoLib's 3D renderer.
     *
     * For all other items: delegates to super.render() unchanged, so vanilla
     * and GeckoLib items not specifically handled here continue to work normally.
     */
    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int light, @NotNull VillagerRenderState state,
                       float limbAngle, float limbDistance) {

        VillagerRenderStateAccess access = (VillagerRenderStateAccess) state;
        ItemStack heldItem = access.nst$getHeldItem();

        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof BaseRevolverItem)) {
            super.render(poseStack, bufferSource, light, state, limbAngle, limbDistance);
            return;
        }

        poseStack.pushPose();
        try {
            this.getParentModel().root().getChild("body").translateAndRotate(poseStack);

            poseStack.translate(0.0F, 0.35F, -0.45F);

            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            poseStack.scale(0.5f, 0.5f, 0.5f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    heldItem,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    null,
                    0
            );
        } finally {
            poseStack.popPose();
        }
    }
}