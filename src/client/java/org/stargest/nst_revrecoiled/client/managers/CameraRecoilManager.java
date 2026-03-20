package org.stargest.nst_revrecoiled.client.managers;

import net.minecraft.client.MinecraftClient;

/**
 * Manages camera recoil effects for revolver weapons.
 * Applies smooth pitch and yaw offsets to the player camera on each shot.
 * Client-side only — offsets are consumed by MouseRecoilMixin each frame.
 *
 * Uses a two-phase animation system:
 * 1. KICKBACK  — quick upward/sideways snap to the recoil peak (easeOutQuad)
 * 2. RECOVERY  — smooth return to the original camera position (easeInOutCubic)
 *
 * Shot detection is external: applyRecoil() is called directly from the
 * clientFireCallback registered in Nst_revolvers_recoiledClient, which fires
 * at the exact moment the shot is processed in BaseRevolverItem.use().
 * This avoids the per-frame charge-state polling that the old wasCharged approach used.
 */
public class CameraRecoilManager {

    private static final CameraRecoilManager INSTANCE = new CameraRecoilManager();

    // -------------------------------------------------------------------------
    // Recoil parameters
    // -------------------------------------------------------------------------

    private static final float RECOIL_PITCH        = 4.5f;  // Upward kick in degrees
    private static final float RECOIL_YAW_VARIANCE = 1.5f;  // Horizontal variance in degrees
    private static final float RECOIL_DURATION     = 0.1f;  // Time to reach peak recoil (seconds)
    private static final float RECOVERY_DURATION   = 0.45f; // Time to return to rest (seconds)

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private float currentPitchOffset = 0.0f;
    private float currentYawOffset   = 0.0f;
    private float targetPitchOffset  = 0.0f;
    private float targetYawOffset    = 0.0f;

    private long        recoilStartTime = 0;
    private RecoilPhase currentPhase    = RecoilPhase.IDLE;

    /**
     * Recoil animation phases.
     * IDLE     — no recoil active; updateAndGetOffsets() returns zero immediately.
     * KICKBACK — camera moves to the recoil peak over RECOIL_DURATION seconds.
     * RECOVERY — camera returns to rest over RECOVERY_DURATION seconds.
     */
    private enum RecoilPhase {
        IDLE,
        KICKBACK,
        RECOVERY
    }

    private CameraRecoilManager() {}

    public static CameraRecoilManager getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Triggers a new recoil event at the moment of firing.
     * Called directly from the clientFireCallback registered in
     * Nst_revolvers_recoiledClient, which is invoked by BaseRevolverItem.use()
     * on the client side exactly when the shot is processed.
     *
     * Randomised yaw variance gives each shot a slightly different sideways kick,
     * preventing the recoil from feeling mechanical or predictable.
     */
    public void applyRecoil() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        targetPitchOffset = -RECOIL_PITCH; // Negative = camera kicks upward
        targetYawOffset   = (client.world.random.nextFloat() - 0.5f) * 2.0f * RECOIL_YAW_VARIANCE;

        recoilStartTime = System.currentTimeMillis();
        currentPhase    = RecoilPhase.KICKBACK;
    }

    /**
     * Advances the recoil animation and returns the current camera offsets.
     * Called every frame from MouseRecoilMixin.
     * Returns immediately with zeroes when no recoil is active.
     *
     * KICKBACK uses easeOutQuad for a snappy initial kick.
     * RECOVERY uses easeInOutCubic for a smooth, natural return.
     *
     * @return float[2] — [pitchOffset, yawOffset] to add to camera rotation this frame
     */
    public float[] updateAndGetOffsets() {
        if (currentPhase == RecoilPhase.IDLE) {
            return new float[]{0.0f, 0.0f};
        }

        float elapsedSeconds = (System.currentTimeMillis() - recoilStartTime) / 1000.0f;

        switch (currentPhase) {
            case KICKBACK:
                if (elapsedSeconds >= RECOIL_DURATION) {
                    // Kickback complete — lock to peak and start recovery
                    currentPhase      = RecoilPhase.RECOVERY;
                    recoilStartTime   = System.currentTimeMillis();
                    currentPitchOffset = targetPitchOffset;
                    currentYawOffset   = targetYawOffset;
                } else {
                    float eased = easeOutQuad(elapsedSeconds / RECOIL_DURATION);
                    currentPitchOffset = targetPitchOffset * eased;
                    currentYawOffset   = targetYawOffset   * eased;
                }
                break;

            case RECOVERY:
                if (elapsedSeconds >= RECOVERY_DURATION) {
                    // Recovery complete — return to idle
                    currentPhase       = RecoilPhase.IDLE;
                    currentPitchOffset = 0.0f;
                    currentYawOffset   = 0.0f;
                    targetPitchOffset  = 0.0f;
                    targetYawOffset    = 0.0f;
                } else {
                    float eased = easeInOutCubic(elapsedSeconds / RECOVERY_DURATION);
                    currentPitchOffset = targetPitchOffset * (1.0f - eased);
                    currentYawOffset   = targetYawOffset   * (1.0f - eased);
                }
                break;
        }

        return new float[]{currentPitchOffset, currentYawOffset};
    }

    /**
     * Resets all recoil state to idle.
     * Called on disconnect, world switch, or player death to prevent
     * a stuck camera offset carrying over into the next session.
     */
    public void reset() {
        currentPhase       = RecoilPhase.IDLE;
        currentPitchOffset = 0.0f;
        currentYawOffset   = 0.0f;
        targetPitchOffset  = 0.0f;
        targetYawOffset    = 0.0f;
    }

    /** @return true if a kickback or recovery animation is currently in progress */
    public boolean isRecoilActive() {
        return currentPhase != RecoilPhase.IDLE;
    }

    // -------------------------------------------------------------------------
    // Easing functions
    // -------------------------------------------------------------------------

    /** Ease-out quadratic: fast start, slow finish. Used for the kickback snap. */
    private float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    /** Ease-in-out cubic: smooth acceleration and deceleration. Used for recovery. */
    private float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4.0f * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }
}
