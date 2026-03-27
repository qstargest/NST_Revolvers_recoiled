package org.stargest.nst_revrecoiled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.util.*;

import static org.stargest.nst_revrecoiled.Structures.ModStructures.*;

/**
 * Main mod initializer for the Revolver mod.
 * Handles server-side and common initialization.
 */
public class Main implements ModInitializer {
    public static final String MOD_ID = "nst_revrecoiled";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {}", MOD_ID);

        ModItems.init();
        ModEntities.init();
        ModParticles.init();
        ModBlocks.init();
        ModVillagers.init();
        ModScreenHandlers.register();
        AssemblyCraftC2SPacket.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!AssemblyRecipes.isFrozen()) {
                AssemblyRecipes.freeze();
            }
            reinit(server);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resourceManager, success) -> {
                    if (success) {
                        reinit(server);
                    }
                }
        );

        PayloadTypeRegistry.playS2C().register(
                RevolverReloadParticlePacket.ID,
                RevolverReloadParticlePacket.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                RevolverFireParticlePacket.ID,
                RevolverFireParticlePacket.CODEC
        );
    }
}
