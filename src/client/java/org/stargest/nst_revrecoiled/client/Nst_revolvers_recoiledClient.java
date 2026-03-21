package org.stargest.nst_revrecoiled.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.gui.AssemblyTableScreen;
import org.stargest.nst_revrecoiled.client.handlers.RevolverParticleHandler;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.particles.RevolverParticle;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.client.util.ModEntityRenderers;
import org.stargest.nst_revrecoiled.client.util.ModItemRenderers;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.util.ModParticles;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;

import java.util.Objects;

/**
 * Client-side initialization for the mod.
 * Orchestrates registration of renderers, particles, screens, event listeners,
 * and network handlers.
 *
 * Initialization order:
 * 1. Entity renderers (bullet projectiles, custom villager renderer)
 * 2. Item renderers (GeckoLib revolver models)
 * 3. Screen handlers (Assembly Table GUI binding)
 * 4. Event listeners (disconnect handler for recoil reset and arm pose cleanup,
 *    entity unload handler for per-entity arm pose cleanup)
 * 5. Particle factories (fire and reload effects)
 * 6. Immediate fire callback (camera recoil + muzzle-flash particles, bypasses animation delay)
 * 7. Network packet receiver for reload particles (server-timed, sent at animation keyframe tick)
 * 8. Network packet receiver for fire particles (synchronization to nearby players)
 *
 * Both fire and reload particles bypass GeckoLib animation keyframe callbacks entirely.
 * Fire particles are triggered client-side at the exact moment of firing via clientFireCallback.
 * Reload particles are triggered by a server packet sent at tick 20 of the charge,
 * matching the bullet-insertion keyframe without depending on GeckoLib keyframe events.
 *
 * Network synchronization ensures all nearby players see fire and reload particles.
 * For fire particles, the local player is skipped on the packet receiver side since
 * it is already handled by clientFireCallback to avoid duplication.
 * For reload particles, the server sends to all players including the shooter.
 *
 * PlayerArmPose state is cleaned up on both disconnect (clearAllStates) and
 * individual entity unload (clearState) to prevent memory leaks.
 */
public class Nst_revolvers_recoiledClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity renderers (bullet projectiles, revolvermaker villager)
        // and item renderers (GeckoLib revolver models)
        ModEntityRenderers.init();
        ModItemRenderers.init();

        // Bind the Assembly Table screen handler to its client-side GUI class
        HandledScreens.register(
                ModScreenHandlers.ASSEMBLY_TABLE_HANDLER,
                AssemblyTableScreen::new);

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
        registry.register(ModParticles.REVOLVER_FIRE, RevolverParticle.FireFactory::new);
        registry.register(ModParticles.REVOLVER_RELOAD, RevolverParticle.ReloadFactory::new);

        // Composite fire callback: triggers camera recoil and spawns muzzle-flash particles
        // in a single call at the exact moment of firing, bypassing GeckoLib animation delay.
        // Camera recoil must come first so the offset is applied before the next frame renders.
        BaseRevolverItem.setClientFireCallback(user -> {
            CameraRecoilManager.getInstance().applyRecoil();
            RevolverParticleHandler.spawnFireImmediate(user);
        });

        // Receive server-timed reload particle packets.
        // Sent to all players including the shooter at tick 20 of the charge,
        // matching the bullet-insertion moment in the reload animation.
        ClientPlayNetworking.registerGlobalReceiver(
                RevolverReloadParticlePacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    Entity entity = Objects.requireNonNull(ctx.client().world)
                            .getEntityById(payload.entityId());

                    if (entity instanceof LivingEntity living) {
                        RevolverParticleHandler.spawnReloadImmediate(living);
                    }
                })
        );

        // Receive server-broadcast fire particle packets for other players' shots.
        // Local player is skipped — particles already spawned via clientFireCallback above.
        ClientPlayNetworking.registerGlobalReceiver(
                RevolverFireParticlePacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    Entity entity = Objects.requireNonNull(ctx.client().world)
                            .getEntityById(payload.entityId());

                    if (entity instanceof LivingEntity living) {
                        // Skip local player — particles already spawned via clientFireCallback
                        if (entity == ctx.client().player) return;

                        // Spawn fire particles for remote players
                        RevolverParticleHandler.spawnFireImmediate(living);
                    }
                })
        );
    }
}
