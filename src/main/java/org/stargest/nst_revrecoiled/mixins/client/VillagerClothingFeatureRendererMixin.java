package org.stargest.nst_revrecoiled.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Suppresses the vanilla profession clothing overlay for revolvermaker villagers.
 * Adapted for Forge 1.20.1.
 */
@Mixin(VillagerProfessionLayer.class)
public class VillagerClothingFeatureRendererMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(PoseStack matrices, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
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
