package org.stargest.nst_revrecoiled.client.render.entity.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Items.RevolverArmPoseItem;
import org.stargest.nst_revrecoiled.util.ModConfig;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.IdentityHashMap;
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
 * Uses a consolidated state management approach with a single Map&lt;Integer,
 * PlayerRevolverState&gt;
 * instead of multiple parallel HashMaps, improving code organization and cache
 * locality.
 *
 * Provides cleanup methods to prevent memory leaks when players disconnect or
 * entities unload.
 */
public class PlayerArmPose {

    private static final Int2ObjectMap<PlayerRevolverState> playerStates = new Int2ObjectOpenHashMap<>();

    private static class PlayerRevolverState {
        ArmState rightArm;
        ArmState leftArm;
        boolean wasCharged = false;
        double lastShotTime = -100.0;
        double drawStartTime = 0.0;
        int lastItemId = -1;
    }

    private static class ArmState {
        float pitch, yaw, roll;

        ArmState(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }
    }

    private static final Map<Item, RevolverArmConfig> ARM_CONFIGS = new IdentityHashMap<>();

    public static void registerArmConfig(Item item, RevolverArmConfig config) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.getPath().equals("air")) {
            throw new IllegalStateException("registerArmConfig called before item registration for: " + item);
        }
        ARM_CONFIGS.put(item, config);
    }

    public static void applyRevolverPose(HumanoidModel<PlayerRenderState> model,
            PlayerRenderState renderState) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null)
            return;

        // Use renderTime with proper delta for smoothing
        double renderTime = client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Entity entity = client.level.getEntity(renderState.id);
        if (!(entity instanceof Player player))
            return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!(stack.getItem() instanceof RevolverArmPoseItem)) {
                playerStates.remove(renderState.id);
                return;
            }
        }

        RevolverArmConfig cfg = ARM_CONFIGS.getOrDefault(stack.getItem(), RevolverArmConfig.DEFAULT);
        PlayerRevolverState state = playerStates.computeIfAbsent(renderState.id, id -> new PlayerRevolverState());

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
        float drawProgress = Mth.clamp(
                (float) Math.pow(Mth.clamp((float) rawDrawProgress, 0f, 1f), cfg.drawEaseExponent),
                0f, 1f);

        boolean isCharging = player.isUsingItem();
        double timeSinceShot = renderTime - state.lastShotTime;

        float headPitch = renderState.xRot * ((float) Math.PI / 180F);
        float headYaw = renderState.yRot * ((float) Math.PI / 180F);

        float sideSign = (renderState.mainArm == HumanoidArm.RIGHT) ? -1.0f : 1.0f;
        float basePitch = cfg.baseAimPitch;
        float baseYaw = sideSign * cfg.baseAimYawOffset;

        float reloadBasePitch = Mth.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float reloadBaseYaw = baseYaw;

        if (ModConfig.get().visuals.enableAimTracking && drawProgress > 0.5f) {
            float trackingStrength = Mth.clamp((drawProgress - 0.5f) / 0.5f, 0f, 1f);
            basePitch += headPitch * trackingStrength;
            baseYaw += headYaw * trackingStrength;
        }

        float targetRP = Mth.lerp(drawProgress, cfg.drawStartPitch, basePitch);
        float targetRY = baseYaw;
        float targetRR = Mth.lerp(drawProgress, cfg.drawStartRoll, 0.0f);

        float targetLP = model.leftArm.xRot;
        float targetLY = model.leftArm.yRot;
        float targetLR = model.leftArm.zRot;

        if (drawProgress > 0.2f && timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax) {
            float recoil = (float) ((cfg.recoilTicksMax - timeSinceShot) / (cfg.recoilTicksMax - cfg.recoilTicksMin));
            targetRP -= recoil * cfg.recoilPitchFactor;
            targetRR -= recoil * cfg.recoilRollFactor;
        }

        if (isCharging) {
            float chargeProgress = getProgress(player, stack);
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
            float adaptiveLeftYaw = targetRY + (cfg.reloadLeftYawOffset * -sideSign);
            float adaptiveLeftRoll = 0.0f;

            targetLP = Mth.lerp(snap, targetLP, adaptiveLeftPitch);
            targetLY = Mth.lerp(snap, targetLY, adaptiveLeftYaw);
            targetLR = Mth.lerp(snap, targetLR, adaptiveLeftRoll);

            if (snap > 0.9f) {
                float shake = (float) (Math.sin(renderTime * cfg.reloadShakeFreq) * cfg.reloadShakeAmp);
                targetLP += shake;
                targetLR += shake * 2;
            }
        }

        // Bobbing/Shake logic
        float horizontalSpeed = (float) player.getDeltaMovement().horizontalDistance();
        float shakeAmp = player.isSprinting() ? cfg.shakeSprintAmp
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

        applyToModel(model, renderState, state, targetRP, targetRY, targetRR, targetLP, targetLY, targetLR,
                drawProgress, (float) timeSinceShot, cfg);
    }

    private static void applyToModel(HumanoidModel<PlayerRenderState> model,
            PlayerRenderState renderState,
            PlayerRevolverState state,
            float rp, float ry, float rr,
            float lp, float ly, float lr,
            float drawProgress, float timeSinceShot, RevolverArmConfig cfg) {

        float speed = cfg.lerpSpeed; // Lowered defaults in config, or we can scale here

        ArmState lastR = state.rightArm != null ? state.rightArm : new ArmState(rp, ry, rr);
        ArmState lastL = state.leftArm != null ? state.leftArm : new ArmState(lp, ly, lr);

        float rightLerp = (timeSinceShot >= cfg.recoilTicksMin && timeSinceShot < cfg.recoilTicksMax)
                ? cfg.lerpSpeedRecoil
                : speed;

        // Use exponential smoothing for maximum fluidity
        float fRp = Mth.lerp(rightLerp, lastR.pitch, rp);
        float fRy = Mth.lerp(speed, lastR.yaw, ry);
        float fRr = Mth.lerp(rightLerp, lastR.roll, rr);
        state.rightArm = new ArmState(fRp, fRy, fRr);

        model.rightArm.xRot = fRp;
        model.rightArm.yRot = fRy;
        model.rightArm.zRot = fRr;

        float fLp = Mth.lerp(speed, lastL.pitch, lp);
        float fLy = Mth.lerp(speed, lastL.yaw, ly);
        float fLr = Mth.lerp(speed, lastL.roll, lr);
        state.leftArm = new ArmState(fLp, fLy, fLr);

        model.leftArm.xRot = fLp;
        model.leftArm.yRot = fLy;
        model.leftArm.zRot = fLr;

        // Small adjustment for draw animation position
        model.rightArm.x = -5.0f;
        model.rightArm.y = Mth.lerp(drawProgress, 4.0f, 2.0f) + (renderState.isCrouching ? 2.0f : 0.0f);
        model.rightArm.z = Mth.lerp(drawProgress, 2.0f, 0.0f);
    }

    private static float getProgress(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof BaseRevolverItem revolver))
            return 0;
        int chargeMax = revolver.getChargeTimeTicks();
        int elapsed = revolver.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
        return chargeMax <= 0 ? 0 : Mth.clamp((float) elapsed / chargeMax, 0, 1);
    }

    public static void clearState(int entityId) {
        playerStates.remove(entityId);
    }

    public static void clearAllStates() {
        playerStates.clear();
    }
}
