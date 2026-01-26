package org.stargest.nst_revrecoiled;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;

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
    }
}
