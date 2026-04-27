package org.stargest.nst_revrecoiled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
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

    /**
     * Called upon mod initialization.
     * Registers network payloads, configuration, registries, and server events.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {}", MOD_ID);

        // Register networking payloads FIRST to avoid client sync crashes
        AssemblyCraftC2SPacket.register();

        // Load configuration
        ConfigLoader.load();

        ModItems.init();
        ModSounds.init();
        ModEntities.init();
        ModParticles.init();
        ModBlocks.init();
        ModVillagers.init();
        ModScreenHandlers.register();
        
        ModCommands.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!AssemblyRecipes.isFrozen()) {
                AssemblyRecipes.freeze();
            }
            reinit(server);
        });

        // Clean up pending delayed shots and bullets when a player disconnects to prevent memory leaks
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler != null && handler.player != null) {
                BaseRevolverItem.removePendingShot(handler.player.getUuid());
                BaseRevolverItem.removePendingBullet(handler.player.getUuid());
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resourceManager, success) -> {
                    if (success) {
                        // Also reload config on datapack reload if requested?
                        // For now we keep it explicit via command or keep this for convenience.
                        ConfigLoader.load();
                        reinit(server);
                    }
                }
        );

        // Sync config on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.player, new SyncConfigS2CPacket(ConfigLoader.toJson())));
    }
}
