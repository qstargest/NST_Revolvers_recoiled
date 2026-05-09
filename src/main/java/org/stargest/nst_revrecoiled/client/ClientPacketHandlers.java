package org.stargest.nst_revrecoiled.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.stargest.nst_revrecoiled.util.ModConfig;

/**
 * Handles the logic for server-to-client packets.
 *
 * This class is invoked by the packet handlers in the network package
 * to execute visual or state changes on the client main thread.
 * It manages muzzle flash effects, reload smoke, and configuration
 * synchronization from the server.
 *
 * Muzzle flash particles are skipped for the local player to avoid
 * duplicate effects, as they are triggered locally for instant feedback.
 *
 * Adapted for Forge 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    /**
     * Synchronizes the server's configuration to the client.
     */
    public static void handleConfigSync(String configJson) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        ModConfig config = gson.fromJson(configJson, ModConfig.class);
        if (config != null) {
            ModConfig.set(config);
        }
    }
}
