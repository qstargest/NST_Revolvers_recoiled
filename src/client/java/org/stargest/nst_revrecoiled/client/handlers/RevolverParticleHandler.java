package org.stargest.nst_revrecoiled.client.handlers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
 *   registered in Nst_revolvers_recoiledClient, which is invoked directly by
 *   BaseRevolverItem.use() at the exact moment of firing.
 * - Reload particles: spawned via spawnReloadImmediate(), called from the
 *   RevolverReloadParticlePacket receiver registered in Nst_revolvers_recoiledClient,
 *   which is sent server-side at tick 20 of the charge — matching the bullet-insertion
 *   keyframe timing without depending on GeckoLib keyframe callbacks.
 *
 * The holder entity is resolved from the network packet's entity ID rather than from
 * GeckoLib DataTickets.ENTITY, which is not populated for item animatables in GeckoLib 4.8.5.
 */
public class RevolverParticleHandler {

    // First-person offsets (forward / right / up)
    private static final double FP_FIRE_F   =  0.85, FP_FIRE_R   =  0.40, FP_FIRE_U   = -0.25;
    private static final double FP_RELOAD_F =  0.15, FP_RELOAD_R =  0.10, FP_RELOAD_U = -0.15;

    // Third-person fire offsets
    private static final double TP_FIRE_SHOULDER = 0.35; // Body-right offset to shoulder
    private static final double TP_FIRE_F        = 1.0;
    private static final double TP_FIRE_R        = -0.20;
    private static final double TP_FIRE_U        = 0.05;

    // Third-person reload offsets
    private static final double TP_RELOAD_F = 0.20, TP_RELOAD_R = -0.10, TP_RELOAD_U = 0.0;

    // -------------------------------------------------------------------------
    // Position calculation helpers
    // -------------------------------------------------------------------------

    /**
     * Calculates particle spawn position for first-person view.
     * Uses camera position and holder look vectors for accurate placement.
     * Only called when the holder is the local player.
     */
    private static Vec3d calcFirstPersonPos(MinecraftClient client, LivingEntity holder,
                                            float tickDelta, String effect) {
        Vec3d base  = client.gameRenderer.getCamera().getPos();
        Vec3d fwd   = holder.getRotationVec(tickDelta);
        Vec3d up    = Vec3d.fromPolar(holder.getPitch(tickDelta) - 90, holder.getYaw(tickDelta)).normalize();
        Vec3d right = fwd.crossProduct(up).normalize();

        return "fire".equals(effect)
                ? offset(base, fwd, right, up, FP_FIRE_F,   FP_FIRE_R,   FP_FIRE_U)
                : offset(base, fwd, right, up, FP_RELOAD_F, FP_RELOAD_R, FP_RELOAD_U);
    }

    /**
     * Calculates particle spawn position for third-person view.
     * Accounts for body rotation and shoulder positioning for realistic placement.
     * Fire particles spawn from the barrel/shoulder; reload particles from the hand.
     */
    private static Vec3d calcThirdPersonPos(LivingEntity holder, float tickDelta, String effect) {
        float pitch  = holder.getPitch(tickDelta);
        float yaw    = holder.getYaw(tickDelta);
        double lerpX = MathHelper.lerp(tickDelta, holder.prevX, holder.getX());
        double lerpZ = MathHelper.lerp(tickDelta, holder.prevZ, holder.getZ());

        if ("fire".equals(effect)) {
            float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, holder.prevBodyYaw, holder.bodyYaw);
            double lerpY  = MathHelper.lerp(tickDelta, holder.prevY, holder.getY()) + 1.45;

            Vec3d bodyRight = Vec3d.fromPolar(0, bodyYaw + 90).normalize();
            Vec3d lookFwd   = Vec3d.fromPolar(pitch, yaw);
            Vec3d lookUp    = Vec3d.fromPolar(pitch - 90, yaw);
            Vec3d lookRight = lookFwd.crossProduct(lookUp).normalize();

            Vec3d shoulder = new Vec3d(
                    lerpX + bodyRight.x * TP_FIRE_SHOULDER,
                    lerpY,
                    lerpZ + bodyRight.z * TP_FIRE_SHOULDER
            );
            return offset(shoulder, lookFwd, lookRight, lookUp, TP_FIRE_F, TP_FIRE_R, TP_FIRE_U);

        } else {
            double lerpY = MathHelper.lerp(tickDelta, holder.prevY, holder.getY()) + 1.2;

            Vec3d bodyFwd   = Vec3d.fromPolar(0, yaw);
            Vec3d bodyRight = bodyFwd.crossProduct(new Vec3d(0, 1, 0)).normalize();
            Vec3d base      = new Vec3d(lerpX, lerpY, lerpZ);

            return offset(base, bodyFwd, bodyRight, Vec3d.ZERO, TP_RELOAD_F, TP_RELOAD_R, TP_RELOAD_U);
        }
    }

    /**
     * Applies directional offset to a base position.
     * Formula: base + (forward * f) + (right * r) + (up * u)
     */
    private static Vec3d offset(Vec3d base, Vec3d fwd, Vec3d right, Vec3d up, double f, double r, double u) {
        return base.add(fwd.multiply(f)).add(right.multiply(r)).add(up.multiply(u));
    }

    // -------------------------------------------------------------------------
    // Particle spawning
    // -------------------------------------------------------------------------

    /**
     * Spawns muzzle-flash particles at the barrel position immediately on firing.
     * Called from the clientFireCallback registered in Nst_revolvers_recoiledClient,
     * which is invoked directly by BaseRevolverItem.use() on the client at shot time.
     * This bypasses GeckoLib animation delay, providing instant visual feedback.
     *
     * The local player receives particles via this path; other players receive them
     * via RevolverFireParticlePacket to avoid duplication.
     *
     * @param holder the living entity firing the revolver
     */
    public static void spawnFireImmediate(LivingEntity holder) {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        boolean isLocal     = (holder == client.player);
        boolean firstPerson = isLocal && client.options.getPerspective().isFirstPerson();

        Vec3d spawnPos = firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, "fire")
                : calcThirdPersonPos(holder, tickDelta, "fire");

        spawnScattered(holder.getWorld(), ModParticles.REVOLVER_FIRE, spawnPos, 12, 0.01);
    }

    /**
     * Spawns reload smoke particles at the cylinder position on bullet insertion.
     * Called from the RevolverReloadParticlePacket receiver registered in
     * Nst_revolvers_recoiledClient, which is triggered by the server at tick 20
     * of the charge — matching the bullet-insertion keyframe timing.
     *
     * Sent to all players including the shooter, so no local-player skip is needed.
     *
     * @param holder the living entity reloading the revolver
     */
    public static void spawnReloadImmediate(LivingEntity holder) {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        boolean isLocal     = (holder == client.player);
        boolean firstPerson = isLocal && client.options.getPerspective().isFirstPerson();

        Vec3d spawnPos = firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, "reload")
                : calcThirdPersonPos(holder, tickDelta, "reload");

        spawnScattered(holder.getWorld(), ModParticles.REVOLVER_RELOAD, spawnPos, 20, 0.04);
    }

    /**
     * Spawns multiple particles in a scattered pattern using Gaussian distribution.
     *
     * @param world  world to spawn in
     * @param type   particle type to spawn
     * @param pos    center position
     * @param count  number of particles
     * @param spread Gaussian spread radius
     */
    private static void spawnScattered(World world, ParticleEffect type, Vec3d pos, int count, double spread) {
        for (int i = 0; i < count; i++) {
            world.addParticle(type,
                    pos.x + world.random.nextGaussian() * spread,
                    pos.y + world.random.nextGaussian() * spread,
                    pos.z + world.random.nextGaussian() * spread,
                    0, 0, 0);
        }
    }
}
