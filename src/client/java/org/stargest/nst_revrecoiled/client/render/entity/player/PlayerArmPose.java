package org.stargest.nst_revrecoiled.client.render.entity.player;

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
import org.stargest.nst_revrecoiled.Items.RevolverArmPoseItem;

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
 *
 * Uses a consolidated state management approach with a single Map&lt;Integer, PlayerRevolverState&gt;
 * instead of multiple parallel HashMaps, improving code organization and cache locality.
 *
 * Provides cleanup methods to prevent memory leaks when players disconnect or entities unload.
 */
public class PlayerArmPose {

    // -------------------------------------------------------------------------
    // Animation constants
    // -------------------------------------------------------------------------

    /** Lerp factor for normal arm movement (0–1, higher = faster). */
    private static final float LERP_SPEED = 0.7f;

    /** Lerp factor during recoil for a dramatic slow snap-back. */
    private static final float LERP_SPEED_RECOIL = 0.12f;

    /** Whether the arm follows the player's look direction. */
    private static final boolean ENABLE_AIM_TRACKING = true;

    /** Base arm pitch when aiming forward (≈ -π/2 radians = straight forward). */
    private static final float BASE_AIM_PITCH = -1.57f;

    /** Horizontal arm offset based on dominant hand side. */
    private static final float BASE_AIM_YAW_OFFSET = 0.2f;

    /** Roll (tilt) of arm at the start of the draw animation. */
    private static final float DRAW_START_ROLL = 0.4f;

    /** Pitch offset of the arm at the start of the draw animation. */
    private static final float DRAW_START_PITCH = 0.5f;

    /** Duration in ticks over which the draw animation plays. */
    private static final float DRAW_DURATION_TICKS = 8.0f;

    /** Easing exponent applied to raw draw progress for a slight ease-out. */
    private static final float DRAW_EASE_EXPONENT = 0.9f;

    /** Reload: max cylinder rotation in radians. */
    private static final float RELOAD_CYLINDER_ROLL = 0.9f;

    /** Reload: max cylinder yaw offset. */
    private static final float RELOAD_CYLINDER_YAW = 0.25f;

    /** Reload: fraction of charge progress at which snap-blend edges occur. */
    private static final float RELOAD_SNAP_EDGE = 0.15f;

    /** Reload: left-hand pitch offset relative to right-hand. */
    private static final float RELOAD_LEFT_PITCH_OFFSET = 0.45f;

    /** Reload: left-hand yaw offset relative to right-hand. */
    private static final float RELOAD_LEFT_YAW_OFFSET = 0.55f;

    /** Reload: sine frequency of bullet-insertion shake effect (radians/tick). */
    private static final float RELOAD_SHAKE_FREQ = 2.5f;

    /** Reload: amplitude of bullet-insertion shake (radians). */
    private static final float RELOAD_SHAKE_AMP = 0.025f;

    /** Ticks after a shot during which recoil lerp speed applies. */
    private static final float RECOIL_TICKS_MIN = 2.0f;
    private static final float RECOIL_TICKS_MAX = 8.0f;

    /** Pitch and roll reduction during recoil phase. */
    private static final float RECOIL_PITCH_FACTOR = 0.6f;
    private static final float RECOIL_ROLL_FACTOR = 0.2f;

    /** Movement shake amplitude for sprinting and walking. */
    private static final float SHAKE_SPRINT_AMP = 0.06f;
    private static final float SHAKE_WALK_AMP = 0.03f;
    private static final float SHAKE_WALK_MIN_SPEED = 0.02f;

    /** Frequencies for the two sine waves composing the walk shake. */
    private static final float SHAKE_FREQ_SPRINT = 1.6f;
    private static final float SHAKE_FREQ_WALK = 0.9f;

    /** Blend weights of each body axis for walk shake. */
    private static final float SHAKE_PITCH_FACTOR = 0.6f;
    private static final float SHAKE_ROLL_FACTOR = 1.25f;
    private static final float SHAKE_YAW_FACTOR = 0.28f;

    /** Secondary sine multiplier for irregular, natural-looking shake. */
    private static final float SHAKE_SECONDARY_FREQ_MULT = 1.7f;
    private static final float SHAKE_SECONDARY_AMP_MULT = 0.35f;

    // -------------------------------------------------------------------------
    // Per-player state — consolidated into single map for better organization
    // -------------------------------------------------------------------------

    private static final Map<Integer, PlayerRevolverState> playerStates = new HashMap<>();

    /**
     * Holds all per-player revolver rendering state.
     * Consolidates what were previously six separate HashMaps into one state object
     * for better code organization and cache locality.
     */
    private static class PlayerRevolverState {
        ArmState rightArm;
        ArmState leftArm;
        boolean  wasCharged   = false;
        double   lastShotTime = -100.0;
        double   drawStartTime = 0.0;
        int      lastItemId   = -1;
    }

    /**
     * Arm rotation state (pitch, yaw, roll in radians).
     */
    private static class ArmState {
        float pitch, yaw, roll;

        ArmState(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw   = yaw;
            this.roll  = roll;
        }
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Main method to apply revolver-specific arm poses to the player model.
     * Called from PlayerEntityModelMixin injecting into setAngles().
     *
     * @param model       The biped entity model to modify
     * @param renderState The player's current render state
     */
    public static void applyRevolverPose(BipedEntityModel<PlayerEntityRenderState> model,
                                         PlayerEntityRenderState renderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double renderTime = client.world.getTime() + client.getRenderTickCounter().getTickDelta(false);
        Entity entity = client.world.getEntityById(renderState.id);
        if (!(entity instanceof PlayerEntity player)) return;

        ItemStack stack = player.getStackInHand(player.getActiveHand());
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            // Not holding revolver - remove cached state
            playerStates.remove(renderState.id);
            return;
        }

        // Get or create player state
        PlayerRevolverState state = playerStates.computeIfAbsent(renderState.id,
                id -> new PlayerRevolverState());

        // Track item identity to detect item switches
        int currentItemHash = System.identityHashCode(stack.getItem());
        if (state.lastItemId != currentItemHash) {
            state.drawStartTime = renderTime;
            state.lastItemId = currentItemHash;
        }

        // Detect shot event (charged → not charged transition)
        boolean isCharged = BaseRevolverItem.isCharged(stack);
        if (state.wasCharged && !isCharged) {
            state.lastShotTime = renderTime;
        }
        state.wasCharged = isCharged;

        // Draw animation progress (0 → 1 over DRAW_DURATION_TICKS ticks)
        double rawDrawProgress = (renderTime - state.drawStartTime) / DRAW_DURATION_TICKS;
        float drawProgress = MathHelper.clamp(
                (float) Math.pow(MathHelper.clamp((float) rawDrawProgress, 0f, 1f), DRAW_EASE_EXPONENT),
                0f, 1f);

        boolean isCharging = player.isUsingItem();
        double timeSinceShot = renderTime - state.lastShotTime;

        // ------------------------------------------------------------------
        // RIGHT ARM (primary / shooting hand)
        // ------------------------------------------------------------------
        float headPitch = model.head.pitch;
        float headYaw   = model.head.yaw;

        float sideSign = (renderState.mainArm == Arm.RIGHT) ? -1.0f : 1.0f;
        float basePitch = BASE_AIM_PITCH;
        float baseYaw   = sideSign * BASE_AIM_YAW_OFFSET;

        // Store base reload position (without aim tracking)
        float reloadBasePitch = MathHelper.lerp(drawProgress, DRAW_START_PITCH, basePitch);
        float reloadBaseYaw   = baseYaw;

        // Apply aim tracking after draw animation is mostly complete
        if (ENABLE_AIM_TRACKING && drawProgress > 0.5f) {
            float trackingStrength = MathHelper.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += headPitch * trackingStrength;
            baseYaw   += headYaw  * trackingStrength;
        }

        float targetRP = MathHelper.lerp(drawProgress, DRAW_START_PITCH, basePitch);
        float targetRY = baseYaw;
        float targetRR = MathHelper.lerp(drawProgress, DRAW_START_ROLL, 0.0f);

        // ------------------------------------------------------------------
        // LEFT ARM (supporting hand) — initially unchanged from vanilla
        // ------------------------------------------------------------------
        float targetLP = model.leftArm.pitch;
        float targetLY = model.leftArm.yaw;
        float targetLR = model.leftArm.roll;

        // Recoil effect - applies after draw and within recoil time window
        if (drawProgress > 0.2f && timeSinceShot >= RECOIL_TICKS_MIN && timeSinceShot < RECOIL_TICKS_MAX) {
            float recoil = (float) ((RECOIL_TICKS_MAX - timeSinceShot) / (RECOIL_TICKS_MAX - RECOIL_TICKS_MIN));
            targetRP -= recoil * RECOIL_PITCH_FACTOR;
            targetRR -= recoil * RECOIL_ROLL_FACTOR;
        }

        // ------------------------------------------------------------------
        // ADAPTIVE RELOAD ANIMATION
        // ------------------------------------------------------------------
        if (isCharging) {
            float chargeProgress = getProgress(player, stack);

            // Snap factor creates smooth ease in/out at animation boundaries
            float snap = (chargeProgress < RELOAD_SNAP_EDGE)
                    ? chargeProgress / RELOAD_SNAP_EDGE
                    : (chargeProgress > (1f - RELOAD_SNAP_EDGE)
                    ? (1f - chargeProgress) / RELOAD_SNAP_EDGE
                    : 1.0f);

            // Rotate cylinder and adjust aim
            targetRR = snap * RELOAD_CYLINDER_ROLL;
            targetRY -= snap * RELOAD_CYLINDER_YAW;
            targetRP = MathHelper.lerp(snap, targetRP, reloadBasePitch);
            targetRY = MathHelper.lerp(snap, targetRY, reloadBaseYaw - snap * RELOAD_CYLINDER_YAW);

            // Left hand follows right hand position
            float adaptiveLeftPitch = targetRP + RELOAD_LEFT_PITCH_OFFSET;
            float adaptiveLeftYaw   = targetRY + (RELOAD_LEFT_YAW_OFFSET * -sideSign);
            float adaptiveLeftRoll  = 0.0f;

            targetLP = MathHelper.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = MathHelper.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = MathHelper.lerp(snap, targetLR, adaptiveLeftRoll);

            // Bullet-insertion shake near end of reload
            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * RELOAD_SHAKE_FREQ) * RELOAD_SHAKE_AMP);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // ------------------------------------------------------------------
        // MOVEMENT SHAKE
        // ------------------------------------------------------------------
        Vec3d velocity = player.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float shakeAmp  = player.isSprinting() ? SHAKE_SPRINT_AMP
                : (horizontalSpeed > SHAKE_WALK_MIN_SPEED ? SHAKE_WALK_AMP : 0f);
        float shakeFreq = player.isSprinting() ? SHAKE_FREQ_SPRINT : SHAKE_FREQ_WALK;

        if (shakeAmp > 0f) {
            float timeSeed = (float) ((renderTime + renderState.id * 7) * shakeFreq * 0.5);
            float shake = (float) (Math.sin(timeSeed) * shakeAmp
                    + Math.sin(timeSeed * SHAKE_SECONDARY_FREQ_MULT) * (shakeAmp * SHAKE_SECONDARY_AMP_MULT));
            targetRP += shake * SHAKE_PITCH_FACTOR;
            targetRR += shake * SHAKE_ROLL_FACTOR;
            targetRY += shake * SHAKE_YAW_FACTOR;
        }

        applyToModel(model, renderState, state, targetRP, targetRY, targetRR, targetLP, targetLY, targetLR,
                drawProgress, (float) timeSinceShot);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies interpolated arm rotations to the model with smooth transitions.
     * Uses different lerp speeds for recoil vs normal movement.
     */
    private static void applyToModel(BipedEntityModel<PlayerEntityRenderState> model,
                                     PlayerEntityRenderState renderState,
                                     PlayerRevolverState state,
                                     float rp, float ry, float rr,
                                     float lp, float ly, float lr,
                                     float drawProgress, float timeSinceShot) {

        // Initialize with current targets if no previous state
        ArmState lastR = state.rightArm != null ? state.rightArm : new ArmState(rp, ry, rr);
        ArmState lastL = state.leftArm  != null ? state.leftArm  : new ArmState(lp, ly, lr);

        // Use slower lerp speed during recoil for a more dramatic effect
        float rightLerp = (timeSinceShot >= RECOIL_TICKS_MIN && timeSinceShot < RECOIL_TICKS_MAX)
                ? LERP_SPEED_RECOIL : LERP_SPEED;

        // Interpolate right arm
        float fRp = MathHelper.lerp(rightLerp, lastR.pitch, rp);
        float fRy = MathHelper.lerp(LERP_SPEED,  lastR.yaw,   ry);
        float fRr = MathHelper.lerp(rightLerp, lastR.roll,  rr);
        state.rightArm = new ArmState(fRp, fRy, fRr);

        model.rightArm.pitch = fRp;
        model.rightArm.yaw   = fRy;
        model.rightArm.roll  = fRr;

        // Interpolate left arm
        float fLp = MathHelper.lerp(LERP_SPEED, lastL.pitch, lp);
        float fLy = MathHelper.lerp(LERP_SPEED, lastL.yaw,   ly);
        float fLr = MathHelper.lerp(LERP_SPEED, lastL.roll,  lr);
        state.leftArm = new ArmState(fLp, fLy, fLr);

        model.leftArm.pitch = fLp;
        model.leftArm.yaw   = fLy;
        model.leftArm.roll  = fLr;

        // Adjust arm pivot points for better visual positioning
        model.rightArm.pivotX = -5.0f;
        model.rightArm.pivotY = MathHelper.lerp(drawProgress, 4.0f, 2.0f) + (renderState.sneaking ? 2.0f : 0.0f);
        model.rightArm.pivotZ = MathHelper.lerp(drawProgress, 2.0f, 0.0f);
    }

    /**
     * Calculates the progress of item use as a 0→1 value.
     */
    private static float getProgress(PlayerEntity player, ItemStack stack) {
        int max = stack.getMaxUseTime(player);
        return max <= 0 ? 0 : MathHelper.clamp((float) (max - player.getItemUseTimeLeft()) / max, 0, 1);
    }

    // -------------------------------------------------------------------------
    // Public cleanup methods (called from client event listeners)
    // -------------------------------------------------------------------------

    /**
     * Clears cached state for a specific entity.
     * Called when an entity unloads to prevent memory leaks.
     *
     * @param entityId The entity ID to clear state for
     */
    public static void clearState(int entityId) {
        playerStates.remove(entityId);
    }

    /**
     * Clears all cached player states.
     * Called when disconnecting from server to prevent stale data.
     */
    public static void clearAllStates() {
        playerStates.clear();
    }
}
