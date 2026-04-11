package org.stargest.nst_revrecoiled.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

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
 * The local player is handled separately to avoid duplication.
 */
public record RevolverFireParticlePacket(int entityId) implements CustomPacketPayload {

    /** Unique packet identifier for networking. */
    public static final Type<RevolverFireParticlePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "revolver_fire_particle"));

    /**
     * Packet codec for serialization/deserialization.
     * Uses integer codec for entity ID.
     */
    public static final StreamCodec<ByteBuf, RevolverFireParticlePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, RevolverFireParticlePacket::entityId,
                    RevolverFireParticlePacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
