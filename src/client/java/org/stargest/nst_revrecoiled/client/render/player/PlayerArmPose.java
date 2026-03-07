package org.stargest.nst_revrecoiled.client.render.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles custom arm positioning and animations for revolver weapons.
 * Manages first-person and third-person arm poses including:
 * - Aim tracking (following player's look direction)
 * - Reload animations with adaptive hand positioning
 * - Recoil effects
 * - Movement shake
 * - Smooth interpolation between states
 */
public class PlayerArmPose {

    private static final float LERP_SPEED = 0.7f;
    private static final float LERP_SPEED_RECOIL = 0.12f;
    private static final boolean ENABLE_AIM_TRACKING = true;

    // Cache maps for smooth interpolation across frames
    private static final Map<Integer, ArmState> rightArmCache = new HashMap<>();
    private static final Map<Integer, ArmState> leftArmCache = new HashMap<>();
    private static final Map<Integer, Boolean> wasChargedCache = new HashMap<>();
    private static final Map<Integer, Double> lastShotTime = new HashMap<>();
    private static final Map<Integer, Double> drawStartTime = new HashMap<>();
    private static final Map<Integer, Integer> lastItemIdCache = new HashMap<>();

    /**
     * Stores arm rotation state (pitch, yaw, roll).
     */
    private static class ArmState {
        float p, y, r;
        ArmState(float p, float y, float r) {
            this.p = p;
            this.y = y;
            this.r = r;
        }
    }

    /**
     * Main method to apply revolver-specific arm poses to the player model.
     * Called from a mixin injecting into PlayerEntityRenderer.
     *
     * @param model The biped entity model to modify
     * @param renderState The player's current render state
     */
    public static void applyRevolverPose(BipedEntityModel<PlayerEntityRenderState> model, PlayerEntityRenderState renderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double renderTime = client.world.getTime() + client.getRenderTickCounter().getTickDelta(false);
        Entity entity = client.world.getEntityById(renderState.id);
        if (!(entity instanceof PlayerEntity player)) return;

        ItemStack stack = player.getStackInHand(player.getActiveHand());
        if (!(stack.getItem() instanceof BaseRevolverItem)) {
            clearAllCaches(renderState.id);
            return;
        }

        // Track item identity hash to detect item switches
        int currentItemHash = System.identityHashCode(stack.getItem());
        if (!lastItemIdCache.getOrDefault(renderState.id, -1).equals(currentItemHash)) {
            drawStartTime.put(renderState.id, renderTime);
            lastItemIdCache.put(renderState.id, currentItemHash);
        }

        // Detect shot events (charged -> not charged transition)
        boolean isCharged = BaseRevolverItem.isCharged(stack);
        boolean wasCharged = wasChargedCache.getOrDefault(renderState.id, false);
        if (wasCharged && !isCharged) {
            lastShotTime.put(renderState.id, renderTime);
        }
        wasChargedCache.put(renderState.id, isCharged);

        // Calculate draw animation progress (0 to 1 over 8 ticks)
        double rawDrawProgress = (renderTime - drawStartTime.getOrDefault(renderState.id, 0.0)) / 8.0;
        float drawProgress = MathHelper.clamp((float) Math.pow(MathHelper.clamp((float)rawDrawProgress, 0f, 1f), 0.9f), 0f, 1f);

        boolean isCharging = player.isUsingItem();
        double timeSinceShot = renderTime - lastShotTime.getOrDefault(renderState.id, -100.0);

        // --- RIGHT ARM (PRIMARY/SHOOTING HAND) ---
        float headPitch = model.head.pitch;
        float headYaw = model.head.yaw;

        // Base position without aim tracking
        float basePitch = -1.57f; // ~90 degrees down (aiming forward)
        float baseYaw = (renderState.mainArm == Arm.RIGHT ? -0.2f : 0.2f);

        // Store reload-specific base values
        float reloadBasePitch = MathHelper.lerp(drawProgress, 0.5f, basePitch);
        float reloadBaseYaw = baseYaw;

        // Apply aim tracking only when not reloading
        if (ENABLE_AIM_TRACKING && drawProgress > 0.5f) {
            float trackingStrength = MathHelper.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += (headPitch * trackingStrength);
            baseYaw += (headYaw * trackingStrength);
        }

        float targetRP = MathHelper.lerp(drawProgress, 0.5f, basePitch);
        float targetRY = baseYaw;
        float targetRR = MathHelper.lerp(drawProgress, 0.4f, 0.0f);

        // --- LEFT ARM (SUPPORTING HAND) ---
        float targetLP = model.leftArm.pitch;
        float targetLY = model.leftArm.yaw;
        float targetLR = model.leftArm.roll;

        // Recoil effect
        if (drawProgress > 0.2f && timeSinceShot >= 2.0 && timeSinceShot < 8.0) {
            float recoil = (float) ((8.0 - timeSinceShot) / 6.0);
            targetRP -= recoil * 0.6f;
            targetRR -= recoil * 0.2f;
        }

        // --- ADAPTIVE RELOAD ANIMATION ---
        if (isCharging) {
            float chargeProgress = getProgress(player, stack);

            // Snap factor creates smooth easing in/out at animation boundaries
            float snap = (chargeProgress < 0.15f)
                    ? chargeProgress / 0.15f
                    : (chargeProgress > 0.85f ? (1f - chargeProgress) / 0.15f : 1.0f);

            // 1. Rotate revolver cylinder
            targetRR = snap * 0.9f;
            targetRY -= snap * 0.25f;

            // 2. Return to base reload position (no aim tracking during reload)
            targetRP = MathHelper.lerp(snap, targetRP, reloadBasePitch);
            targetRY = MathHelper.lerp(snap, targetRY, reloadBaseYaw - snap * 0.25f);

            // 3. Left hand follows right hand position
            float sideSign = (renderState.mainArm == Arm.RIGHT ? 1.0f : -1.0f);

            float adaptiveLeftPitch = targetRP + 0.45f;
            float adaptiveLeftYaw = targetRY + (0.55f * sideSign);
            float adaptiveLeftRoll = 0.0f;

            targetLP = MathHelper.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = MathHelper.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = MathHelper.lerp(snap, targetLR, adaptiveLeftRoll);

            // Bullet insertion shake effect near end of reload
            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * 2.5f) * 0.025f);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // --- MOVEMENT SHAKE ---
        Vec3d velocity = player.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float shakeAmp = player.isSprinting() ? 0.06f : (horizontalSpeed > 0.02f ? 0.03f : 0f);
        float shakeFreq = player.isSprinting() ? 1.6f : 0.9f;

        if (shakeAmp > 0f) {
            float timeSeed = (float) ((renderTime + renderState.id * 7) * shakeFreq * 0.5);
            float shake = (float) (Math.sin(timeSeed) * shakeAmp + Math.sin(timeSeed * 1.7) * (shakeAmp * 0.35));
            targetRP += shake * 0.6f;
            targetRR += shake * 1.25f;
            targetRY += shake * 0.28f;
        }

        applyToModel(model, renderState, targetRP, targetRY, targetRR, targetLP, targetLY, targetLR, drawProgress, (float) timeSinceShot);
    }

    /**
     * Applies interpolated arm rotations to the model with smooth transitions.
     * Uses different lerp speeds for recoil vs normal movement.
     */
    private static void applyToModel(BipedEntityModel<PlayerEntityRenderState> model, PlayerEntityRenderState renderState,
                                     float rp, float ry, float rr, float lp, float ly, float lr,
                                     float drawProgress, float timeSinceShot) {
        int id = renderState.id;
        ArmState lastR = rightArmCache.getOrDefault(id, new ArmState(rp, ry, rr));
        ArmState lastL = leftArmCache.getOrDefault(id, new ArmState(lp, ly, lr));

        // Use slower lerp speed during recoil for more dramatic effect
        float rightLerp = (timeSinceShot >= 2.0f && timeSinceShot < 8.0f) ? LERP_SPEED_RECOIL : LERP_SPEED;

        // Interpolate right arm
        float fRp = MathHelper.lerp(rightLerp, lastR.p, rp);
        float fRy = MathHelper.lerp(LERP_SPEED, lastR.y, ry);
        float fRr = MathHelper.lerp(rightLerp, lastR.r, rr);
        rightArmCache.put(id, new ArmState(fRp, fRy, fRr));

        model.rightArm.pitch = fRp;
        model.rightArm.yaw = fRy;
        model.rightArm.roll = fRr;

        // Interpolate left arm
        model.leftArm.pitch = MathHelper.lerp(LERP_SPEED, lastL.p, lp);
        model.leftArm.yaw = MathHelper.lerp(LERP_SPEED, lastL.y, ly);
        model.leftArm.roll = MathHelper.lerp(LERP_SPEED, lastL.r, lr);
        leftArmCache.put(id, new ArmState(model.leftArm.pitch, model.leftArm.yaw, model.leftArm.roll));

        // Adjust arm pivot points for better positioning
        model.rightArm.pivotX = -5.0f;
        model.rightArm.pivotY = MathHelper.lerp(drawProgress, 4.0f, 2.0f) + (renderState.sneaking ? 2.0f : 0.0f);
        model.rightArm.pivotZ = MathHelper.lerp(drawProgress, 2.0f, 0.0f);
    }

    /**
     * Calculates the progress of item use (0 to 1).
     */
    private static float getProgress(PlayerEntity player, ItemStack stack) {
        int max = stack.getMaxUseTime(player);
        return max <= 0 ? 0 : MathHelper.clamp((float)(max - player.getItemUseTimeLeft()) / max, 0, 1);
    }

    /**
     * Clears all cached data for a specific player entity.
     * Called when player is no longer holding a revolver.
     */
    private static void clearAllCaches(int id) {
        rightArmCache.remove(id);
        leftArmCache.remove(id);
        wasChargedCache.remove(id);
        drawStartTime.remove(id);
        lastItemIdCache.remove(id);
        lastShotTime.remove(id);
    }
}
