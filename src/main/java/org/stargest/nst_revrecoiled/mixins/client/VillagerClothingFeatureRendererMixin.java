package org.stargest.nst_revrecoiled.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
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
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(PoseStack matrices, MultiBufferSource vertexConsumers, int light, Entity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Villager villager
                && BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
            ci.cancel();
            return;
        }
        if (entity instanceof ZombieVillager zombie
                && BuiltInRegistries.VILLAGER_PROFESSION.getKey(zombie.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
            ci.cancel();
        }
    }
}
