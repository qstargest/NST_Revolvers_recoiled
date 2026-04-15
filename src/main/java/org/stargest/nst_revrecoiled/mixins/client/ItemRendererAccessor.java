package org.stargest.nst_revrecoiled.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for ItemRenderer to expose private rendering methods.
 * Used by BaseRevolverItemRenderer to manually render 2D flat models.
 * Adapted for Forge 1.20.1.
 */
@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Invoker("renderModelLists")
    void invokeRenderBakedItemModel(BakedModel model, ItemStack stack,
                                    int light, int overlay,
                                    PoseStack matrices, VertexConsumer vertices);
}
