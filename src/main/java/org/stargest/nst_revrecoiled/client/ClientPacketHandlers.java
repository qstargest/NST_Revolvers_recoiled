package org.stargest.nst_revrecoiled.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
import org.stargest.nst_revrecoiled.util.ModConfig;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    public static void handleConfigSync(SyncConfigS2CPacket payload, IPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            ModConfig config = gson.fromJson(payload.configJson(), ModConfig.class);
            if (config != null) ModConfig.set(config);
        });
    }
}
