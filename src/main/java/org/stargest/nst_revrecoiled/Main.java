package org.stargest.nst_revrecoiled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.util.*;

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
