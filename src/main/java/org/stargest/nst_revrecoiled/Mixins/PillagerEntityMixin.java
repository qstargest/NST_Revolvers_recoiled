package org.stargest.nst_revrecoiled.Mixins;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModEntities;

/**
 * Mixin for PillagerEntity to handle the natural spawning of RevolverBanditEntity.
 * This mixin intercepts the initialization of Pillagers to occasionally replace them with
 * Revolver Bandits when they spawn as part of patrols or in outposts, based on configuration settings.
 */
@Mixin(PillagerEntity.class)
public class PillagerEntityMixin {

    /**
     * Injected into the start of the initialize method of PillagerEntity.
     * Checks if the spawning Pillager should be replaced by a RevolverBanditEntity
     * based on the spawn reason and configured probabilities.
     *
     * @param world       The world access where the entity is being initialized.
     * @param difficulty  The local difficulty of the area.
     * @param spawnReason The reason why the entity is being spawned (e.g., STRUCTURE, PATROL).
     * @param entityData  The initial entity data.
     * @param cir         Callback info to allow cancelling and returning a different EntityData.
     */
    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true)
    private void onInitialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        // Determine if the spawn is occurring in a structure (Outpost) or as part of a Patrol.
        boolean isOutpost = spawnReason == SpawnReason.STRUCTURE || spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION;
        boolean isPatrol = spawnReason == SpawnReason.PATROL;

        if (isOutpost || isPatrol) {
            // Check if bandit spawning is enabled for the specific spawn context (Outpost or Patrol).
            boolean enabled = isOutpost ? ModConfig.get().bandit.spawn.spawnInOutposts : ModConfig.get().bandit.spawn.spawnInPatrols;
            if (!enabled) return;

            float chance;
            if (isOutpost) {
                // Outpost chance calculation based on a weight system.
                // We use a 1-to-10 scale where 10 is roughly 50/50.
                // This formula handles weight 0 correctly and prevents division by zero for negative values.
                int weight = Math.max(0, ModConfig.get().bandit.spawn.outpostSpawnWeight);
                chance = (float) weight / (10.0f + weight);
            } else {
                // Patrol chance is a direct percentage value (0.0 to 1.0).
                chance = Math.clamp(ModConfig.get().bandit.spawn.patrolSpawnChance, 0.0f, 1.0f);
            }

            // Perform the replacement if the random roll succeeds.
            if (world.getRandom().nextFloat() < chance) {
                PillagerEntity pillager = (PillagerEntity) (Object) this;
                RevolverBanditEntity bandit = ModEntities.REVOLVER_BANDIT.create(world.toServerWorld(), spawnReason);
                
                if (bandit != null) {
                    // Position the Bandit exactly where the Pillager was supposed to spawn.
                    bandit.refreshPositionAndAngles(pillager.getX(), pillager.getY(), pillager.getZ(), pillager.getYaw(), pillager.getPitch());
                    
                    // Initialize the Bandit and spawn it into the world.
                    EntityData newEntityData = bandit.initialize(world, difficulty, spawnReason, entityData);
                    world.spawnEntity(bandit);
                    
                    // Discard the original Pillager entity and return the Bandit's initialization data.
                    pillager.discard();
                    cir.setReturnValue(newEntityData);
                }
            }
        }
    }
}
