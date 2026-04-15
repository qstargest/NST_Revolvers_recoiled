package org.stargest.nst_revrecoiled.client.render.entity.player;

/**
 * Configuration for revolver arm pose animation.
 * Controls all animation parameters.
 * Adapted for Forge 1.20.1.
 */
public class RevolverArmConfig {

    public static final RevolverArmConfig DEFAULT = new RevolverArmConfig();

    // -- Lerp --
    public float lerpSpeed       = 0.7f;
    public float lerpSpeedRecoil = 0.12f;

    // -- Aim tracking --
    public boolean enableAimTracking = true;
    public float baseAimPitch      = -1.57f;
    public float baseAimYawOffset  = 0.2f;

    // -- Draw animation --
    public float drawStartRoll      = 0.4f;
    public float drawStartPitch     = 0.5f;
    public float drawDurationTicks  = 8.0f;
    public float drawEaseExponent   = 0.9f;

    // -- Reload animation --
    public float reloadCylinderRoll      = 0.9f;
    public float reloadCylinderYaw       = 0.25f;
    public float reloadSnapEdge          = 0.15f;
    public float reloadLeftPitchOffset   = 0.45f;
    public float reloadLeftYawOffset     = 0.55f;
    public float reloadShakeFreq         = 2.5f;
    public float reloadShakeAmp          = 0.025f;

    // -- Recoil --
    public float recoilTicksMin  = 2.0f;
    public float recoilTicksMax  = 8.0f;
    public float recoilPitchFactor = 0.6f;
    public float recoilRollFactor  = 0.2f;

    // -- Movement shake --
    public float shakeSprintAmp          = 0.06f;
    public float shakeWalkAmp            = 0.03f;
    public float shakeWalkMinSpeed       = 0.02f;
    public float shakeFreqSprint         = 1.6f;
    public float shakeFreqWalk           = 0.9f;
    public float shakePitchFactor        = 0.6f;
    public float shakeRollFactor         = 1.25f;
    public float shakeYawFactor          = 0.28f;
    public float shakeSecondaryFreqMult  = 1.7f;
    public float shakeSecondaryAmpMult   = 0.35f;
}
