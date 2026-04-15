package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.stargest.nst_revrecoiled.client.ClientPacketHandlers;

import java.util.function.Supplier;

/**
 * S2C packet that triggers the reload (smoke) particle effect for a specific shooter.
 * Sent by the server to all nearby players at the exact tick a bullet is inserted.
 *
 * Adapted for Forge 1.20.1.
 */
public class RevolverReloadParticlePacket {
    private final int entityId;

    public RevolverReloadParticlePacket(int entityId) {
        this.entityId = entityId;
    }

    public RevolverReloadParticlePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandlers.handleReload(this.entityId));
        return true;
    }
}
