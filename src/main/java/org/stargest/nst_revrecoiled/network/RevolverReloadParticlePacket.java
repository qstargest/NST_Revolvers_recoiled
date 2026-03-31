package org.stargest.nst_revrecoiled.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
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
public record RevolverReloadParticlePacket(int entityId) implements FabricPacket {

    public static final PacketType<RevolverReloadParticlePacket> TYPE =
            PacketType.create(
                    new Identifier(Main.MOD_ID, "revolver_reload_particle"),
                    buf -> new RevolverReloadParticlePacket(buf.readInt())
            );

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    public PacketType<?> getType() { return TYPE; }
}
