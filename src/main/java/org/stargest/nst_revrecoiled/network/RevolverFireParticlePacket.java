package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
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
    public static final ResourceLocation ID = new ResourceLocation(NstRevRecoiled.MOD_ID, "revolver_fire_particle");

    public RevolverFireParticlePacket(FriendlyByteBuf buf) {
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
