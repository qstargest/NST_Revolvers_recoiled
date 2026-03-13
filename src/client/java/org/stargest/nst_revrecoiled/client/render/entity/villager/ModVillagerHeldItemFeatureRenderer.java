package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHat;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Feature renderer for items held by villagers.
 * Fixes incorrect 3D GeckoLib model rendering when a villager holds a revolver
 * during the trade offer UI — replaces it with a standard 2D flat item render.
 *
 * Problem: GeckoLib registers a custom item renderer for BaseRevolverItem.
 * When vanilla VillagerHeldItemFeatureRenderer calls ItemRenderer with
 * ModelTransformationMode.GROUND or NONE, GeckoLib intercepts the call and
 * renders the full animated 3D model instead of the flat 2D sprite.
 *
 * Fix: Detect revolver items in the render state and force
 * ModelTransformationMode.FIXED, which maps to the "fixed" display entry in
 * the item JSON model. This bypasses GeckoLib's renderer and produces the
 * correct 2D appearance matching other held items in the villager's hand.
 * All non-revolver items fall through to vanilla super.render() unchanged.
 *
 * The held item is read from VillagerRenderStateAccess rather than queried
 * directly from the entity, since feature renderers only receive render state.
 * ModVillagerRenderer.updateRenderState() populates the field each frame.
 */
@Environment(EnvType.CLIENT)
public class ModVillagerHeldItemFeatureRenderer<S extends VillagerEntityRenderState, M extends EntityModel<S> & ModelWithHat>
        extends VillagerHeldItemFeatureRenderer<S, M> {

    public ModVillagerHeldItemFeatureRenderer(FeatureRendererContext<S, M> context) {
        super(context);
    }

    /**
     * Renders the item held by the villager.
     * For revolvers: applies the same model transforms as the vanilla implementation
     * but forces FIXED transformation mode to obtain a 2D flat render,
     * bypassing GeckoLib's item renderer override.
     * For all other items: delegates to vanilla super.render() unchanged.
     */
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, S state, float limbAngle, float limbDistance) {

        ItemStack heldItem = ((VillagerRenderStateAccess) state).nst$getHeldItem();

        if (!heldItem.isEmpty() && heldItem.getItem() instanceof BaseRevolverItem) {
            matrices.push();
            this.applyTransforms(state, matrices);
            matrices.scale(0.5f, 0.5f, 0.5f);

            // FIXED mode maps to the "fixed" display entry in the item JSON,
            // producing a 2D sprite and bypassing GeckoLib's 3D renderer
            MinecraftClient.getInstance().getItemRenderer().renderItem(
                    null,                             // entity — not needed for flat render
                    heldItem,
                    ModelTransformationMode.FIXED,
                    false,
                    matrices,
                    vertexConsumers,
                    null,                             // world — not needed for flat render
                    light,
                    OverlayTexture.DEFAULT_UV,
                    0
            );

            matrices.pop();
        } else {
            super.render(matrices, vertexConsumers, light, state, limbAngle, limbDistance);
        }
    }
}
