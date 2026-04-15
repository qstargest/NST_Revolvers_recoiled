package org.stargest.nst_revrecoiled.client.managers;

import net.minecraft.client.Minecraft;
import org.stargest.nst_revrecoiled.util.ModConfig;

/**
 * Manages camera recoil effects for revolver weapons.
 * Adapted for Forge 1.20.1.
 */
public class CameraRecoilManager {

    private static final CameraRecoilManager INSTANCE = new CameraRecoilManager();

    private float currentPitchOffset = 0.0f;
    private float currentYawOffset   = 0.0f;
    private float targetPitchOffset  = 0.0f;
    private float targetYawOffset    = 0.0f;

    private float recoilKickDuration     = 0.1f;
    private float recoilRecoveryDuration = 0.45f;

    private long        recoilStartTime = 0;
    private RecoilPhase currentPhase    = RecoilPhase.IDLE;

    private enum RecoilPhase {
        IDLE,
        KICKBACK,
        RECOVERY
    }

    private CameraRecoilManager() {}

    public static CameraRecoilManager getInstance() {
        return INSTANCE;
    }

    public void applyRecoil() {
        ModConfig.VisualsConfig.RecoilConfig cfg = ModConfig.get().visuals.recoil;
        applyRecoil(cfg.pitch, cfg.yawVariance, cfg.kickDuration, cfg.recoveryDuration);
    }

    public void applyRecoil(float pitch, float yawVariance,
                            float kickDuration, float recoveryDuration) {
        if (!ModConfig.get().visuals.recoil.enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        this.recoilKickDuration    = kickDuration;
        this.recoilRecoveryDuration = recoveryDuration;

        targetPitchOffset = -pitch;
        targetYawOffset   = (client.level.random.nextFloat() - 0.5f) * 2.0f * yawVariance;

        recoilStartTime = System.currentTimeMillis();
        currentPhase    = RecoilPhase.KICKBACK;
    }

    public float[] updateAndGetOffsets() {
        if (currentPhase == RecoilPhase.IDLE) {
            return new float[]{0.0f, 0.0f};
        }

        float elapsedSeconds = (System.currentTimeMillis() - recoilStartTime) / 1000.0f;

        switch (currentPhase) {
            case KICKBACK:
                if (elapsedSeconds >= recoilKickDuration) {
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

    public void reset() {
        currentPhase       = RecoilPhase.IDLE;
        currentPitchOffset = 0.0f;
        currentYawOffset   = 0.0f;
        targetPitchOffset  = 0.0f;
        targetYawOffset    = 0.0f;
    }

    public boolean isRecoilActive() {
        return currentPhase != RecoilPhase.IDLE;
    }

    private float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4.0f * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }
}
