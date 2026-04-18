package org.stargest.nst_revrecoiled.client.render.revolvers;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.mixins.client.ItemRendererAccessor;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Base renderer for all revolvers.
 * Standard GeckoLib renderer that handles the 2D icon / 3D model switch.
 * For GUI and FIXED contexts, it renders a high-quality 2D flat model using
 * the ItemRendererAccessor to bypass GeckoLib's default 3D rendering.
 *
 * Adapted for Forge 1.20.1.
 */
public class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
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
