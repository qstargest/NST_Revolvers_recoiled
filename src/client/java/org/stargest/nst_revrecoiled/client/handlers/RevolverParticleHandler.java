package org.stargest.nst_revrecoiled.client.handlers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.render.revolvers.BaseRevolverItemRenderer;
import org.stargest.nst_revrecoiled.util.ModParticles;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;

import java.util.function.Consumer;

/**
 * Handles GeckoLib keyframe particle events for revolver animations.
 * Spawns REVOLVER_FIRE and REVOLVER_RELOAD particles at the correct world position
 * for both first-person and third-person camera modes.
 *
 * Particle positions are calculated using vector math to account for:
 * - Player look direction
 * - Camera perspective
 * - Weapon positioning in hand
 * - Body rotation (third-person only)
 *
 * Registered from the client initializer via BaseRevolverItem.setParticleKeyframeHandler()
 * to avoid importing client-only classes from the common item class.
 *
 * The holder entity is resolved via BaseRevolverItemRenderer.getCurrentHolder() since
 * DataTickets.ENTITY is not populated for item animatables in GeckoLib 4.8.5.
 *
 * Fire particles bypass GeckoLib animation delay by using spawnFireImmediate() called
 * directly from BaseRevolverItem.use() via clientFireCallback. This ensures particles
 * appear exactly when the shot is fired, not when the animation keyframe is reached.
 */
public class RevolverParticleHandler implements Consumer<ParticleKeyframeEvent<BaseRevolverItem>> {

    // First-person offsets (forward / right / up)
    private static final double FP_FIRE_F   =  0.85, FP_FIRE_R   =  0.40, FP_FIRE_U   = -0.25;
    private static final double FP_RELOAD_F =  0.15, FP_RELOAD_R =  0.10, FP_RELOAD_U = -0.15;

    // Third-person fire offsets
    private static final double TP_FIRE_SHOULDER = 0.35;   // Body-right offset to shoulder
    private static final double TP_FIRE_F        = 1.0;
    private static final double TP_FIRE_R        = -0.20;
    private static final double TP_FIRE_U        = 0.05;

    // Third-person reload offsets
    private static final double TP_RELOAD_F = 0.20, TP_RELOAD_R = -0.10, TP_RELOAD_U = 0.0;

    /**
     * Handles particle keyframe events from GeckoLib animations.
     * Fire events are ignored since fire particles use immediate spawning.
     * Reload events spawn particles at the calculated position.
     */
    @Override
    public void accept(ParticleKeyframeEvent<BaseRevolverItem> event) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Get holder from our base renderer
        LivingEntity holder = BaseRevolverItemRenderer.getCurrentHolder();
        if (holder == null) return;

        String effect = event.getKeyframeData().getEffect();

        boolean isLocal = (holder == client.player);
        // Fire particles use immediate spawning, ignore keyframe event
        if ("fire".equals(effect)) return;

        World world = holder.getWorld();
        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        // Logic: if this is the local player, check their camera settings.
        // If this is another player, they are ALWAYS in third-person for us.
        boolean firstPerson = isLocal && client.options.getPerspective().isFirstPerson();

        Vec3d spawnPos = firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, effect)
                : calcThirdPersonPos(holder, tickDelta, effect);

        spawnParticles(world, spawnPos, effect);
    }

    // Position calculation helpers

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
     */
    private static Vec3d calcThirdPersonPos(LivingEntity holder, float tickDelta, String effect) {
        float pitch  = holder.getPitch(tickDelta);
        float yaw    = holder.getYaw(tickDelta);
        double lerpX = MathHelper.lerp(tickDelta, holder.prevX, holder.getX());
        double lerpZ = MathHelper.lerp(tickDelta, holder.prevZ, holder.getZ());

        if ("fire".equals(effect)) {
            // Fire particle spawns from shoulder position
            float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, holder.prevBodyYaw, holder.bodyYaw);
            double lerpY  = MathHelper.lerp(tickDelta, holder.prevY, holder.getY()) + 1.45;

            Vec3d bodyRight = Vec3d.fromPolar(0, bodyYaw + 90).normalize();
            Vec3d lookFwd   = Vec3d.fromPolar(pitch, yaw);
            Vec3d lookUp    = Vec3d.fromPolar(pitch - 90, yaw);
            Vec3d lookRight = lookFwd.crossProduct(lookUp).normalize();

            // Calculate shoulder position
            Vec3d shoulder = new Vec3d(
                    lerpX + bodyRight.x * TP_FIRE_SHOULDER,
                    lerpY,
                    lerpZ + bodyRight.z * TP_FIRE_SHOULDER
            );
            return offset(shoulder, lookFwd, lookRight, lookUp, TP_FIRE_F, TP_FIRE_R, TP_FIRE_U);

        } else {
            // Reload particle spawns from hand position
            double lerpY = MathHelper.lerp(tickDelta, holder.prevY, holder.getY()) + 1.2;

            Vec3d bodyFwd   = Vec3d.fromPolar(0, yaw);
            Vec3d bodyRight = bodyFwd.crossProduct(new Vec3d(0, 1, 0)).normalize();
            Vec3d base      = new Vec3d(lerpX, lerpY, lerpZ);

            return offset(base, bodyFwd, bodyRight, Vec3d.ZERO, TP_RELOAD_F, TP_RELOAD_R, TP_RELOAD_U);
        }
    }

    /**
     * Applies directional offset to base position.
     * Formula: base + (forward * f) + (right * r) + (up * u)
     */
    private static Vec3d offset(Vec3d base, Vec3d fwd, Vec3d right, Vec3d up, double f, double r, double u) {
        return base.add(fwd.multiply(f)).add(right.multiply(r)).add(up.multiply(u));
    }

    // Particle spawning

    /**
     * Spawns fire particles immediately at shot time, bypassing GeckoLib keyframe delay.
     * Called directly from BaseRevolverItem.use() on the client side via clientFireCallback.
     * This ensures particles appear exactly when the shot is fired, providing instant visual feedback.
     *
     * @param holder The living entity firing the revolver
     */
    public static void spawnFireImmediate(LivingEntity holder) {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        boolean isLocal = (holder == client.player);
        boolean firstPerson = isLocal && client.options.getPerspective().isFirstPerson();

        Vec3d spawnPos = firstPerson
                ? calcFirstPersonPos(client, holder, tickDelta, "fire")
                : calcThirdPersonPos(holder, tickDelta, "fire");

        spawnScattered(holder.getWorld(), ModParticles.REVOLVER_FIRE, spawnPos, 12, 0.01);
    }

    /**
     * Spawns appropriate particle type at the given position.
     * Fire particles are short-lived with minimal spread for precise muzzle flash.
     * Reload particles last longer with wider spread for smoke effect.
     */
    private void spawnParticles(World world, Vec3d pos, String effect) {
        if ("fire".equals(effect)) {
            // Fire particles now use immediate spawning (this path shouldn't be reached)
            spawnScattered(world, ModParticles.REVOLVER_FIRE, pos, 12, 0.01);
        } else {
            spawnScattered(world, ModParticles.REVOLVER_RELOAD, pos, 20, 0.04);
        }
    }

    /**
     * Spawns multiple particles in a scattered pattern using Gaussian distribution.
     *
     * @param world  World to spawn in
     * @param type   Particle type to spawn
     * @param pos    Center position
     * @param count  Number of particles
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
