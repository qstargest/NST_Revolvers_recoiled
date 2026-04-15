package org.stargest.nst_revrecoiled.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.stargest.nst_revrecoiled.client.handlers.RevolverParticleHandler;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
import org.stargest.nst_revrecoiled.util.ModConfig;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    public static void handleFire(RevolverFireParticlePacket payload, IPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ctx.level().ifPresent(level -> {
            Entity e = level.getEntity(payload.entityId());
            if (e instanceof LivingEntity shooter && e != ctx.player().orElse(null)) {
                RevolverParticleHandler.spawnFireImmediate(shooter);
            }
        }));
    }

    public static void handleReload(RevolverReloadParticlePacket payload, IPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ctx.level().ifPresent(level -> {
            Entity e = level.getEntity(payload.entityId());
            if (e instanceof LivingEntity reloader) {
                RevolverParticleHandler.spawnReloadImmediate(reloader);
            }
        }));
    }

    public static void handleConfigSync(SyncConfigS2CPacket payload, IPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            ModConfig config = gson.fromJson(payload.configJson(), ModConfig.class);
            if (config != null) ModConfig.set(config);
        });
    }
}
