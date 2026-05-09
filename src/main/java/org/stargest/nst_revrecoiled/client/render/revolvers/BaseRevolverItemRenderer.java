package org.stargest.nst_revrecoiled.client.render.revolvers;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.mixins.client.ItemRendererAccessor;
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
public class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Called before rendering the model to apply custom transformations and render states.
     *
     * Transformations applied:
     * - Adjusts the vertical position slightly downwards for third-person views
     * - to accurately align the revolver model with the player's hand.
     * - Applies horizontal mirroring (negative X scale) for left-hand rendering
     * - to ensure the model faces the correct direction.
     * - Disables backface culling when mirroring to prevent the model from appearing
     * inside-out due to inverted surface normals.
     */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {

        if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(0, -0.22, 0);
        }

        if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                this.renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.translate(0.5, 0, 0);
            poseStack.last().pose().scale(-1, 1, 1);
            poseStack.translate(-0.5, 0, 0);
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.FIXED ||
                displayContext == ItemDisplayContext.GUI) {

            Minecraft client = Minecraft.getInstance();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ResourceLocation modelLocation = new ResourceLocation(itemId.getNamespace(), itemId.getPath() + "_item");
            ModelResourceLocation modelId = new ModelResourceLocation(modelLocation, "inventory");

            BakedModel flatModel = client.getModelManager().getModel(modelId);

            if (flatModel != null && flatModel != client.getModelManager().getMissingModel()) {
                if (displayContext == ItemDisplayContext.GUI) {
                    Lighting.setupForFlatItems();
                    ((ItemRendererAccessor) client.getItemRenderer()).invokeRenderBakedItemModel(
                            flatModel,
                            stack,
                            packedLight,
                            packedOverlay,
                            poseStack,
                            bufferSource.getBuffer(RenderType.translucent())
                    );
                } else {
                    ((ItemRendererAccessor) client.getItemRenderer()).invokeRenderBakedItemModel(
                            flatModel,
                            stack,
                            packedLight,
                            packedOverlay,
                            poseStack,
                            bufferSource.getBuffer(RenderType.cutout())
                    );
                }
                return;
            }
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
