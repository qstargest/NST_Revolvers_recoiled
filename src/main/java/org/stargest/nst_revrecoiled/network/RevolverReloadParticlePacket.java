package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * Network packet for synchronizing reload particles across clients.
 * Sent from the server to all nearby players (including the shooter) at tick 20
 * of the reload animation — the moment the bullet is visually inserted.
 *
 * Contains only the entity ID of the reloader, allowing clients to:
 * 1. Look up the entity in their world
 * 2. Calculate the particle spawn position based on the entity's current position/rotation
 * 3. Spawn reload particles at the correct location via RevolverParticleHandler.spawnReloadImmediate()
 *
 * Unlike RevolverFireParticlePacket, the local player is NOT skipped on the receiving end —
 * reload particles are sent to all players including the shooter, since there is no
 * client-side callback that would otherwise cause duplication for the local player.
 */
public record RevolverReloadParticlePacket(int entityId) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(NstRevRecoiled.MOD_ID, "revolver_reload_particle");

    public RevolverReloadParticlePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }
}
