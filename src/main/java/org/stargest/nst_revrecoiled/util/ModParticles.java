package org.stargest.nst_revrecoiled.util;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for custom particle types used by revolver weapons.
 *
 * Particle types:
 * - REVOLVER_FIRE: Muzzle flash effect when shooting
 * - REVOLVER_RELOAD: Smoke effect during reload animation
 */
public class ModParticles {

    public static DefaultParticleType REVOLVER_FIRE;
    public static DefaultParticleType REVOLVER_RELOAD;

    /**
     * Registers all custom particle types.
     * Should be called during mod initialization.
     */
    public static void init() {
        REVOLVER_FIRE = Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(Main.MOD_ID, "revolver_fire"),
                FabricParticleTypes.simple()
        );

        REVOLVER_RELOAD = Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(Main.MOD_ID, "revolver_reload"),
                FabricParticleTypes.simple()
        );
    }
}
