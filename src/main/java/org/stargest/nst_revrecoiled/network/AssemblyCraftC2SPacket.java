package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    // Revolver packet (C2S)
    // -------------------------------------------------------------------------
    public record Payload(String recipeId) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation(NstRevRecoiled.MOD_ID, "assembly_craft");

        public Payload(FriendlyByteBuf buf) {
            this(buf.readUtf());
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.recipeId);
        }

        @Override
        public @NotNull ResourceLocation id() {
            return ID;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            context.workHandler().submitAsync(() -> {
                // Извлекаем игрока из опционала (на сервере он всегда есть в контексте)
                context.player().ifPresent(player -> {
                    if (!(player instanceof ServerPlayer serverPlayer)) return;

                    ResourceLocation recipeRes = new ResourceLocation(payload.recipeId());
                    Optional<AssemblyRecipe> recipeOpt = AssemblyRecipes.getById(recipeRes);

                    recipeOpt.ifPresent(recipe -> {
                        if (recipe.canCraft(serverPlayer.getInventory())) {
                            recipe.craft(serverPlayer.getInventory());

                            serverPlayer.level().playSound(
                                    null,
                                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                    SoundEvents.ANVIL_USE,
                                    SoundSource.BLOCKS,
                                    1.0f, 0.8f
                            );
                        }
                    });
                });
            });
        }
    }

    // -------------------------------------------------------------------------
    // Bullet packet (C2S)
    // -------------------------------------------------------------------------
    public record BulletPayload(String recipeId, int quantity) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation(NstRevRecoiled.MOD_ID, "assembly_craft_bullet");

        public BulletPayload(FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readVarInt());
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.recipeId);
            buf.writeVarInt(this.quantity);
        }

        @Override
        public @NotNull ResourceLocation id() {
            return ID;
        }

        public static void handle(BulletPayload payload, IPayloadContext context) {
            context.workHandler().submitAsync(() -> context.player().ifPresent(player -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;

                ResourceLocation recipeRes = new ResourceLocation(payload.recipeId());
                Optional<AssemblyRecipe> recipeOpt = AssemblyRecipes.getById(recipeRes);

                recipeOpt.ifPresent(recipe -> {
                    if (!(recipe instanceof BulletAssemblyRecipe br)) return;

                    int qty = Math.max(1, payload.quantity());
                    if (br.canCraftBullets(serverPlayer.getInventory(), qty)) {
                        br.craftBullets(serverPlayer.getInventory(), qty);

                        serverPlayer.level().playSound(
                                null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.VILLAGER_WORK_TOOLSMITH,
                                SoundSource.BLOCKS,
                                1.0f, 1.0f
                        );
                    }
                });
            }));
        }
    }
}
