package org.stargest.nst_revrecoiled.client.Mixins;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.client.render.player.PlayerArmPose;

/**
 * Mixin to apply custom arm poses for revolver weapons.
 * Injects into the setAngles method to modify arm positions during rendering.
 * Compatible with Minecraft 1.21.4 render state system.
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL")
    )
    private void applyRevolverArmPose(PlayerEntityRenderState renderState, CallbackInfo ci) {
        PlayerArmPose.applyRevolverPose(this, renderState);
    }
}
