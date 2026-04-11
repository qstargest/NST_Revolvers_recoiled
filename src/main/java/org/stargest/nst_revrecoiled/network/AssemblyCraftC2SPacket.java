package org.stargest.nst_revrecoiled.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;

import java.util.Optional;

/**
 * Network packet sent from the client to the server to request a craft action
 * at the Assembly Table.
 *
 * Contains two payload types:
 * - Payload       — crafts a single revolver, identified by string ResourceLocation
 * - BulletPayload — crafts a configurable number of bullets, identified by string ResourceLocation
 *
 * Both payloads identify recipes by their string resource location (not list index)
 * so that recipe order changes in AssemblyRecipes do not silently break in-flight
 * or replayed packets.
 *
 * Both payload types are registered in the ModNetworking registry. The server validates 
 * the recipe before executing the craft; inventory changes are synced back through 
 * the normal slot-sync mechanism.
 */
public class AssemblyCraftC2SPacket {

    // -------------------------------------------------------------------------
    // Revolver packet
    // -------------------------------------------------------------------------

    /**
     * Payload for crafting a single revolver.
     * Uses the recipe's string ResourceLocation so the server can look up the recipe
     * via AssemblyRecipes.getById(), independent of declaration order.
     *
     * @param recipeId string form of the recipe's ResourceLocation
     */
    public record Payload(String recipeId) implements CustomPacketPayload {

        public static final Type<Payload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "assembly_craft"));

        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, Payload::recipeId,
                        Payload::new
                );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // -------------------------------------------------------------------------
    // Bullet packet
    // -------------------------------------------------------------------------

    /**
     * Payload for crafting bullets in a given quantity.
     * Uses the recipe's string ResourceLocation to look up the recipe.
     *
     * @param recipeId string form of the recipe's ResourceLocation
     * @param quantity number of bullets to craft (clamped to ≥1 server-side)
     */
    public record BulletPayload(String recipeId, int quantity) implements CustomPacketPayload {

        public static final Type<BulletPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "assembly_craft_bullet"));

        public static final StreamCodec<ByteBuf, BulletPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, BulletPayload::recipeId,
                        ByteBufCodecs.VAR_INT,     BulletPayload::quantity,
                        BulletPayload::new
                );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // -------------------------------------------------------------------------
    // Server-side handlers
    // -------------------------------------------------------------------------

    /**
     * Handles revolver craft requests on the server.
     * Resolves the recipe by ResourceLocation via AssemblyRecipes.getById(),
     * silently ignoring unknown IDs to handle version mismatches gracefully.
     * Validates ingredients, and executes craft.
     */
    public static void handleRevolver(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer)) return;

            ResourceLocation id = ResourceLocation.parse(payload.recipeId());
            Optional<AssemblyRecipe> recipeOpt = AssemblyRecipes.getById(id);
            recipeOpt.ifPresent(recipe -> {
                if (recipe.canCraft(player.getInventory())) {
                    recipe.craft(player.getInventory());

                    player.level().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_USE,
                            SoundSource.BLOCKS,
                            1.0f, 0.8f
                    );
                }
            });
        });
    }

    /**
     * Handles bullet craft requests on the server.
     * Resolves the recipe by ResourceLocation, validates ingredients, and crafts
     * the requested quantity. Clamps quantity to a minimum of 1 to guard against 
     * malformed or replayed packets.
     */
    public static void handleBullet(BulletPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer)) return;

            ResourceLocation id = ResourceLocation.parse(payload.recipeId());
            Optional<AssemblyRecipe> recipeOpt = AssemblyRecipes.getById(id);
            recipeOpt.ifPresent(recipe -> {
                if (!(recipe instanceof BulletAssemblyRecipe br)) return;
                int qty = Math.max(1, payload.quantity());
                if (br.canCraftBullets(player.getInventory(), qty)) {
                    br.craftBullets(player.getInventory(), qty);

                    player.level().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_WORK_TOOLSMITH,
                            SoundSource.BLOCKS,
                            1.0f, 1.0f
                    );
                }
            });
        });
    }
}
