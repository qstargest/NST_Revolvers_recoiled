package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * S2C packet to synchronize the complete ModConfig from server to client.
 * Sent on player join and whenever the config is reloaded on the server.
 */
public record SyncConfigS2CPacket(String configJson) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(NstRevRecoiled.MOD_ID, "sync_config");

    public SyncConfigS2CPacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.configJson);
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }
}
