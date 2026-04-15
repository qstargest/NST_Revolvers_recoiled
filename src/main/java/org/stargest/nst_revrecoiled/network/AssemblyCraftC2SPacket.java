package org.stargest.nst_revrecoiled.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Notifies the server that a player wants to craft a revolver at an Assembly Table.
 * Includes the unique ResourceLocation of the recipe to craft.
 *
 * Adapted for Forge 1.20.1.
 */
public class AssemblyCraftC2SPacket {
    private final String recipeId;

    public AssemblyCraftC2SPacket(String recipeId) {
        this.recipeId = recipeId;
    }

    public AssemblyCraftC2SPacket(FriendlyByteBuf buf) {
        this.recipeId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.recipeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) return;

            ResourceLocation recipeRes = new ResourceLocation(this.recipeId);
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
        return true;
    }
}
