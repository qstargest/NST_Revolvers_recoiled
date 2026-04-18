package org.stargest.nst_revrecoiled.client.render.entity.villager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
 * manually via getParentModel().root().getChild("body").translateAndRotate(poseStack)
 * — without this the item renders at the entity origin and is invisible.
 *
 * Adapted for Forge 1.20.1.
 */
public class ModVillagerHeldItemFeatureRenderer<T extends LivingEntity, M extends VillagerModel<T>>
        extends CrossedArmsItemLayer<T, M> {

    public ModVillagerHeldItemFeatureRenderer(RenderLayerParent<T, M> context, net.minecraft.client.renderer.ItemInHandRenderer itemInHandRenderer) {
        super(context, itemInHandRenderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int light, @NotNull T entity,
                       float limbAngle, float limbDistance, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack heldItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);

        if (!heldItem.isEmpty() && heldItem.getItem() instanceof BaseRevolverItem) {
            poseStack.pushPose();

            this.getParentModel().root().getChild("body").translateAndRotate(poseStack);

            poseStack.translate(0.0f, 0.4f, -0.4f);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            poseStack.scale(0.5f, 0.5f, 0.5f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    heldItem,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    entity.level(),
                    entity.getId()
            );

            poseStack.popPose();
        } else {
            super.render(poseStack, bufferSource, light, entity,
                    limbAngle, limbDistance, partialTick,
                    ageInTicks, netHeadYaw, headPitch);
        }
    }
}
