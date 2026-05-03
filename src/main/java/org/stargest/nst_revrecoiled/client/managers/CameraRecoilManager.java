package org.stargest.nst_revrecoiled.client.managers;

import net.minecraft.client.MinecraftClient;
import org.stargest.nst_revrecoiled.util.ModConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private float recoilKickDuration     = RECOIL_DURATION;
    private float recoilRecoveryDuration = RECOVERY_DURATION;

    private long        recoilStartTime = 0;
    private RecoilPhase currentPhase    = RecoilPhase.IDLE;

    private final Queue<int[]> pendingRecoils = new ConcurrentLinkedQueue<>();

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

    public void scheduleRecoil(int delayTicks) {
        pendingRecoils.add(new int[]{delayTicks});
    }

    public void tickPending() {
        pendingRecoils.removeIf(entry -> {
            if (--entry[0] <= 0) {
                applyRecoil();
                return true;
            }
            return false;
        });
    }

    /**
     * Triggers a recoil event with default parameters from configuration.
     * Used by the base mod's revolvers.
     */
    public void applyRecoil() {
        ModConfig.VisualsConfig.RecoilConfig cfg =
                ModConfig.get().visuals.recoil;
        applyRecoil(cfg.pitch, cfg.yawVariance, cfg.kickDuration, cfg.recoveryDuration);
    }

    /**
     * Triggers a recoil event with custom parameters.
     * Intended for addon weapons with different recoil characteristics.
     *
     * @param pitch           upward kick in degrees
     * @param yawVariance     horizontal variance in degrees
     * @param kickDuration    time to reach peak recoil in seconds
     * @param recoveryDuration time to return to rest in seconds
     */
    public void applyRecoil(float pitch, float yawVariance,
                            float kickDuration, float recoveryDuration) {
        if (!ModConfig.get().visuals.recoil.enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        this.recoilKickDuration    = kickDuration;
        this.recoilRecoveryDuration = recoveryDuration;

        targetPitchOffset = -pitch;
        targetYawOffset   = (client.world.random.nextFloat() - 0.5f) * 2.0f * yawVariance;

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
                if (elapsedSeconds >= recoilKickDuration) {
                    // Kickback complete — lock to peak and start recovery
                    currentPhase      = RecoilPhase.RECOVERY;
                    recoilStartTime   = System.currentTimeMillis();
                    currentPitchOffset = targetPitchOffset;
                    currentYawOffset   = targetYawOffset;
                } else {
                    float eased = easeOutQuad(elapsedSeconds / recoilKickDuration);
                    currentPitchOffset = targetPitchOffset * eased;
                    currentYawOffset   = targetYawOffset   * eased;
                }
                break;

            case RECOVERY:
                if (elapsedSeconds >= recoilRecoveryDuration) {
                    // Recovery complete — return to idle
                    currentPhase       = RecoilPhase.IDLE;
                    currentPitchOffset = 0.0f;
                    currentYawOffset   = 0.0f;
                    targetPitchOffset  = 0.0f;
                    targetYawOffset    = 0.0f;
                } else {
                    float eased = easeInOutCubic(elapsedSeconds / recoilRecoveryDuration);
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
