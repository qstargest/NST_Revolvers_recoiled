package org.stargest.nst_revrecoiled.client.render.entity.player;


import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Items.RevolverArmPoseItem;
import org.stargest.nst_revrecoiled.util.ModConfig;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Handles custom arm positioning and animations for revolver weapons.
 * Shared between player entities and mob entities to ensure consistent visual behaviour.
 * Manages first-person and third-person arm poses including:
 * <ul>
 *   <li>Aim tracking (following entity's look direction)</li>
 *   <li>Reload animations with adaptive hand positioning</li>
 *   <li>Recoil effects</li>
 *   <li>Movement shake</li>
 *   <li>Smooth interpolation between states</li>
 * </ul>
 *
 * <p>Uses a consolidated state management approach with a single
 * {@code Int2ObjectMap<PlayerRevolverState>} instead of multiple parallel HashMaps,
 * improving code organisation and cache locality.
 *
 * <p>Provides cleanup methods to prevent memory leaks when players disconnect or
 * entities unload.
 */
public class PlayerArmPose {

    // -------------------------------------------------------------------------
    // Per-entity state — keyed by entity ID (players AND mobs share the same map)
    // -------------------------------------------------------------------------

    private static final Int2ObjectMap<PlayerRevolverState> playerStates = new Int2ObjectOpenHashMap<>();

    /**
     * Holds all per-entity revolver rendering state.
     */
    private static class PlayerRevolverState {
        ArmState rightArm;
        ArmState leftArm;
        boolean wasCharged   = false;
        double  lastShotTime = -100.0;
        double  drawStartTime = 0.0;
        int     lastItemId   = -1;
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

    private static final Map<Item, RevolverArmConfig> ARM_CONFIGS = new IdentityHashMap<>();

    /**
     * Registers a custom arm pose config for a specific revolver item.
     * Falls back to {@link RevolverArmConfig#DEFAULT} if no config is registered.
     * Must be called during client initialisation.
     *
     * @param item   the revolver item to bind the config to
     * @param config custom animation parameters
     * @throws IllegalStateException if the item is not yet registered
     */
    public static void registerArmConfig(Item item, RevolverArmConfig config) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.getPath().equals("air")) {
            throw new IllegalStateException(
                    "registerArmConfig called before item registration for: " + item);
        }
        ARM_CONFIGS.put(item, config);
    }

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /**
     * Entry point for player entities.
     * Called from {@code PlayerModelMixin} injecting into {@code setupAnim()}.
     *
     * @param model       the humanoid model to modify
     * @param renderState the player's current render state
     */
    public static void applyRevolverPose(HumanoidModel<AbstractClientPlayer> model,
                                         AbstractClientPlayer renderState) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        float partialTick = client.getTimer().getGameTimeDeltaPartialTick(true);
        double renderTime = client.level.getGameTime() + partialTick;

        Entity entity = client.level.getEntity(renderState.getId());
        if (!(entity instanceof Player player)) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
                playerStates.remove(renderState.getId());
                return;
            }
        }

        boolean isCharging    = player.isUsingItem();
        float   chargeProgress = getPlayerProgress(player, stack);
        boolean isLeftHanded  = renderState.getMainArm() == HumanoidArm.LEFT;

        applyCore(model, player, renderState.getId(), stack, renderTime,
                isCharging, chargeProgress, isLeftHanded, renderState.isCrouching());
    }

    /**
     * Entry point for {@link RevolverBanditEntity} mob entities.
     * Called from {@code RevolverBanditModel.setupAnim()} after vanilla angle setup.
     *
     * <p>Reads charge state from the mob's synced data so it is always
     * consistent with server-side weapon logic.
     *
     * @param model    the biped entity model to modify
     * @param bandit   the mob entity
     * @param entityId the entity's numeric ID (used as state map key)
     */
    public static void applyRevolverPoseForMob(HumanoidModel<?> model,
                                               RevolverBanditEntity bandit,
                                               int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        float partialTick = client.getTimer().getGameTimeDeltaPartialTick(true);
        double renderTime = client.level.getGameTime() + partialTick;

        ItemStack stack = bandit.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            playerStates.remove(entityId);
            return;
        }

        // Read synced state from SynchedEntityData — always consistent with server
        boolean isCharging    = bandit.isChargingRevolver();
        float   chargeProgress = bandit.getChargeProgress();

        // Mobs always use right hand; they don't sneak while shooting
        applyCore(model, bandit, entityId, stack, renderTime,
                isCharging, chargeProgress, false, bandit.isCrouching());
    }

    // -------------------------------------------------------------------------
    // Shared animation core
    // -------------------------------------------------------------------------

    /**
     * Core animation logic shared by both player and mob entry points.
     * All entity-type-specific data has been extracted to primitives before
     * this is called, so the implementation is fully generic.
     *
     * @param model          model to write bone angles into
     * @param entity         living entity (for velocity, sprint state)
     * @param entityId       entity ID used as the state map key
     * @param stack          held item stack (for arm config lookup and charge check)
     * @param renderTime     current fractional world time (gameTime + tickDelta)
     * @param isCharging     true while the entity is actively reloading
     * @param chargeProgress reload progress 0–1
     * @param isLeftHanded   true if the dominant arm is left
     * @param isSneaking     true if the entity is sneaking/crouching
     */
    private static void applyCore(HumanoidModel<?> model,
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
            state.lastItemId    = currentItemHash;
        }

        boolean isCharged = BaseRevolverItem.isCharged(stack);
        if (state.wasCharged && !isCharged) {
            state.lastShotTime = renderTime;
        }
        state.wasCharged = isCharged;

        double rawDrawProgress = (renderTime - state.drawStartTime) / cfg.drawDurationTicks;
        float drawProgress = Mth.clamp(
                (float) Math.pow(Mth.clamp((float) rawDrawProgress, 0f, 1f), cfg.drawEaseExponent),
                0f, 1f);

        double timeSinceShot = renderTime - state.lastShotTime;

        // ------------------------------------------------------------------
        // RIGHT ARM (primary / shooting hand)
        // ------------------------------------------------------------------
        float headPitch = model.head.xRot;
        float headYaw   = model.head.yRot;

        float sideSign  = isLeftHanded ? 1.0f : -1.0f;
        float basePitch = cfg.baseAimPitch;
        float baseYaw   = sideSign * cfg.baseAimYawOffset;

        float reloadBasePitch = Mth.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float reloadBaseYaw   = baseYaw;

        boolean isPlayer      = entity instanceof Player;
        boolean enableTracking = isPlayer
                ? ModConfig.get().visuals.enablePlayerAimTracking
                : ModConfig.get().visuals.enableBanditAimTracking;

        if (enableTracking && drawProgress > 0.5f) {
            float trackingStrength = Mth.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += headPitch * trackingStrength;
            baseYaw   += headYaw  * trackingStrength;
        }

        float targetRP = Mth.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float targetRY = baseYaw;
        float targetRR = Mth.lerp(drawProgress, cfg.drawStartRoll, 0.0f);

        // ------------------------------------------------------------------
        // LEFT ARM (supporting hand) — seed from vanilla pose
        // ------------------------------------------------------------------
        ModelPart supportVanillaArm = isLeftHanded ? model.rightArm : model.leftArm;
        float targetLP = supportVanillaArm.xRot;
        float targetLY = supportVanillaArm.yRot;
        float targetLR = supportVanillaArm.zRot;

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
            targetRP = Mth.lerp(snap, targetRP, reloadBasePitch);
            targetRY = Mth.lerp(snap, targetRY, reloadBaseYaw - snap * cfg.reloadCylinderYaw);

            float adaptiveLeftPitch = targetRP + cfg.reloadLeftPitchOffset;
            float adaptiveLeftYaw   = targetRY + (cfg.reloadLeftYawOffset * -sideSign);

            targetLP = Mth.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = Mth.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = Mth.lerp(snap, targetLR, 0.0f);

            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * cfg.reloadShakeFreq) * cfg.reloadShakeAmp);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // ------------------------------------------------------------------
        // MOVEMENT SHAKE
        // ------------------------------------------------------------------
        float horizontalSpeed = (float) entity.getDeltaMovement().horizontalDistance();
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

        applyToModel(model, entityId, state,
                targetRP, targetRY, targetRR,
                targetLP, targetLY, targetLR,
                drawProgress, (float) timeSinceShot, cfg,
                isLeftHanded, isSneaking);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies interpolated arm rotations to the model with smooth transitions.
     * Uses exponential smoothing for maximum fluidity, with a faster lerp
     * speed during the recoil window.
     *
     * @param isLeftHanded true when the dominant arm is on the left
     * @param isSneaking   true when the entity is sneaking/crouching
     */
    private static void applyToModel(HumanoidModel<?> model,
                                     int entityId,
                                     PlayerRevolverState state,
                                     float rp, float ry, float rr,
                                     float lp, float ly, float lr,
                                     float drawProgress, float timeSinceShot,
                                     RevolverArmConfig cfg,
                                     boolean isLeftHanded, boolean isSneaking) {

        ArmState lastR = state.rightArm != null ? state.rightArm : new ArmState(rp, ry, rr);
        ArmState lastL = state.leftArm  != null ? state.leftArm  : new ArmState(lp, ly, lr);

        float rightLerp = (timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax)
                ? cfg.lerpSpeedRecoil : cfg.lerpSpeed;

        float fRp = Mth.lerp(rightLerp,    lastR.pitch, rp);
        float fRy = Mth.lerp(cfg.lerpSpeed, lastR.yaw,   ry);
        float fRr = Mth.lerp(rightLerp,    lastR.roll,  rr);
        state.rightArm = new ArmState(fRp, fRy, fRr);

        float fLp = Mth.lerp(cfg.lerpSpeed, lastL.pitch, lp);
        float fLy = Mth.lerp(cfg.lerpSpeed, lastL.yaw,   ly);
        float fLr = Mth.lerp(cfg.lerpSpeed, lastL.roll,  lr);
        state.leftArm = new ArmState(fLp, fLy, fLr);

        ModelPart shootingArm = isLeftHanded ? model.leftArm  : model.rightArm;
        ModelPart supportArm  = isLeftHanded ? model.rightArm : model.leftArm;

        shootingArm.xRot = fRp;
        shootingArm.yRot = fRy;
        shootingArm.zRot = fRr;

        supportArm.xRot = fLp;
        supportArm.yRot = fLy;
        supportArm.zRot = fLr;

        // Small position adjustment for draw animation
        shootingArm.x = isLeftHanded ? 5.0f : -5.0f;
        shootingArm.y = Mth.lerp(drawProgress, 4.0f, 2.0f) + (isSneaking ? 2.0f : 0.0f);
        shootingArm.z = Mth.lerp(drawProgress, 2.0f, 0.0f);
    }

    /**
     * Calculates the player's item-use progress as a 0→1 value.
     * Only used for the player entry point; mobs provide progress via synced data.
     *
     * @param player the player using the item
     * @param stack  the item being used
     * @return normalised charge progress [0, 1]
     */
    private static float getPlayerProgress(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof BaseRevolverItem revolver)) return 0;
        int chargeMax = revolver.getChargeTimeTicks();
        int elapsed   = revolver.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        return chargeMax <= 0 ? 0 : Mth.clamp((float) elapsed / chargeMax, 0, 1);
    }

    // -------------------------------------------------------------------------
    // Public cleanup methods
    // -------------------------------------------------------------------------

    /**
     * Clears cached state for a specific entity (player or mob).
     * Must be called when an entity unloads to prevent memory leaks.
     *
     * @param entityId the entity ID to clear state for
     */
    public static void clearState(int entityId) {
        playerStates.remove(entityId);
    }

    /**
     * Clears all cached entity states.
     * Called when disconnecting from a server to prevent stale data.
     */
    public static void clearAllStates() {
        playerStates.clear();
    }
}
