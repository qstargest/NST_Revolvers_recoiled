package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

/**
 * Network packet for synchronizing fire particles across clients.
 * Sent from server to all nearby players when a revolver is fired.
 *
 * Contains only the entity ID of the shooter, allowing clients to:
 * 1. Look up the entity in their world
 * 2. Calculate particle spawn position based on entity's current position/rotation
 * 3. Spawn fire particles at the correct location
 *
 * This ensures all players see muzzle flash particles, not just the shooter.
 * The local player is handled separately via clientFireCallback to avoid duplication.
 */
public record RevolverFireParticlePacket(int entityId) implements CustomPayload {

    /**
     * Unique packet identifier for Fabric networking.
     */
    public static final Id<RevolverFireParticlePacket> ID =
            new Id<>(Identifier.of(Main.MOD_ID, "revolver_fire_particle"));

    /**
     * Packet codec for serialization/deserialization.
     * Uses integer codec for entity ID.
     */
    public static final PacketCodec<PacketByteBuf, RevolverFireParticlePacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, RevolverFireParticlePacket::entityId,
                    RevolverFireParticlePacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
