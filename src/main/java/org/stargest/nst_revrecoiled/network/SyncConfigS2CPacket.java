package org.stargest.nst_revrecoiled.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * S2C packet to synchronize the complete ModConfig from server to client.
 * Sent on player join and whenever the config is reloaded on the server.
 */
public record SyncConfigS2CPacket(String configJson) implements CustomPacketPayload {

    public static final Type<SyncConfigS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "sync_config"));

    public static final StreamCodec<ByteBuf, SyncConfigS2CPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncConfigS2CPacket::configJson,
                    SyncConfigS2CPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
