package org.stargest.nst_revrecoiled.client.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
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
@Mixin(Mouse.class)
public class MouseRecoilMixin {

    @Shadow @Final private MinecraftClient client;

    @Unique
    private float lastRecoilPitch = 0.0f;
    @Unique
    private float lastRecoilYaw = 0.0f;

    /**
     * Injects into updateMouse to apply recoil offsets after normal mouse input.
     * This ensures recoil works smoothly with player camera movement.
     */
    @Inject(method = "updateMouse", at = @At("TAIL"))
    private void applyRecoilToCamera(CallbackInfo ci) {
        if (this.client.player == null) {
            return;
        }

        ClientPlayerEntity player = this.client.player;
        CameraRecoilManager recoilManager = CameraRecoilManager.getInstance();

        // Get current recoil offsets from manager
        float[] offsets = recoilManager.updateAndGetOffsets();
        float pitchOffset = offsets[0];
        float yawOffset = offsets[1];

        // Calculate delta from last frame to apply incrementally
        float pitchDelta = pitchOffset - lastRecoilPitch;
        float yawDelta = yawOffset - lastRecoilYaw;

        // Apply recoil to player camera rotation
        if (Math.abs(pitchDelta) > 0.001f || Math.abs(yawDelta) > 0.001f) {
            float newPitch = player.getPitch() + pitchDelta;
            float newYaw = player.getYaw() + yawDelta;

            // Clamp pitch to valid range (-90 to 90 degrees)
            newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

            player.setPitch(newPitch);
            player.setYaw(newYaw);
        }

        // Store current offsets for next frame's delta calculation
        lastRecoilPitch = pitchOffset;
        lastRecoilYaw = yawOffset;

        // Reset stored values when recoil animation is complete
        if (!recoilManager.isRecoilActive()) {
            lastRecoilPitch = 0.0f;
            lastRecoilYaw = 0.0f;
        }
    }
}
