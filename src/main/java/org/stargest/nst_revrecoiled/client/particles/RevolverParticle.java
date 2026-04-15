package org.stargest.nst_revrecoiled.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

/**
 * Custom particle for revolver visual effects.
 * Adapted for Forge 1.20.1.
 */
 public class RevolverParticle extends TextureSheetParticle {

    protected RevolverParticle(ClientLevel world, double x, double y, double z,
                             double vx, double vy, double vz,
                             SpriteSet spriteSet, int lifetime, float particleScale) {
        super(world, x, y, z, 0, 0, 0);

        this.setSpriteFromAge(spriteSet);
        this.lifetime = lifetime;
        this.quadSize = particleScale;
        this.alpha = 1.0f;
        this.hasPhysics = false;

        this.xd = (this.random.nextDouble() - 0.5) * 0.02;
        this.yd = (this.random.nextDouble() - 0.5) * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 1.0f - ((float) this.age / (float) this.lifetime);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class FireFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public FireFactory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType parameters, @NotNull ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(world, x, y, z, 0, 0, 0, spriteSet, 2, 0.1f);
        }
    }

    public static class ReloadFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public ReloadFactory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType parameters, @NotNull ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RevolverParticle(world, x, y, z, 0, 0, 0, spriteSet, 6, 0.12f);
        }
    }
}
