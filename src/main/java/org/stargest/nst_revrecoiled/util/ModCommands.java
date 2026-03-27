package org.stargest.nst_revrecoiled.util;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.stargest.nst_revrecoiled.Structures.ModStructures;

/**
 * Registers commands for the mod.
 * Includes the /nst_rev reload command for updating configuration in-game.
 */
public class ModCommands {

    /**
     * Registers the mod's commands.
     * Called from Main.onInitialize.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("nst_rev")
                .requires(source -> source.hasPermissionLevel(3)) // OP level 3
                .then(CommandManager.literal("reload")
                        .executes(ModCommands::reloadConfig)
                )
        ));
    }

    /**
     * Logic for the reload command.
     * Reloads the config file and re-initializes world generation structures.
     */
    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Reload the config file from disk
            ConfigLoader.load();
            
            // Re-initialize village structures with new weights/toggle
            ModStructures.reinit(source.getServer());
            
            // Broadcast new config to all connected clients
            String json = ConfigLoader.toJson();
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket(json));
            });
            
            source.sendFeedback(() -> Text.literal("§a[NST Revolvers] Configuration reloaded and synced successfully!"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§c[NST Revolvers] Failed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }
}
