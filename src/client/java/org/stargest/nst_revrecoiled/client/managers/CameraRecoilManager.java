package org.stargest.nst_revrecoiled.client.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Manages camera recoil effects for revolver weapons.
 * Handles smooth recoil kickback and gradual camera return to original position.
 * Client-side only - manages pitch and yaw offsets applied to player camera.
 *
 * Uses a two-phase system:
 * 1. KICKBACK - Quick upward/sideways camera movement
 * 2. RECOVERY - Smooth return to original camera position
 */
public class CameraRecoilManager {

    private static final CameraRecoilManager INSTANCE = new CameraRecoilManager();

    // Recoil parameters
    private static final float RECOIL_PITCH = 4.5f;  // Upward kick in degrees
    private static final float RECOIL_YAW_VARIANCE = 1.5f;  // Horizontal variance in degrees
    private static final float RECOIL_DURATION = 0.1f;  // Time to reach peak recoil (seconds)
    private static final float RECOVERY_DURATION = 0.45f;  // Time to return to original position (seconds)

    // Shot detection state
    private boolean wasCharged = false;
    private ItemStack lastStack = ItemStack.EMPTY;

    // Current recoil state
    private float currentPitchOffset = 0.0f;
    private float currentYawOffset = 0.0f;
    private float targetPitchOffset = 0.0f;
    private float targetYawOffset = 0.0f;

    private long recoilStartTime = 0;
    private RecoilPhase currentPhase = RecoilPhase.IDLE;

    /**
     * Recoil animation phases.
     */
    private enum RecoilPhase {
        IDLE,       // No recoil active
        KICKBACK,   // Camera moving up/sideways
        RECOVERY    // Camera returning to original position
    }

    private CameraRecoilManager() {}

    public static CameraRecoilManager getInstance() {
        return INSTANCE;
    }

    /**
     * Triggers a new recoil event.
     * Called when the revolver is fired.
     * Applies randomized horizontal variance for realistic feel.
     */
    public void applyRecoil() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        // Set target offsets with randomness
        targetPitchOffset = -RECOIL_PITCH;  // Negative = camera goes up
        targetYawOffset = (client.world.random.nextFloat() - 0.5f) * 2.0f * RECOIL_YAW_VARIANCE;

        recoilStartTime = System.currentTimeMillis();
        currentPhase = RecoilPhase.KICKBACK;
    }

    /**
     * Updates recoil state and returns current camera offsets.
     * Should be called every frame from MouseRecoilMixin.
     *
     * @return float array [pitchOffset, yawOffset] to add to camera rotation
     */
    public float[] updateAndGetOffsets() {
        checkPlayerShot();

        if (currentPhase == RecoilPhase.IDLE) {
            return new float[]{0.0f, 0.0f};
        }

        float elapsedSeconds = (System.currentTimeMillis() - recoilStartTime) / 1000.0f;

        switch (currentPhase) {
            case KICKBACK:
                if (elapsedSeconds >= RECOIL_DURATION) {
                    // Kickback complete, start recovery
                    currentPhase = RecoilPhase.RECOVERY;
                    recoilStartTime = System.currentTimeMillis();
                    currentPitchOffset = targetPitchOffset;
                    currentYawOffset = targetYawOffset;
                } else {
                    // Interpolate to target using easeOut curve for snappy feel
                    float progress = elapsedSeconds / RECOIL_DURATION;
                    float eased = easeOutQuad(progress);

                    currentPitchOffset = targetPitchOffset * eased;
                    currentYawOffset = targetYawOffset * eased;
                }
                break;

            case RECOVERY:
                if (elapsedSeconds >= RECOVERY_DURATION) {
                    // Recovery complete, return to idle
                    currentPhase = RecoilPhase.IDLE;
                    currentPitchOffset = 0.0f;
                    currentYawOffset = 0.0f;
                    targetPitchOffset = 0.0f;
                    targetYawOffset = 0.0f;
                } else {
                    // Interpolate back to zero using easeInOut for smooth recovery
                    float progress = elapsedSeconds / RECOVERY_DURATION;
                    float eased = easeInOutCubic(progress);

                    currentPitchOffset = targetPitchOffset * (1.0f - eased);
                    currentYawOffset = targetYawOffset * (1.0f - eased);
                }
                break;
        }

        return new float[]{currentPitchOffset, currentYawOffset};
    }

    /**
     * Detects when player fires the revolver by monitoring charge state transitions.
     * Triggers recoil when weapon goes from charged to uncharged state.
     */
    private void checkPlayerShot() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = client.player.getMainHandStack();

        if (stack.getItem() instanceof BaseRevolverItem) {
            boolean isCharged = BaseRevolverItem.isCharged(stack);

            // Detect transition: was charged -> now uncharged
            if (wasCharged && !isCharged && ItemStack.areItemsEqual(stack, lastStack)) {
                applyRecoil();
            }

            wasCharged = isCharged;
            lastStack = stack;
        } else {
            wasCharged = false;
            lastStack = ItemStack.EMPTY;
        }
    }

    /**
     * Resets recoil state to idle.
     * Called when player dies, switches worlds, disconnects, etc.
     */
    public void reset() {
        currentPhase = RecoilPhase.IDLE;
        wasCharged = false;
        lastStack = ItemStack.EMPTY;
        currentPitchOffset = 0.0f;
        currentYawOffset = 0.0f;
        targetPitchOffset = 0.0f;
        targetYawOffset = 0.0f;
    }

    // Easing functions for smooth motion curves

    /**
     * Ease-out quadratic function for quick start, slow finish.
     */
    private float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    /**
     * Ease-in-out cubic function for smooth acceleration and deceleration.
     */
    private float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4.0f * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    /**
     * Checks if recoil is currently active (not idle).
     */
    public boolean isRecoilActive() {
        return currentPhase != RecoilPhase.IDLE;
    }
}
