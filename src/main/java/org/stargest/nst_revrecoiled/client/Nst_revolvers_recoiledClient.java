package org.stargest.nst_revrecoiled.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.client.gui.AssemblyTableScreen;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.client.util.ModEntityRenderers;
import org.stargest.nst_revrecoiled.client.util.ModItemRenderers;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;

import java.util.function.Consumer;

/**
 * Client-side initialization for the mod.
 * Orchestrates registration of renderers, screens, event listeners,
 * and network handlers for the client.
 *
 * Initialization order:
 * 1. Entity renderers (bullet projectiles, custom villager renderer)
 * 2. Item renderers (GeckoLib revolver models)
 * 3. Screen handlers (Assembly Table GUI binding)
 * 4. Event listeners (disconnect handler for recoil reset and arm pose cleanup,
 *    entity unload handler for per-entity arm pose cleanup)
 * 5. Per-item recoil callbacks (registered for all base-mod revolvers)
 * 6. Network packet receiver for configuration synchronization
 *
 * Per-item callbacks (DEFAULT_RECOIL) are shared across all base-mod
 * revolvers. Addon mods can register their own callbacks for custom revolver items via
 * BaseRevolverItem.registerRecoilCallback() from their own ClientModInitializer.
 *
 * PlayerArmPose state is cleaned up on both disconnect (clearAllStates) and
 * individual entity unload (clearState) to prevent memory leaks.
 */
public class Nst_revolvers_recoiledClient implements ClientModInitializer {

    /**
     * Default recoil callback shared by all base-mod revolvers.
     * Triggers camera kickback via CameraRecoilManager at the moment of firing.
     * Extracted as a constant to avoid allocating a new lambda per item registration.
     */
    private static final Consumer<LivingEntity> DEFAULT_RECOIL =
            shooter -> CameraRecoilManager.getInstance().applyRecoil();

    /**
     * Called upon client initialization.
     * Orchestrates registration of renderers, particles, screens, event listeners,
     * network handlers, and recoil callbacks.
     */
    @Override
    public void onInitializeClient() {
        // Register entity renderers (bullet projectiles, revolvermaker villager)
        // and item renderers (GeckoLib revolver models)
        ModEntityRenderers.init();
        ModItemRenderers.init();

        // Bind the Assembly Table screen handler to its client-side GUI class
        HandledScreens.register(
                ModScreenHandlers.ASSEMBLY_TABLE_HANDLER,
                AssemblyTableScreen.FACTORY
        );

        // Reset camera recoil when disconnecting from server
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> CameraRecoilManager.getInstance().reset()
        );

        // Clear all cached arm pose states on disconnect to prevent stale data
        // persisting across server sessions
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                PlayerArmPose.clearAllStates()
        );

        // Clear per-entity arm pose state when a player entity unloads
        // (e.g. goes out of render distance) to prevent unbounded map growth
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PlayerEntity) {
                PlayerArmPose.clearState(entity.getId());
            }
        });

        // Register particle factories for revolver muzzle flash and reload smoke
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();

        // Register per-item recoil callbacks for all base-mod revolvers.
        // Addon mods can call registerRecoilCallback() with their own items
        // and a custom consumer from their ClientModInitializer.
        BaseRevolverItem.registerRecoilCallback(ModItems.COBBLESTONE_REVOLVER, DEFAULT_RECOIL);
        BaseRevolverItem.registerRecoilCallback(ModItems.IRON_REVOLVER,        DEFAULT_RECOIL);
        BaseRevolverItem.registerRecoilCallback(ModItems.GOLDEN_REVOLVER,      DEFAULT_RECOIL);
        BaseRevolverItem.registerRecoilCallback(ModItems.DIAMOND_REVOLVER,     DEFAULT_RECOIL);

        // Receive configuration synchronization packet from server.
        // Updates the local ModConfig singleton to match the server's values.
        ClientPlayNetworking.registerGlobalReceiver(
                SyncConfigS2CPacket.TYPE,
                (packet, player, responseSender) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> {
                        ModConfig config = new com.google.gson.Gson()
                                .fromJson(packet.configJson(), ModConfig.class);
                        if (config != null) {
                            ModConfig.set(config);
                            Main.LOGGER.info("Synchronized configuration with server.");
                            if (client.currentScreen instanceof AssemblyTableScreen screen) {
                                screen.refreshSelectedRecipe();
                            }
                        }
                    });
                }
        );
    }
}
