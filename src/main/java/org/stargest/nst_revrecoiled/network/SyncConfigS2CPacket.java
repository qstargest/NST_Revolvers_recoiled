package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.stargest.nst_revrecoiled.client.ClientPacketHandlers;

import java.util.function.Supplier;

/**
 * S2C packet that synchronizes the server's configuration JSON to the client.
 * Sent to the player upon login or when an admin reloads the config.
 *
 * Adapted for Forge 1.20.1.
 */
public class SyncConfigS2CPacket {
    private final String configJson;

    public SyncConfigS2CPacket(String configJson) {
        this.configJson = configJson;
    }

    public SyncConfigS2CPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.configJson);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            ClientPacketHandlers.handleConfigSync(this.configJson);
        });
        return true;
    }
}
