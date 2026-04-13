package org.stargest.nst_revrecoiled.mixins.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
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
@Mixin(PlayerModel.class)
public abstract class PlayerEntityModelMixin extends HumanoidModel<AbstractClientPlayer> {

    @Final
    @Shadow
    public ModelPart rightSleeve;
    @Final
    @Shadow public ModelPart leftSleeve;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void applyRevolverArmPose(LivingEntity entity, float limbAngle, float limbDistance,
                                      float animationProgress, float headYaw, float headPitch,
                                      CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        PlayerArmPose.applyRevolverPose(this, player);

        if (rightSleeve != null) {
            rightSleeve.xRot  = this.rightArm.xRot;
            rightSleeve.yRot    = this.rightArm.yRot;
            rightSleeve.zRot   = this.rightArm.zRot;
            rightSleeve.x = this.rightArm.x;
            rightSleeve.y = this.rightArm.y;
            rightSleeve.z = this.rightArm.z;
        }
        if (leftSleeve != null) {
            leftSleeve.xRot  = this.leftArm.xRot;
            leftSleeve.yRot    = this.leftArm.yRot;
            leftSleeve.zRot   = this.leftArm.zRot;
            leftSleeve.x = this.leftArm.x;
            leftSleeve.y = this.leftArm.y;
            leftSleeve.z = this.leftArm.z;
        }
    }
}
