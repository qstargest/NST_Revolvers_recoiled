package org.stargest.nst_revrecoiled.mixins.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;

/**
 * Mixin to apply camera recoil to mouse movement.
 * Injects into the mouse update method to add recoil offsets to camera rotation.
 *
 * Works by:
 * 1. Getting current recoil offsets from CameraRecoilManager
 * 2. Calculating delta from previous frame
 * 3. Applying delta to player pitch and yaw
 * 4. Clamping pitch to valid range (-90 to 90)
 */
@Mixin(MouseHandler.class)
public class MouseRecoilMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique
    private float lastRecoilPitch = 0.0f;
    @Unique
    private float lastRecoilYaw = 0.0f;

    /**
     * Injects into turnPlayer to apply recoil offsets after normal mouse input.
     * This ensures recoil works smoothly with player camera movement.
     */
    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void applyRecoilToCamera(CallbackInfo ci) {
        if (this.minecraft.player == null) {
            return;
        }

        LocalPlayer player = this.minecraft.player;
        CameraRecoilManager recoilManager = CameraRecoilManager.getInstance();

        float[] offsets = recoilManager.updateAndGetOffsets();
        float pitchOffset = offsets[0];
        float yawOffset = offsets[1];

        float pitchDelta = pitchOffset - lastRecoilPitch;
        float yawDelta = yawOffset - lastRecoilYaw;

        if (Math.abs(pitchDelta) > 0.001f || Math.abs(yawDelta) > 0.001f) {
            float newPitch = player.getXRot() + pitchDelta;
            float newYaw = player.getYRot() + yawDelta;

            newPitch = Math.clamp(newPitch, -90.0f, 90.0f);

            player.setXRot(newPitch);
            player.setYRot(newYaw);
        }

        lastRecoilPitch = pitchOffset;
        lastRecoilYaw = yawOffset;

        if (!recoilManager.isRecoilActive()) {
            lastRecoilPitch = 0.0f;
            lastRecoilYaw = 0.0f;
        }
    }
}
