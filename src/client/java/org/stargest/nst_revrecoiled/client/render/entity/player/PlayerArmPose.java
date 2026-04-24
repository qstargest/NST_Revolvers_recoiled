package org.stargest.nst_revrecoiled.client.render.entity.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Items.RevolverArmPoseItem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
    // Per-player state — consolidated into single map for better organization
    // -------------------------------------------------------------------------

    private static final Int2ObjectMap<PlayerRevolverState> playerStates = new Int2ObjectOpenHashMap<>();

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

    private static final Map<Item, RevolverArmConfig> ARM_CONFIGS =
            new java.util.IdentityHashMap<>();

    /**
     * Registers a custom arm pose config for a specific revolver item.
     * Falls back to RevolverArmConfig.DEFAULT if no config is registered.
     * Must be called during client initialization.
     *
     * @param item   the revolver item to bind the config to
     * @param config custom animation parameters
     * @throws IllegalStateException if the item is not yet registered
     */
    public static void registerArmConfig(Item item, RevolverArmConfig config) {
        Identifier id = Registries.ITEM.getId(item);
        if (id == null || id.equals(Registries.ITEM.getId(Items.AIR))) {
            throw new IllegalStateException(
                    "registerArmConfig called before item registration for: " + item
            );
        }
        ARM_CONFIGS.put(item, config);
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

        RevolverArmConfig cfg = ARM_CONFIGS.getOrDefault(stack.getItem(), RevolverArmConfig.DEFAULT);

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
        double rawDrawProgress = (renderTime - state.drawStartTime) / cfg.drawDurationTicks;
        float drawProgress = MathHelper.clamp(
                (float) Math.pow(MathHelper.clamp((float) rawDrawProgress, 0f, 1f), cfg.drawEaseExponent),
                0f, 1f);

        boolean isCharging = player.isUsingItem();
        double timeSinceShot = renderTime - state.lastShotTime;

        // ------------------------------------------------------------------
        // RIGHT ARM (primary / shooting hand)
        // ------------------------------------------------------------------
        float headPitch = model.head.pitch;
        float headYaw   = model.head.yaw;

        float sideSign = (renderState.mainArm == Arm.RIGHT) ? -1.0f : 1.0f;
        float basePitch = cfg.baseAimPitch;
        float baseYaw   = sideSign * cfg.baseAimYawOffset;

        // Store base reload position (without aim tracking)
        float reloadBasePitch = MathHelper.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float reloadBaseYaw   = baseYaw;

        // Apply aim tracking after draw animation is mostly complete
        if (org.stargest.nst_revrecoiled.util.ModConfig.get().visuals.enableAimTracking && drawProgress > 0.5f) {
            float trackingStrength = MathHelper.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += headPitch * trackingStrength;
            baseYaw   += headYaw  * trackingStrength;
        }

        float targetRP = MathHelper.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float targetRY = baseYaw;
        float targetRR = MathHelper.lerp(drawProgress, cfg.drawStartRoll, 0.0f);

        // ------------------------------------------------------------------
        // LEFT ARM (supporting hand) — initially unchanged from vanilla
        // ------------------------------------------------------------------
        boolean isLeftHanded = renderState.mainArm == Arm.LEFT;

        ModelPart supportVanillaArm = isLeftHanded ? model.rightArm : model.leftArm;
        float targetLP = supportVanillaArm.pitch;
        float targetLY = supportVanillaArm.yaw;
        float targetLR = supportVanillaArm.roll;

        // Recoil effect - applies after draw and within recoil time window
        if (drawProgress > 0.2f && timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax) {
            float recoil = (float) ((cfg.recoilTicksMax - timeSinceShot) / (cfg.recoilTicksMax - cfg.recoilTicksMin));
            targetRP -= recoil * cfg.recoilPitchFactor;
            targetRR -= recoil * cfg.recoilRollFactor;
        }

        // ------------------------------------------------------------------
        // ADAPTIVE RELOAD ANIMATION
        // ------------------------------------------------------------------
        if (isCharging) {
            float chargeProgress = getProgress(player, stack);

            // Snap factor creates smooth ease in/out at animation boundaries
            float snap = (chargeProgress < cfg.reloadSnapEdge)
                    ? chargeProgress / cfg.reloadSnapEdge
                    : (chargeProgress > (1f - cfg.reloadSnapEdge)
                    ? (1f - chargeProgress) / cfg.reloadSnapEdge
                    : 1.0f);

            // Rotate cylinder and adjust aim
            targetRR = snap * cfg.reloadCylinderRoll;
            targetRY -= snap * cfg.reloadCylinderYaw;
            targetRP = MathHelper.lerp(snap, targetRP, reloadBasePitch);
            targetRY = MathHelper.lerp(snap, targetRY, reloadBaseYaw - snap * cfg.reloadCylinderYaw);

            // Left hand follows right hand position
            float adaptiveLeftPitch = targetRP + cfg.reloadLeftPitchOffset;
            float adaptiveLeftYaw   = targetRY + (cfg.reloadLeftYawOffset * -sideSign);
            float adaptiveLeftRoll  = 0.0f;

            targetLP = MathHelper.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = MathHelper.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = MathHelper.lerp(snap, targetLR, adaptiveLeftRoll);

            // Bullet-insertion shake near end of reload
            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * cfg.reloadShakeFreq) * cfg.reloadShakeAmp);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // ------------------------------------------------------------------
        // MOVEMENT SHAKE
        // ------------------------------------------------------------------
        Vec3d velocity = player.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float shakeAmp  = player.isSprinting() ? cfg.shakeSprintAmp
                : (horizontalSpeed > cfg.shakeWalkMinSpeed ? cfg.shakeWalkAmp : 0f);
        float shakeFreq = player.isSprinting() ? cfg.shakeFreqSprint : cfg.shakeFreqWalk;

        if (shakeAmp > 0f) {
            float timeSeed = (float) ((renderTime + renderState.id * 7) * shakeFreq * 0.5);
            float shake = (float) (Math.sin(timeSeed) * shakeAmp
                    + Math.sin(timeSeed * cfg.shakeSecondaryFreqMult) * (shakeAmp * cfg.shakeSecondaryAmpMult));
            targetRP += shake * cfg.shakePitchFactor;
            targetRR += shake * cfg.shakeRollFactor;
            targetRY += shake * cfg.shakeYawFactor;
        }

        if (isLeftHanded) {
            targetRR = -targetRR;
            targetLR = -targetLR;
        }

        applyToModel(model, renderState, state, targetRP, targetRY, targetRR, targetLP, targetLY, targetLR,
                drawProgress, (float) timeSinceShot, cfg, isLeftHanded);
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
                                     float drawProgress, float timeSinceShot, RevolverArmConfig cfg, boolean isLeftHanded) {

        // Initialize with current targets if no previous state
        ArmState lastR = state.rightArm != null ? state.rightArm : new ArmState(rp, ry, rr);
        ArmState lastL = state.leftArm  != null ? state.leftArm  : new ArmState(lp, ly, lr);

        // Use slower lerp speed during recoil for a more dramatic effect
        float rightLerp = (timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax)
                ? cfg.lerpSpeedRecoil : cfg.lerpSpeed;

        // Interpolate right arm
        float fRp = MathHelper.lerp(rightLerp, lastR.pitch, rp);
        float fRy = MathHelper.lerp(cfg.lerpSpeed,  lastR.yaw,   ry);
        float fRr = MathHelper.lerp(rightLerp, lastR.roll,  rr);
        state.rightArm = new ArmState(fRp, fRy, fRr);

        model.rightArm.pitch = fRp;
        model.rightArm.yaw   = fRy;
        model.rightArm.roll  = fRr;

        // Interpolate left arm
        float fLp = MathHelper.lerp(cfg.lerpSpeed, lastL.pitch, lp);
        float fLy = MathHelper.lerp(cfg.lerpSpeed, lastL.yaw,   ly);
        float fLr = MathHelper.lerp(cfg.lerpSpeed, lastL.roll,  lr);
        state.leftArm = new ArmState(fLp, fLy, fLr);

        ModelPart shootingArm = isLeftHanded ? model.leftArm  : model.rightArm;
        ModelPart supportArm  = isLeftHanded ? model.rightArm : model.leftArm;

        shootingArm.pitch = fRp;
        shootingArm.yaw   = fRy;
        shootingArm.roll  = fRr;

        supportArm.pitch = fLp;
        supportArm.yaw   = fLy;
        supportArm.roll  = fLr;

        // Adjust arm pivot points for better visual positioning
        shootingArm.pivotX = isLeftHanded ? 5.0f : -5.0f;
        shootingArm.pivotY = MathHelper.lerp(drawProgress, 4.0f, 2.0f) + (renderState.sneaking ? 2.0f : 0.0f);
        shootingArm.pivotZ = MathHelper.lerp(drawProgress, 2.0f, 0.0f);
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
