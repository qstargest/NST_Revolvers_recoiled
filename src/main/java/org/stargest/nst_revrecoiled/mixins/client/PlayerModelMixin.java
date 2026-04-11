package org.stargest.nst_revrecoiled.mixins.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;

/**
 * Mixin to apply custom arm poses for revolver weapons.
 * Injects into the setAngles method to modify arm positions during rendering.
 * Compatible with Minecraft 1.21.4 render state system.
 *
 * This mixin runs after all vanilla arm positioning logic,
 * allowing revolver-specific poses to override default animations.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<PlayerRenderState> {

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    /**
     * Injects at the tail of setupAnim to apply revolver arm poses.
     * This ensures our custom poses are applied after vanilla logic.
     */
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V",
            at = @At("TAIL")
    )
    private void applyRevolverArmPose(PlayerRenderState renderState, CallbackInfo ci) {
        PlayerArmPose.applyRevolverPose(this, renderState);
    }
}
