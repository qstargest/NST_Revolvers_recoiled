package org.stargest.nst_revrecoiled.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

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
@Mixin(VillagerProfessionLayer.class)
public class VillagerClothingFeatureRendererMixin {

    /**
     * Cancels clothing layer rendering for revolvermaker villagers.
     * The generic S is erased to LivingEntityRenderState in the .class descriptor,
     * so the method signature must match the erased form for Mixin injection.
     *
     * @param ci CallbackInfo used to cancel the render call
     */
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(PoseStack poseStack, MultiBufferSource bufferSource,
                          int light, LivingEntityRenderState state,
                          float limbAngle, float limbDistance, CallbackInfo ci) {
        
        if (state instanceof VillagerRenderState villagerState) {
            if (villagerState.villagerData != null && 
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerState.villagerData.getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
                ci.cancel();
            }
        } else if (state instanceof ZombieVillagerRenderState zombieState) {
            if (zombieState.villagerData != null && 
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(zombieState.villagerData.getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
                ci.cancel();
            }
        }
    }
}
