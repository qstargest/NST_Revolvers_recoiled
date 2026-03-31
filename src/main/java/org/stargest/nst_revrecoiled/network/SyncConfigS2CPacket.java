package org.stargest.nst_revrecoiled.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

/**
 * S2C packet to synchronize the complete ModConfig from server to client.
 * Sent on player join and whenever the config is reloaded on the server.
 */
public record SyncConfigS2CPacket(String configJson) implements FabricPacket {

    public static final PacketType<SyncConfigS2CPacket> TYPE =
            PacketType.create(
                    new Identifier(Main.MOD_ID, "sync_config"),
                    buf -> new SyncConfigS2CPacket(buf.readString())
            );

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(configJson);
    }

    @Override
    public PacketType<?> getType() { return TYPE; }
}
