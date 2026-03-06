package org.stargest.nst_revrecoiled.client.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * Custom particle for revolver visual effects.
 * Renders as a translucent sprite that fades out over time.
 *
 * Two variants:
 * - Fire: Short-lived (2 ticks), small size, for muzzle flash
 * - Reload: Longer-lived (6 ticks), larger size, for reload smoke
 */
public class RevolverParticle extends SpriteBillboardParticle {

    /**
     * Private constructor called by factory classes.
     *
     * @param world Client world
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param vx Velocity X (unused, particles drift randomly)
     * @param vy Velocity Y (unused)
     * @param vz Velocity Z (unused)
     * @param spriteProvider Sprite provider for texture
     * @param maxAge Particle lifetime in ticks
     * @param particleScale Particle size multiplier
     */
    private RevolverParticle(ClientWorld world, double x, double y, double z,
                             double vx, double vy, double vz,
                             SpriteProvider spriteProvider, int maxAge, float particleScale) {
        super(world, x, y, z, 0, 0, 0);

        this.sprite = spriteProvider.getSprite(this.random);
        this.maxAge = maxAge;
        this.scale = particleScale;
        this.alpha = 1.0f;
        this.collidesWithWorld = false;

        // Add slight random drift for cloud effect
        this.velocityX = (random.nextDouble() - 0.5) * 0.02;
        this.velocityY = (random.nextDouble() - 0.5) * 0.02;
        this.velocityZ = (random.nextDouble() - 0.5) * 0.02;
    }

    @Override
    public void tick() {
        super.tick();

        // Smooth fade out over lifetime
        this.alpha = 1.0f - ((float) this.age / (float) this.maxAge);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * Factory for fire particles (muzzle flash).
     * Creates short-lived, small particles.
     */
    public static class FireFactory implements ParticleFactory<SimpleParticleType> {

        private static final int FIRE_MAX_AGE = 2;        // 0.1 seconds
        private static final float FIRE_SCALE = 0.1f;    // Small size

        private final SpriteProvider spriteProvider;

        public FireFactory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(
                    world, x, y, z,
                    0, 0, 0,
                    spriteProvider,
                    FIRE_MAX_AGE,
                    FIRE_SCALE
            );
        }
    }

    /**
     * Factory for reload particles (smoke puff).
     * Creates longer-lived, larger particles.
     */
    public static class ReloadFactory implements ParticleFactory<SimpleParticleType> {

        private static final int RELOAD_MAX_AGE = 6;      // 0.3 seconds
        private static final float RELOAD_SCALE = 0.12f;  // Medium size

        private final SpriteProvider spriteProvider;

        public ReloadFactory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(
                    world, x, y, z,
                    0, 0, 0,
                    spriteProvider,
                    RELOAD_MAX_AGE,
                    RELOAD_SCALE
            );
        }
    }
}
