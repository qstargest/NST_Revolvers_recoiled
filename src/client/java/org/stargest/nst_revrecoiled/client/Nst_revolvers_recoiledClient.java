package org.stargest.nst_revrecoiled.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.handlers.RevolverParticleHandler;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.particles.RevolverParticle;
import org.stargest.nst_revrecoiled.client.util.ModEntityRenderers;
import org.stargest.nst_revrecoiled.client.util.ModItemRenderers;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.util.ModParticles;

import java.util.Objects;

/**
 * Client-side initialization for the mod.
 * Orchestrates registration of renderers, particles, event listeners, and network handlers.
 *
 * Initialization order:
 * 1. Entity renderers (bullet projectiles)
 * 2. Item renderers (GeckoLib revolver models)
 * 3. Event listeners (disconnect handler for recoil reset)
 * 4. Particle factories (fire and reload effects)
 * 5. Animation particle handler (keyframe events for reload)
 * 6. Immediate fire callback (bypasses animation delay for fire particles)
 * 7. Network packet receiver (fire particle synchronization)
 *
 * The immediate fire callback is set separately from the keyframe handler to ensure
 * fire particles appear instantly when shooting, not when the animation keyframe is reached.
 * This provides better visual feedback and perceived responsiveness.
 *
 * Network synchronization ensures all nearby players see fire particles from other players,
 * while avoiding duplication for the local player who already sees particles via clientFireCallback.
 */
public class Nst_revolvers_recoiledClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity and item renderers
        ModEntityRenderers.init();
        ModItemRenderers.init();

        // Reset camera recoil when disconnecting from server
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> CameraRecoilManager.getInstance().reset()
        );

        // Register particle factories for revolver effects
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(ModParticles.REVOLVER_FIRE, RevolverParticle.FireFactory::new);
        registry.register(ModParticles.REVOLVER_RELOAD, RevolverParticle.ReloadFactory::new);

        // Set up particle handler for GeckoLib animation keyframes (reload only)
        BaseRevolverItem.setParticleKeyframeHandler(new RevolverParticleHandler());

        // Set up immediate fire callback to bypass animation delay
        BaseRevolverItem.setClientFireCallback(RevolverParticleHandler::spawnFireImmediate);

        // Register network packet receiver for fire particle synchronization
        ClientPlayNetworking.registerGlobalReceiver(
                RevolverFireParticlePacket.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
                    Entity entity = Objects.requireNonNull(ctx.client().world)
                            .getEntityById(payload.entityId());

                    if (entity instanceof LivingEntity living) {
                        // Skip local player - already handled by clientFireCallback
                        if (entity == ctx.client().player) return;

                        // Spawn fire particles for other players
                        RevolverParticleHandler.spawnFireImmediate(living);
                    }
                })
        );
    }
}
