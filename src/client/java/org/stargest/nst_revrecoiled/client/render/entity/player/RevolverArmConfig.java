package org.stargest.nst_revrecoiled.client.render.entity.player;

/**
 * Configuration for revolver arm pose animation.
 * Controls all animation parameters: lerp speeds, aim tracking,
 * draw animation, reload animation, recoil, and movement shake.
 *
 * Use RevolverArmConfig.DEFAULT for standard revolver behavior.
 * Create a custom instance and register it via PlayerArmPose.registerArmConfig()
 * to override animation parameters for a specific item.
 *
 * Example addon usage:
 *   RevolverArmConfig sniperConfig = new RevolverArmConfig();
 *   sniperConfig.recoilPitchFactor = 1.2f;
 *   sniperConfig.recoilRollFactor  = 0.05f;
 *   sniperConfig.lerpSpeedRecoil   = 0.06f;
 *   PlayerArmPose.registerArmConfig(ModItems.SNIPER_REVOLVER, sniperConfig);
 */
public class RevolverArmConfig {

    /** Default configuration used by all base mod revolvers. */
    public static final RevolverArmConfig DEFAULT = new RevolverArmConfig();

    // ── Lerp ─────────────────────────────────────────────────────────────────

    /** Lerp factor for normal arm movement (0–1, higher = faster). */
    public float lerpSpeed       = 0.7f;

    /** Lerp factor during recoil for a dramatic slow snap-back. */
    public float lerpSpeedRecoil = 0.12f;

    // ── Aim tracking ──────────────────────────────────────────────────────────

    /** Whether the arm follows the player's look direction. */
    public boolean enableAimTracking = true;

    /** Base arm pitch when aiming forward (≈ -π/2 radians = straight forward). */
    public float baseAimPitch      = -1.57f;

    /** Horizontal arm offset based on dominant hand side. */
    public float baseAimYawOffset  = 0.2f;

    // ── Draw animation ────────────────────────────────────────────────────────

    /** Roll (tilt) of arm at the start of the draw animation. */
    public float drawStartRoll      = 0.4f;

    /** Pitch offset of the arm at the start of the draw animation. */
    public float drawStartPitch     = 0.5f;

    /** Duration in ticks over which the draw animation plays. */
    public float drawDurationTicks  = 8.0f;

    /** Easing exponent applied to raw draw progress for a slight ease-out. */
    public float drawEaseExponent   = 0.9f;

    // ── Reload animation ──────────────────────────────────────────────────────

    /** Max cylinder rotation in radians during reload. */
    public float reloadCylinderRoll      = 0.9f;

    /** Max cylinder yaw offset during reload. */
    public float reloadCylinderYaw       = 0.25f;

    /** Fraction of charge progress at which snap-blend edges occur. */
    public float reloadSnapEdge          = 0.15f;

    /** Left-hand pitch offset relative to right-hand during reload. */
    public float reloadLeftPitchOffset   = 0.45f;

    /** Left-hand yaw offset relative to right-hand during reload. */
    public float reloadLeftYawOffset     = 0.55f;

    /** Sine frequency of bullet-insertion shake effect (radians/tick). */
    public float reloadShakeFreq         = 2.5f;

    /** Amplitude of bullet-insertion shake (radians). */
    public float reloadShakeAmp          = 0.025f;

    // ── Recoil ────────────────────────────────────────────────────────────────

    /** Ticks after a shot at which recoil lerp kicks in. */
    public float recoilTicksMin  = 2.0f;

    /** Ticks after a shot at which recoil lerp ends. */
    public float recoilTicksMax  = 8.0f;

    /** Pitch reduction during recoil phase. */
    public float recoilPitchFactor = 0.6f;

    /** Roll reduction during recoil phase. */
    public float recoilRollFactor  = 0.2f;

    // ── Movement shake ────────────────────────────────────────────────────────

    /** Shake amplitude while sprinting. */
    public float shakeSprintAmp          = 0.06f;

    /** Shake amplitude while walking. */
    public float shakeWalkAmp            = 0.03f;

    /** Minimum horizontal speed to trigger walk shake. */
    public float shakeWalkMinSpeed       = 0.02f;

    /** Shake sine frequency while sprinting. */
    public float shakeFreqSprint         = 1.6f;

    /** Shake sine frequency while walking. */
    public float shakeFreqWalk           = 0.9f;

    /** Blend weight of pitch axis for walk shake. */
    public float shakePitchFactor        = 0.6f;

    /** Blend weight of roll axis for walk shake. */
    public float shakeRollFactor         = 1.25f;

    /** Blend weight of yaw axis for walk shake. */
    public float shakeYawFactor          = 0.28f;

    /** Secondary sine frequency multiplier for natural-looking shake. */
    public float shakeSecondaryFreqMult  = 1.7f;

    /** Secondary sine amplitude multiplier. */
    public float shakeSecondaryAmpMult   = 0.35f;
}
