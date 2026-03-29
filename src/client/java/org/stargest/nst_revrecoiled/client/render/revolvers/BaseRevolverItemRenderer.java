package org.stargest.nst_revrecoiled.client.render.revolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import org.stargest.nst_revrecoiled.client.mixins.ItemRendererAccessor;

/**
 * Base renderer for all revolvers.
 * Standard GeckoLib renderer that handles the 2D icon / 3D model switch.
 * For GUI and FIXED contexts, it renders a high-quality 2D flat model using
 * the ItemRendererAccessor to bypass GeckoLib's default 3D rendering.
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {
    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode displayContext, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ModelTransformationMode.FIXED ||
                displayContext == ModelTransformationMode.GUI) {

            MinecraftClient client = MinecraftClient.getInstance();
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            ModelIdentifier modelId = new ModelIdentifier(
                    Identifier.of(itemId.getNamespace(), itemId.getPath() + "_item"),
                    "inventory"
            );

            BakedModel flatModel = client.getBakedModelManager().getModel(modelId);

            if (flatModel != null && flatModel != client.getBakedModelManager().getMissingModel()) {
                if (displayContext == ModelTransformationMode.GUI) {
                    DiffuseLighting.enableGuiDepthLighting();
                    ((ItemRendererAccessor) client.getItemRenderer()).invokeRenderBakedItemModel(
                            flatModel,
                            stack,
                            packedLight,
                            packedOverlay,
                            poseStack,
                            bufferSource.getBuffer(RenderLayer.getTranslucent())
                    );
                } else {
                    ((ItemRendererAccessor) client.getItemRenderer()).invokeRenderBakedItemModel(
                            flatModel,
                            stack,
                            packedLight,
                            packedOverlay,
                            poseStack,
                            bufferSource.getBuffer(RenderLayer.getCutout())
                    );
                }
                return;
            }
        }

        super.render(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
