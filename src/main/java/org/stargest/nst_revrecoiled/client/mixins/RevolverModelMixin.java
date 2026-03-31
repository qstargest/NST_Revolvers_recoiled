package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Mixin to ItemRenderer to swap the 3D revolver model for a 2D icon model
 * in GUI and Item Frame (FIXED) contexts.
 *
 * This avoids the "double transformation" issue where the 3D model's transforms
 * and the icon's transforms were both being applied, leading to orbital shifts
 * and incorrect Z-depth in item frames.
 */
@Mixin(ItemRenderer.class)
public abstract class RevolverModelMixin {
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BakedModel nst_revrecoiled$swapRevolverModel(BakedModel model, ItemStack stack, ModelTransformationMode renderMode) {
        if (stack.getItem() instanceof BaseRevolverItem && (renderMode == ModelTransformationMode.GUI || renderMode == ModelTransformationMode.FIXED)) {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            // Look for the placeholder item that holds the 2D flat model (ID + "_item")
            Item iconItem = Registries.ITEM.get(new Identifier(itemId.getNamespace(), itemId.getPath() + "_item"));

            if (iconItem != Items.AIR) {
                // Intercept and swap the BakedModel before any transformations are applied.
                // This ensures Minecraft only applies the 2D model's own transformations to the slot matrix.
                return MinecraftClient.getInstance().getItemRenderer().getModels().getModel(iconItem);
            }
        }
        return model;
    }
}
