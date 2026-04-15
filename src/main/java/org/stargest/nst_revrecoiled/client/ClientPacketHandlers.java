package org.stargest.nst_revrecoiled.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.stargest.nst_revrecoiled.client.handlers.RevolverParticleHandler;
import org.stargest.nst_revrecoiled.util.ModConfig;

/**
 * Handles the logic for server-to-client packets.
 * Adapted for Forge 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    public static void handleFire(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof LivingEntity shooter && e != mc.player) {
            RevolverParticleHandler.spawnFireImmediate(shooter);
        }
    }

    public static void handleReload(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity e = mc.level.getEntity(entityId);
        if (e instanceof LivingEntity reloader) {
            RevolverParticleHandler.spawnReloadImmediate(reloader);
        }
    }

    public static void handleConfigSync(String configJson) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        ModConfig config = gson.fromJson(configJson, ModConfig.class);
        if (config != null) {
            ModConfig.set(config);
        }
    }
}
