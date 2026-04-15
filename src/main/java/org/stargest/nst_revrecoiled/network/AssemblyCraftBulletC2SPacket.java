package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Notifies the server that a player wants to craft bullets at an Assembly Table.
 * Includes the recipe ID and the requested batch count (max 64).
 *
 * Adapted for Forge 1.20.1.
 */
public class AssemblyCraftBulletC2SPacket {
    private final String recipeId;
    private final int quantity;

    public AssemblyCraftBulletC2SPacket(String recipeId, int quantity) {
        this.recipeId = recipeId;
        this.quantity = quantity;
    }

    public AssemblyCraftBulletC2SPacket(FriendlyByteBuf buf) {
        this.recipeId = buf.readUtf();
        this.quantity = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.recipeId);
        buf.writeVarInt(this.quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) return;

            ResourceLocation recipeRes = new ResourceLocation(this.recipeId);
            Optional<AssemblyRecipe> recipeOpt = AssemblyRecipes.getById(recipeRes);

            recipeOpt.ifPresent(recipe -> {
                if (!(recipe instanceof BulletAssemblyRecipe br)) return;

                int qty = Math.max(1, this.quantity);
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
        });
        return true;
    }
}
