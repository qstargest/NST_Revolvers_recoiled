package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

/**
 * S2C packet to synchronize the complete ModConfig from server to client.
 * Sent on player join and whenever the config is reloaded on the server.
 */
public record SyncConfigS2CPacket(String configJson) implements CustomPayload {

    public static final Id<SyncConfigS2CPacket> ID =
            new Id<>(Identifier.of(Main.MOD_ID, "sync_config"));

    public static final PacketCodec<PacketByteBuf, SyncConfigS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncConfigS2CPacket::configJson,
                    SyncConfigS2CPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
