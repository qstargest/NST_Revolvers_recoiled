package org.stargest.nst_revrecoiled.client.render.entity.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Items.RevolverArmPoseItem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Map;

/**
 * Manages custom arm poses and animations for revolver use.
 * Shared between player entities.json and mob entities.json to ensure consistent visual behavior.
 * Handles aiming, recoil, reloading, and movement-based camera/arm shake.
 */
public class PlayerArmPose {

    // -------------------------------------------------------------------------
    // Per-player state — consolidated into single map for better organization
    // -------------------------------------------------------------------------

    private static final Int2ObjectMap<PlayerRevolverState> playerStates = new Int2ObjectOpenHashMap<>();

    /**
     * Holds all per-entity revolver rendering state.
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
     * Entry point for player entities.json.
     * Called from PlayerEntityModelMixin injecting into setAngles().
     *
     * @param model       the biped entity model to modify
     * @param player      player entity directly
     */
    public static void applyRevolverPose(BipedEntityModel<?> model,
                                         PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double renderTime = client.world.getTime() + client.getRenderTickCounter().getTickDelta(false);
        Entity entity = client.world.getEntityById(player.getId());
        if (!(entity instanceof PlayerEntity)) return;

        ItemStack stack = player.getStackInHand(player.getActiveHand());
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            playerStates.remove(player.getId());
            return;
        }

        boolean isCharging = player.isUsingItem();
        float chargeProgress = getPlayerProgress(player, stack);
        boolean isLeftHanded = player.getMainArm() == Arm.LEFT;

        applyCore(model, player, player.getId(), stack, renderTime,
                isCharging, chargeProgress, isLeftHanded, player.isInSneakingPose());
    }

    /**
     * Entry point for RevolverBandit mob entities.json.
     * Called from RevolverBanditModel.setAngles() after vanilla angle setup.
     *
     * <p>Reads charge state from the mob's TrackedData so it is always
     * consistent with server-side weapon logic.
     *
     * @param model    the biped entity model to modify
     * @param bandit   the mob entity
     * @param entityId the entity's numeric ID (used as state map key)
     */
    public static void applyRevolverPoseForMob(BipedEntityModel<?> model,
                                               RevolverBanditEntity bandit,
                                               int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double renderTime = client.world.getTime() + client.getRenderTickCounter().getTickDelta(false);

        ItemStack stack = bandit.getMainHandStack();
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            playerStates.remove(entityId);
            return;
        }

        // Read synced state from TrackedData — always consistent with server
        boolean isCharging = bandit.isChargingRevolver();
        float chargeProgress = bandit.getChargeProgress();

        // Mobs use right hand; they don't sneak while shooting
        applyCore(model, bandit, entityId, stack, renderTime,
                isCharging, chargeProgress, false, bandit.isSneaking());
    }

    /**
     * Core animation logic shared by both player and mob entry points.
     * All entity-type-specific data has been extracted to primitives before
     * this is called, so the implementation is fully generic.
     *
     * @param model          model to write bone angles into
     * @param entity         living entity (for velocity, sprint state and state map)
     * @param entityId       entity ID used as the state map key
     * @param stack          held item stack (for arm config lookup and charge check)
     * @param renderTime     current fractional world time (world.time + tickDelta)
     * @param isCharging     true while the entity is actively reloading
     * @param chargeProgress reload progress 0–1
     * @param isLeftHanded   true if the dominant arm is left
     * @param isSneaking     true if the entity is sneaking
     */
    protected static void applyCore(BipedEntityModel<?> model,
                                  LivingEntity entity,
                                  int entityId,
                                  ItemStack stack,
                                  double renderTime,
                                  boolean isCharging,
                                  float chargeProgress,
                                  boolean isLeftHanded,
                                  boolean isSneaking) {

        RevolverArmConfig cfg = ARM_CONFIGS.getOrDefault(stack.getItem(), RevolverArmConfig.DEFAULT);

        PlayerRevolverState state = playerStates.computeIfAbsent(entityId,
                id -> new PlayerRevolverState());

        int currentItemHash = System.identityHashCode(stack.getItem());
        if (state.lastItemId != currentItemHash) {
            state.drawStartTime = renderTime;
            state.lastItemId = currentItemHash;
        }

        boolean isCharged = BaseRevolverItem.isCharged(stack);
        if (state.wasCharged && !isCharged) {
            state.lastShotTime = renderTime;
        }
        state.wasCharged = isCharged;

        double rawDrawProgress = (renderTime - state.drawStartTime) / cfg.drawDurationTicks;
        float drawProgress = MathHelper.clamp(
                (float) Math.pow(MathHelper.clamp((float) rawDrawProgress, 0f, 1f), cfg.drawEaseExponent),
                0f, 1f);

        double timeSinceShot = renderTime - state.lastShotTime;

        // ------------------------------------------------------------------
        // RIGHT ARM (primary / shooting hand)
        // ------------------------------------------------------------------
        float headPitch = model.head.pitch;
        float headYaw   = model.head.yaw;

        float sideSign = isLeftHanded ? 1.0f : -1.0f;
        float basePitch = cfg.baseAimPitch;
        float baseYaw   = sideSign * cfg.baseAimYawOffset;

        float reloadBasePitch = MathHelper.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float reloadBaseYaw   = baseYaw;

        boolean isPlayer = entity instanceof PlayerEntity;
        boolean enableTracking = isPlayer
                ? org.stargest.nst_revrecoiled.util.ModConfig.get().visuals.enablePlayerAimTracking
                : org.stargest.nst_revrecoiled.util.ModConfig.get().visuals.enableBanditAimTracking;

        if (enableTracking && drawProgress > 0.5f) {
            float trackingStrength = MathHelper.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += headPitch * trackingStrength;
            baseYaw   += headYaw  * trackingStrength;
        }

        float targetRP = MathHelper.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float targetRY = baseYaw;
        float targetRR = MathHelper.lerp(drawProgress, cfg.drawStartRoll, 0.0f);

        // ------------------------------------------------------------------
        // LEFT ARM (supporting hand)
        // ------------------------------------------------------------------
        ModelPart supportVanillaArm = isLeftHanded ? model.rightArm : model.leftArm;
        float targetLP = supportVanillaArm.pitch;
        float targetLY = supportVanillaArm.yaw;
        float targetLR = supportVanillaArm.roll;

        // Recoil
        if (drawProgress > 0.2f && timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax) {
            float recoil = (float) ((cfg.recoilTicksMax - timeSinceShot) / (cfg.recoilTicksMax - cfg.recoilTicksMin));
            targetRP -= recoil * cfg.recoilPitchFactor;
            targetRR -= recoil * cfg.recoilRollFactor;
        }

        // ------------------------------------------------------------------
        // ADAPTIVE RELOAD ANIMATION
        // ------------------------------------------------------------------
        if (isCharging) {
            float snap = (chargeProgress < cfg.reloadSnapEdge)
                    ? chargeProgress / cfg.reloadSnapEdge
                    : (chargeProgress > (1f - cfg.reloadSnapEdge)
                       ? (1f - chargeProgress) / cfg.reloadSnapEdge
                       : 1.0f);

            targetRR = snap * cfg.reloadCylinderRoll;
            targetRY -= snap * cfg.reloadCylinderYaw;
            targetRP = MathHelper.lerp(snap, targetRP, reloadBasePitch);
            targetRY = MathHelper.lerp(snap, targetRY, reloadBaseYaw - snap * cfg.reloadCylinderYaw);

            float adaptiveLeftPitch = targetRP + cfg.reloadLeftPitchOffset;
            float adaptiveLeftYaw   = targetRY + (cfg.reloadLeftYawOffset * -sideSign);

            targetLP = MathHelper.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = MathHelper.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = MathHelper.lerp(snap, targetLR, 0.0f);

            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * cfg.reloadShakeFreq) * cfg.reloadShakeAmp);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // ------------------------------------------------------------------
        // MOVEMENT SHAKE
        // ------------------------------------------------------------------
        Vec3d velocity = entity.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float shakeAmp  = entity.isSprinting() ? cfg.shakeSprintAmp
                : (horizontalSpeed > cfg.shakeWalkMinSpeed ? cfg.shakeWalkAmp : 0f);
        float shakeFreq = entity.isSprinting() ? cfg.shakeFreqSprint : cfg.shakeFreqWalk;

        if (shakeAmp > 0f) {
            float timeSeed = (float) ((renderTime + entityId * 7) * shakeFreq * 0.5);
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

        applyToModel(model, entityId, state, targetRP, targetRY, targetRR,
                targetLP, targetLY, targetLR, drawProgress, (float) timeSinceShot, cfg,
                isLeftHanded, isSneaking);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies interpolated arm rotations to the model with smooth transitions.
     * entityId replaces the previous renderState.id usage for generic support.
     */
    private static void applyToModel(BipedEntityModel<?> model,
                                     int entityId,
                                     PlayerRevolverState state,
                                     float rp, float ry, float rr,
                                     float lp, float ly, float lr,
                                     float drawProgress, float timeSinceShot,
                                     RevolverArmConfig cfg, boolean isLeftHanded,
                                     boolean isSneaking) {

        ArmState lastR = state.rightArm != null ? state.rightArm : new ArmState(rp, ry, rr);
        ArmState lastL = state.leftArm  != null ? state.leftArm  : new ArmState(lp, ly, lr);

        float rightLerp = (timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax)
                ? cfg.lerpSpeedRecoil : cfg.lerpSpeed;

        float fRp = MathHelper.lerp(rightLerp, lastR.pitch, rp);
        float fRy = MathHelper.lerp(cfg.lerpSpeed,  lastR.yaw,   ry);
        float fRr = MathHelper.lerp(rightLerp, lastR.roll,  rr);
        state.rightArm = new ArmState(fRp, fRy, fRr);

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

        shootingArm.pivotX = isLeftHanded ? 5.0f : -5.0f;
        shootingArm.pivotY = MathHelper.lerp(drawProgress, 4.0f, 2.0f) + (isSneaking ? 2.0f : 0.0f);
        shootingArm.pivotZ = MathHelper.lerp(drawProgress, 2.0f, 0.0f);
    }

    /**
     * Calculates the player's item-use progress as a 0→1 value.
     * Only used for the player entry point; mobs provide progress via TrackedData.
     */
    private static float getPlayerProgress(PlayerEntity player, ItemStack stack) {
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
