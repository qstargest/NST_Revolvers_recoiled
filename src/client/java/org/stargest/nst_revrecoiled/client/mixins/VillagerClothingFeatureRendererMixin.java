package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.client.render.entity.villager.VillagerRenderStateAccess;

/**
 * Suppresses the vanilla profession clothing overlay for revolvermaker villagers.
 * Cancels VillagerClothingFeatureRenderer.render() entirely when the render state
 * identifies a revolvermaker, preventing the default profession hat/apron from
 * being drawn on top of the custom revolvermaker texture.
 *
 * Injection target uses the erased LivingEntityRenderState descriptor because
 * the generic type parameter S is erased to LivingEntityRenderState in bytecode —
 * matching the compiled descriptor is required for Mixin to locate the method.
 *
 * The check is performed at HEAD so that no vanilla rendering work is done
 * before cancellation, keeping the mixin as lightweight as possible.
 */
@Mixin(VillagerClothingFeatureRenderer.class)
public class VillagerClothingFeatureRendererMixin {

    /**
     * Cancels clothing layer rendering for revolvermaker villagers.
     * The generic S is erased to LivingEntityRenderState in the .class descriptor,
     * so the method signature must match the erased form for Mixin injection.
     *
     * @param ci CallbackInfo used to cancel the render call
     */
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                          int light, LivingEntityRenderState state,
                          float limbAngle, float limbDistance, CallbackInfo ci) {
        if (state instanceof VillagerEntityRenderState villagerState
                && ((VillagerRenderStateAccess) villagerState).nst$isRevolvermaker()) {
            ci.cancel();
        }
    }
}
