package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

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
public record RevolverReloadParticlePacket(int entityId) implements CustomPayload {

    /** Unique packet identifier for Fabric networking. */
    public static final Id<RevolverReloadParticlePacket> ID =
            new Id<>(Identifier.of(Main.MOD_ID, "revolver_reload_particle"));

    /** Packet codec for serialization/deserialization. Uses integer codec for entity ID. */
    public static final PacketCodec<PacketByteBuf, RevolverReloadParticlePacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, RevolverReloadParticlePacket::entityId,
                    RevolverReloadParticlePacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
