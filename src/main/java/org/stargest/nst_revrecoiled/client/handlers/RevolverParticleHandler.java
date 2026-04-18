package org.stargest.nst_revrecoiled.client.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModParticles;

/**
 * Handles immediate particle spawning for revolver fire and reload effects.
 * Calculates correct spawn positions for both first-person and third-person camera modes.
 *
 * Particle positions are calculated using vector math to account for:
 * - Player look direction
 * - Camera perspective
 * - Weapon positioning in hand
 * - Body rotation (third-person only)
 *
 * Both fire and reload particles bypass GeckoLib animation keyframes entirely:
 * - Fire particles: spawned via spawnFireImmediate(), called from the clientFireCallback
 *   registered in ClientEvents, which is invoked directly by
 *   BaseRevolverItem.use() at the exact moment of firing.
 * - Reload particles: spawned via spawnReloadImmediate(), called from the
 *   RevolverReloadParticlePacket receiver registered in PacketHandler,
 *   which is sent server-side at tick 20 of the charge — matching the bullet-insertion
 *   keyframe timing without depending on GeckoLib keyframe callbacks.
 *
 * Adapted for Forge 1.20.1.
 */
public class RevolverParticleHandler {

    private static final double FP_FIRE_F   =  0.85, FP_FIRE_R   =  0.40, FP_FIRE_U   = -0.25;
    private static final double FP_RELOAD_F =  0.15, FP_RELOAD_R =  0.10, FP_RELOAD_U = -0.15;

    private static final double TP_FIRE_SHOULDER = 0.35;
    private static final double TP_FIRE_F        = 1.0;
    private static final double TP_FIRE_R        = -0.20;
    private static final double TP_FIRE_U        = 0.05;

    private static final double TP_RELOAD_F = 0.20, TP_RELOAD_R = -0.10, TP_RELOAD_U = 0.0;

    private static Vec3 calcFirstPersonPos(Minecraft client, LivingEntity holder,
                                             float tickDelta, String effect) {
        Vec3 base  = client.gameRenderer.getMainCamera().getPosition();
        Vec3 fwd   = holder.getViewVector(tickDelta);
        Vec3 up    = Vec3.directionFromRotation(holder.getXRot() - 90, holder.getYRot()).normalize();
        Vec3 right = fwd.cross(up).normalize();

        return "fire".equals(effect)
                ? offset(base, fwd, right, up, FP_FIRE_F,   FP_FIRE_R,   FP_FIRE_U)
                : offset(base, fwd, right, up, FP_RELOAD_F, FP_RELOAD_R, FP_RELOAD_U);
    }

    private static Vec3 calcThirdPersonPos(LivingEntity holder, float tickDelta, String effect) {
        float pitch  = holder.getXRot();
        float yaw    = holder.getYRot();
        double lerpX = Mth.lerp(tickDelta, holder.xo, holder.getX());
        double lerpZ = Mth.lerp(tickDelta, holder.zo, holder.getZ());

        if ("fire".equals(effect)) {
            float bodyYaw = Mth.lerp(tickDelta, holder.yBodyRotO, holder.yBodyRot);
            double lerpY  = Mth.lerp(tickDelta, holder.yo, holder.getY()) + 1.45;

            Vec3 bodyRight = Vec3.directionFromRotation(0, bodyYaw + 90).normalize();
            Vec3 lookFwd   = Vec3.directionFromRotation(pitch, yaw);
            Vec3 lookUp    = Vec3.directionFromRotation(pitch - 90, yaw);
            Vec3 lookRight = lookFwd.cross(lookUp).normalize();

            Vec3 shoulder = new Vec3(
                    lerpX + bodyRight.x * TP_FIRE_SHOULDER,
                    lerpY,
                    lerpZ + bodyRight.z * TP_FIRE_SHOULDER
            );
            return offset(shoulder, lookFwd, lookRight, lookUp, TP_FIRE_F, TP_FIRE_R, TP_FIRE_U);

        } else {
            double lerpY = Mth.lerp(tickDelta, holder.yo, holder.getY()) + 1.2;

            Vec3 bodyFwd   = Vec3.directionFromRotation(0, yaw);
            Vec3 bodyRight = bodyFwd.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 base      = new Vec3(lerpX, lerpY, lerpZ);

            return offset(base, bodyFwd, bodyRight, Vec3.ZERO, TP_RELOAD_F, TP_RELOAD_R, TP_RELOAD_U);
        }
    }

    public static Vec3 calcFirePosition(LivingEntity holder) {
        Minecraft client = Minecraft.getInstance();
        float tickDelta = client.getFrameTime();
        boolean isLocal     = (holder == client.player);
        boolean firstPerson = isLocal && client.options.getCameraType().isFirstPerson();

        return firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, "fire")
                : calcThirdPersonPos(holder, tickDelta, "fire");
    }

    public static Vec3 calcReloadPosition(LivingEntity holder) {
        Minecraft client = Minecraft.getInstance();
        float tickDelta = client.getFrameTime();
        boolean isLocal     = (holder == client.player);
        boolean firstPerson = isLocal && client.options.getCameraType().isFirstPerson();

        return firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, "reload")
                : calcThirdPersonPos(holder, tickDelta, "reload");
    }

    private static Vec3 offset(Vec3 base, Vec3 fwd, Vec3 right, Vec3 up, double f, double r, double u) {
        return base.add(fwd.scale(f)).add(right.scale(r)).add(up.scale(u));
    }

    public static void spawnFireImmediate(LivingEntity holder) {
        if (!ModConfig.get().visuals.particles.enableFireParticle) return;
        spawnScattered(holder.level(), ModParticles.REVOLVER_FIRE.get(), calcFirePosition(holder), 12, 0.01);
    }

    public static void spawnReloadImmediate(LivingEntity holder) {
        if (!ModConfig.get().visuals.particles.enableReloadParticle) return;
        spawnScattered(holder.level(), ModParticles.REVOLVER_RELOAD.get(), calcReloadPosition(holder), 20, 0.04);
    }

    public static void spawnScattered(Level world, ParticleOptions type, Vec3 pos, int count, double spread) {
        for (int i = 0; i < count; i++) {
            world.addParticle(type,
                    pos.x + world.random.nextGaussian() * spread,
                    pos.y + world.random.nextGaussian() * spread,
                    pos.z + world.random.nextGaussian() * spread,
                    0, 0, 0);
        }
    }
}
