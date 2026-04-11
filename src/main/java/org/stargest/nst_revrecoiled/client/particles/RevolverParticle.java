package org.stargest.nst_revrecoiled.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

/**
 * Custom particle for revolver visual effects.
 * Renders as a translucent sprite that fades out over time.
 *
 * Two variants:
 * - Fire: Short-lived (2 ticks), small size, for muzzle flash
 * - Reload: Longer-lived (6 ticks), larger size, for reload smoke
 */
 public class RevolverParticle extends TextureSheetParticle {

    protected RevolverParticle(ClientLevel world, double x, double y, double z,
                             double vx, double vy, double vz,
                             SpriteSet spriteSet, int lifetime, float particleScale) {
        super(world, x, y, z, 0, 0, 0);

        this.setSprite(spriteSet.get(this.random));
        this.lifetime = lifetime;
        this.quadSize = particleScale;
        this.alpha = 1.0f;
        this.hasPhysics = false;

        // Add slight random drift for cloud effect
        this.xd = (this.random.nextDouble() - 0.5) * 0.02;
        this.yd = (this.random.nextDouble() - 0.5) * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.02;
    }

    @Override
    public void tick() {
        super.tick();

        // Smooth fade out over lifetime
        this.alpha = 1.0f - ((float) this.age / (float) this.lifetime);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * Factory for fire particles (muzzle flash).
     * Creates short-lived, small particles.
     */
    public static class FireFactory implements ParticleProvider<SimpleParticleType> {

        private static final int FIRE_MAX_AGE = 2;        // 0.1 seconds
        private static final float FIRE_SCALE = 0.1f;    // Small size

        private final SpriteSet spriteSet;

        public FireFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType parameters, @NotNull ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(
                    world, x, y, z,
                    0, 0, 0,
                    spriteSet,
                    FIRE_MAX_AGE,
                    FIRE_SCALE
            );
        }
    }

    public static class ReloadFactory implements ParticleProvider<SimpleParticleType> {

        private static final int RELOAD_MAX_AGE = 6;      // 0.3 seconds
        private static final float RELOAD_SCALE = 0.12f;  // Medium size

        private final SpriteSet spriteSet;

        public ReloadFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType parameters, @NotNull ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(
                    world, x, y, z,
                    0, 0, 0,
                    spriteSet,
                    RELOAD_MAX_AGE,
                    RELOAD_SCALE
            );
        }
    }
}
