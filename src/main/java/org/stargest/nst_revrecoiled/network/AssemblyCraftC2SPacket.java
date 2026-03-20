package org.stargest.nst_revrecoiled.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;

import java.util.List;

/**
 * Network packet sent from the client to the server to request a craft action
 * at the Assembly Table.
 *
 * Contains two payload types:
 * - Payload      — crafts a single revolver
 * - BulletPayload — crafts a configurable number of bullets
 *
 * Both payload types are registered via register(), which must be called once
 * from the main ModInitializer. The server validates the recipe index and
 * executes the craft; inventory changes are synced back through the normal
 * slot-sync mechanism.
 */
public class AssemblyCraftC2SPacket {

    // -------------------------------------------------------------------------
    // Revolver packet
    // -------------------------------------------------------------------------

    /** Payload ID for the revolver craft packet. */
    public static final CustomPayload.Id<Payload> ID =
            new CustomPayload.Id<>(Identifier.of(Main.MOD_ID, "assemble_craft"));

    /**
     * Payload for crafting a single revolver.
     *
     * @param recipeIndex index of the recipe in AssemblyRecipes.getAll()
     */
    public record Payload(int recipeIndex) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, Payload::recipeIndex, Payload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    // -------------------------------------------------------------------------
    // Bullet packet
    // -------------------------------------------------------------------------

    /** Payload ID for the bullet craft packet. */
    public static final CustomPayload.Id<BulletPayload> BULLET_ID =
            new CustomPayload.Id<>(Identifier.of(Main.MOD_ID, "assemble_craft_bullets"));

    /**
     * Payload for crafting bullets in a given quantity.
     *
     * @param recipeIndex index of the recipe in AssemblyRecipes.getAll()
     * @param quantity    number of bullets to craft
     */
    public record BulletPayload(int recipeIndex, int quantity) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, BulletPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.INTEGER, BulletPayload::recipeIndex,
                        PacketCodecs.INTEGER, BulletPayload::quantity,
                        BulletPayload::new
                );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return BULLET_ID; }
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers both packet types with the Fabric networking API and binds
     * their server-side handlers. Must be called once from the main ModInitializer.
     *
     * The server handler validates the recipe index bounds before executing the craft.
     * For bullet packets, quantity is clamped to a minimum of 1 to guard against
     * malformed or replayed packets.
     */
    public static void register() {
        // Revolver packet — crafts a single revolver from the given recipe index
        PayloadTypeRegistry.playC2S().register(ID, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) ->
                context.server().execute(() -> {
                    List<AssemblyRecipe> recipes = AssemblyRecipes.getAll();
                    int idx = payload.recipeIndex();
                    if (idx < 0 || idx >= recipes.size()) return;
                    recipes.get(idx).craft(context.player().getInventory());
                }));

        // Bullet packet — crafts a batch of bullets from the given recipe index and quantity
        PayloadTypeRegistry.playC2S().register(BULLET_ID, BulletPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BULLET_ID, (payload, context) ->
                context.server().execute(() -> {
                    List<AssemblyRecipe> recipes = AssemblyRecipes.getAll();
                    int idx = payload.recipeIndex();
                    if (idx < 0 || idx >= recipes.size()) return;
                    if (!(recipes.get(idx) instanceof BulletAssemblyRecipe bullet)) return;

                    int qty = Math.max(1, payload.quantity());
                    bullet.craftBullets(context.player().getInventory(), qty);
                }));
    }
}
