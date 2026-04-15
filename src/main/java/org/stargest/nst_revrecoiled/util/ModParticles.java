package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for custom particle types used by revolver weapons.
 * Particle types:
 * - REVOLVER_FIRE: Muzzle flash effect when shooting
 * - REVOLVER_RELOAD: Smoke effect during reload animation
 * Adapted for Forge 1.20.1.
 */
public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Main.MODID);

    public static final RegistryObject<SimpleParticleType> REVOLVER_FIRE =
            PARTICLE_TYPES.register("revolver_fire", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> REVOLVER_RELOAD =
            PARTICLE_TYPES.register("revolver_reload", () -> new SimpleParticleType(false));
}
