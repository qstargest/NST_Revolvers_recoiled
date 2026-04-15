package org.stargest.nst_revrecoiled.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Structures.ModStructures;
import org.stargest.nst_revrecoiled.network.PacketHandler;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;

/**
 * Registers mod commands on the Forge game event bus.
 * Called from RegisterCommandsEvent in Main.GameEvents.
 *
 * Adapted for Forge 1.20.1.
 */
public class ModCommands {

    /**
     * Registers the mod's commands.
     * Called from ModEvents server starting handler.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nst_rev")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("reload")
                        .executes(ModCommands::reloadConfig)
                )
        );
    }

    /**
     * Logic for the reload command.
     * Reloads the config file and re-initializes world generation structures.
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigLoader.load();

            source.getServer().getPlayerList().getPlayers().forEach(player -> {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() instanceof BaseRevolverItem item) {
                        int currentDamage = stack.getDamageValue();
                        int newMax = item.getMaxDamage(stack);
                        if (currentDamage >= newMax) {
                            stack.setDamageValue(newMax - 1);
                        }
                    }
                }
            });

            ModStructures.reinit(source.getServer());

            String json = ConfigLoader.toJson();
            source.getServer().getPlayerList().getPlayers().forEach(player ->
                    PacketHandler.sendToPlayer(new SyncConfigS2CPacket(json), player));

            source.sendSuccess(() -> Component.literal(
                    "§a[NST Revolvers recoiled] Configuration reloaded and synced successfully!"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "§c[NST Revolvers recoiled] Failed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }
}
