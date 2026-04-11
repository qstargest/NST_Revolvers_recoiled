package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.NstRevRecoiled;


/**
 * Registry for custom particle types used by revolver weapons.
 * Particle types:
 * - REVOLVER_FIRE: Muzzle flash effect when shooting
 * - REVOLVER_RELOAD: Smoke effect during reload animation
 */
public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, NstRevRecoiled.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REVOLVER_FIRE =
            PARTICLE_TYPES.register("revolver_fire", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REVOLVER_RELOAD =
            PARTICLE_TYPES.register("revolver_reload", () -> new SimpleParticleType(false));
}
