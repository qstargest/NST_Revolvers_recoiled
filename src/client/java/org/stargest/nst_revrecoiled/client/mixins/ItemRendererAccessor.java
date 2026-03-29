package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for ItemRenderer to expose private rendering methods.
 * Used by BaseRevolverItemRenderer to manually render 2D flat models
 * for revolvers in GUI and item frame contexts.
 */
@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Invoker("renderBakedItemModel")
    void invokeRenderBakedItemModel(BakedModel model, ItemStack stack,
                                    int light, int overlay,
                                    MatrixStack matrices, VertexConsumer vertices);
}
