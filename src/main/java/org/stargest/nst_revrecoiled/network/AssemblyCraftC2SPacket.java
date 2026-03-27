package org.stargest.nst_revrecoiled.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
 * - Payload      — crafts a single revolver, identified by namespaced Identifier
 * - BulletPayload — crafts a configurable number of bullets, identified by list index
 *
 * The revolver payload uses an Identifier rather than a list index so that recipe
 * order changes in AssemblyRecipes do not silently break in-flight or replayed packets.
 * The bullet payload retains the list index for now since bullet recipes are
 * resolved server-side and quantity validation provides sufficient safety.
 *
 * Both payload types are registered via register(), which must be called once
 * from the main ModInitializer. The server validates the recipe before executing
 * the craft; inventory changes are synced back through the normal slot-sync mechanism.
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
     * Uses the recipe's namespaced Identifier so the server can look up the recipe
     * via AssemblyRecipes.getById(), independent of declaration order.
     *
     * @param recipeId namespaced identifier of the recipe to craft
     */
    public record Payload(Identifier recipeId) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.tuple(Identifier.PACKET_CODEC, Payload::recipeId, Payload::new);

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
     * The revolver handler resolves the recipe by Identifier via AssemblyRecipes.getById(),
     * silently ignoring unknown IDs to handle version mismatches gracefully.
     * The bullet handler validates the list index bounds and clamps quantity to a
     * minimum of 1 to guard against malformed or replayed packets.
     */
    public static void register() {
        // Revolver packet — resolves recipe by Identifier, independent of list order
        PayloadTypeRegistry.playC2S().register(ID, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) ->
                context.server().execute(() -> AssemblyRecipes.getById(payload.recipeId())
                        .ifPresent(r -> {
                            if (r.craft(context.player().getInventory())) {
                                context.player().getWorld().playSound(null, context.player().getBlockPos(),
                                        SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
                            }
                        })));

        // Bullet packet — resolves recipe by list index and crafts the requested quantity
        PayloadTypeRegistry.playC2S().register(BULLET_ID, BulletPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BULLET_ID, (payload, context) ->
                context.server().execute(() -> {
                    List<AssemblyRecipe> recipes = AssemblyRecipes.getAll();
                    int idx = payload.recipeIndex();
                    if (idx < 0 || idx >= recipes.size()) return;
                    if (!(recipes.get(idx) instanceof BulletAssemblyRecipe bullet)) return;

                    int qty = Math.max(1, payload.quantity());
                    if (bullet.craftBullets(context.player().getInventory(), qty)) {
                        context.player().getWorld().playSound(null, context.player().getBlockPos(),
                                SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    }
                }));
    }
}
