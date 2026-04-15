package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.stargest.nst_revrecoiled.client.ClientPacketHandlers;

import java.util.function.Supplier;

/**
 * S2C packet that triggers muzzle flash and recoil particles for a specific shooter.
 * Sent by the server to all nearby players when a revolver is fired.
 *
 * Adapted for Forge 1.20.1.
 */
public class RevolverFireParticlePacket {
    private final int entityId;

    public RevolverFireParticlePacket(int entityId) {
        this.entityId = entityId;
    }

    public RevolverFireParticlePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandlers.handleFire(this.entityId));
        return true;
    }
}
