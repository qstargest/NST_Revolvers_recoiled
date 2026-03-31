package org.stargest.nst_revrecoiled.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;

import java.util.List;
import java.util.Objects;

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

    // Revolver payload
    public record Payload(Identifier recipeId) implements FabricPacket {

        public static final PacketType<Payload> TYPE =
                PacketType.create(
                        new Identifier(Main.MOD_ID, "assemble_craft"),
                        buf -> new Payload(buf.readIdentifier())
                );

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeIdentifier(recipeId);
        }

        @Override
        public PacketType<?> getType() { return TYPE; }
    }

    // Bullet payload
    public record BulletPayload(int recipeIndex, int quantity) implements FabricPacket {

        public static final PacketType<BulletPayload> TYPE =
                PacketType.create(
                        new Identifier(Main.MOD_ID, "assemble_craft_bullets"),
                        buf -> new BulletPayload(buf.readInt(), buf.readInt())
                );

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeInt(recipeIndex);
            buf.writeInt(quantity);
        }

        @Override
        public PacketType<?> getType() { return TYPE; }
    }

    public static void register() {
        // Revolver
        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE,
                (payload, player, responseSender) ->
                        Objects.requireNonNull(player.getServer()).execute(() ->
                                AssemblyRecipes.getById(payload.recipeId())
                                        .ifPresent(r -> {
                                            if (r.craft(player.getInventory())) {
                                                player.getWorld().playSound(null,
                                                        player.getBlockPos(),
                                                        SoundEvents.BLOCK_ANVIL_USE,
                                                        SoundCategory.BLOCKS, 1.0f, 0.8f);
                                            }
                                        })));

        // Bullets
        ServerPlayNetworking.registerGlobalReceiver(BulletPayload.TYPE,
                (payload, player, responseSender) ->
                        Objects.requireNonNull(player.getServer()).execute(() -> {
                            List<AssemblyRecipe> recipes = AssemblyRecipes.getAll();
                            int idx = payload.recipeIndex();
                            if (idx < 0 || idx >= recipes.size()) return;
                            if (!(recipes.get(idx) instanceof BulletAssemblyRecipe bullet)) return;

                            int qty = Math.max(1, payload.quantity());
                            if (bullet.craftBullets(player.getInventory(), qty)) {
                                player.getWorld().playSound(null,
                                        player.getBlockPos(),
                                        SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH,
                                        SoundCategory.BLOCKS, 1.0f, 1.0f);
                            }
                        }));
    }
}
