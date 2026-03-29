package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;

/**
 * Mixin to apply custom arm poses for revolver weapons.
 * Injects into the setAngles method to modify arm positions during rendering.
 * Compatible with Minecraft 1.21.1.
 *
 * This mixin runs after all vanilla arm positioning logic,
 * allowing revolver-specific poses to override default animations.
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<AbstractClientPlayerEntity> {

    @Final
    @Shadow
    public ModelPart rightSleeve;
    @Final
    @Shadow public ModelPart leftSleeve;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void applyRevolverArmPose(LivingEntity entity, float limbAngle, float limbDistance,
                                      float animationProgress, float headYaw, float headPitch,
                                      CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        PlayerArmPose.applyRevolverPose(this, player);

        if (rightSleeve != null) {
            rightSleeve.pitch  = this.rightArm.pitch;
            rightSleeve.yaw    = this.rightArm.yaw;
            rightSleeve.roll   = this.rightArm.roll;
            rightSleeve.pivotX = this.rightArm.pivotX;
            rightSleeve.pivotY = this.rightArm.pivotY;
            rightSleeve.pivotZ = this.rightArm.pivotZ;
        }
        if (leftSleeve != null) {
            leftSleeve.pitch  = this.leftArm.pitch;
            leftSleeve.yaw    = this.leftArm.yaw;
            leftSleeve.roll   = this.leftArm.roll;
            leftSleeve.pivotX = this.leftArm.pivotX;
            leftSleeve.pivotY = this.leftArm.pivotY;
            leftSleeve.pivotZ = this.leftArm.pivotZ;
        }
    }
}
