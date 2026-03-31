package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
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
 * Fix: Detect revolver items and force ModelTransformationMode.FIXED, 
 * which maps to the "fixed" display entry in the item JSON model. 
 * This bypasses GeckoLib's renderer and produces the correct 2D appearance 
 * matching other held items in the villager's hand.
 * All non-revolver items fall through to vanilla super.render() unchanged.
 */
@Environment(EnvType.CLIENT)
public class ModVillagerHeldItemFeatureRenderer<T extends VillagerEntity, M extends EntityModel<T>>
        extends VillagerHeldItemFeatureRenderer<T, M> {

    public ModVillagerHeldItemFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context, MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer());
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, T entity,
                       float limbAngle, float limbDistance,
                       float tickDelta, float customAngle,
                       float headYaw, float headPitch) {

        ItemStack heldItem = entity.getEquippedStack(EquipmentSlot.MAINHAND);

        if (!heldItem.isEmpty() && heldItem.getItem() instanceof BaseRevolverItem) {
            matrices.push();
            matrices.translate(0.0f, 0.4f, -0.4f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
            matrices.scale(0.5f, 0.5f, 0.5f);

            MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer().renderItem(
                    entity,
                    heldItem,
                    ModelTransformationMode.FIXED,
                    false,
                    matrices,
                    vertexConsumers,
                    light
            );

            matrices.pop();
        } else {
            super.render(matrices, vertexConsumers, light, entity,
                    limbAngle, limbDistance, tickDelta, customAngle, headYaw, headPitch);
        }
    }
}