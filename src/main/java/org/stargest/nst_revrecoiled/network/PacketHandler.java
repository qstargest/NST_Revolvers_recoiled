package org.stargest.nst_revrecoiled.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.stargest.nst_revrecoiled.Main;

/**
 * Handles all network communication for the mod.
 * Uses Forge's SimpleChannel API for 1.20.1.
 *
 * Registration occurs during mod initialization (common setup phase).
 * Packets are routed to their respective handle() methods on either the client or server main threads.
 */
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Main.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    /**
     * Registers all packet classes and their associated codecs/handlers.
     * Called during FMLCommonSetupEvent via Main.commonSetup.
     */
    public static void register() {
        INSTANCE.messageBuilder(SyncConfigS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncConfigS2CPacket::new)
                .encoder(SyncConfigS2CPacket::toBytes)
                .consumerMainThread(SyncConfigS2CPacket::handle)
                .add();

        INSTANCE.messageBuilder(AssemblyCraftC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AssemblyCraftC2SPacket::new)
                .encoder(AssemblyCraftC2SPacket::toBytes)
                .consumerMainThread(AssemblyCraftC2SPacket::handle)
                .add();

        INSTANCE.messageBuilder(AssemblyCraftBulletC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AssemblyCraftBulletC2SPacket::new)
                .encoder(AssemblyCraftBulletC2SPacket::toBytes)
                .consumerMainThread(AssemblyCraftBulletC2SPacket::handle)
                .add();
    }

    /** Sends a packet from client to server. */
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    /** Sends a packet from server to a specific player. */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** Sends a packet from server to all connected players. */
    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToNearby(MSG message, PacketDistributor.TargetPoint point) {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> point), message);
    }
}
