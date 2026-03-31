package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Suppresses the vanilla profession clothing overlay for revolvermaker villagers.
 * Cancels VillagerClothingFeatureRenderer.render() entirely when the render state
 * identify as a revolvermaker, preventing the default profession hat/apron from
 * being drawn on top of the custom revolvermaker texture.
 *
 * The check is performed at HEAD so that no vanilla rendering work is done
 * before cancellation, keeping the mixin as lightweight as possible.
 */
@Mixin(VillagerClothingFeatureRenderer.class)
public class VillagerClothingFeatureRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Entity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof VillagerEntity villager
                && villager.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER) {
            ci.cancel();
            return;
        }
        if (entity instanceof ZombieVillagerEntity zombie
                && zombie.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER) {
            ci.cancel();
        }
    }
}
