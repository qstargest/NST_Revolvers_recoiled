package org.stargest.nst_revrecoiled.mixins;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.ServerLevelAccessor;
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
@Mixin(Pillager.class)
public class PillagerEntityMixin {

    /**
     * Injected into the start of the finalizeSpawn method of PillagerEntity.
     * Checks if the spawning Pillager should be replaced by a RevolverBanditEntity
     * based on the spawn reason and configured probabilities.
     */
    @Inject(method = "finalizeSpawn", at = @At("HEAD"), cancellable = true)
    private void onFinalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        // Determine if the spawn is occurring in a structure (Outpost) or as part of a Patrol.
        boolean isOutpost = spawnReason == EntitySpawnReason.STRUCTURE || spawnReason == EntitySpawnReason.NATURAL || spawnReason == EntitySpawnReason.CHUNK_GENERATION;
        boolean isPatrol = spawnReason == EntitySpawnReason.PATROL;

        if (isOutpost || isPatrol) {
            // Check if bandit spawning is enabled for the specific spawn context (Outpost or Patrol).
            boolean enabled = isOutpost ? ModConfig.get().bandit.spawn.spawnInOutposts : ModConfig.get().bandit.spawn.spawnInPatrols;
            if (!enabled) return;

            float chance;
            if (isOutpost) {
                // Outpost chance calculation based on a weight system.
                int weight = Math.max(0, ModConfig.get().bandit.spawn.outpostSpawnWeight);
                chance = (float) weight / (10.0f + weight);
            } else {
                // Patrol chance is a direct percentage value (0.0 to 1.0).
                chance = Math.clamp(ModConfig.get().bandit.spawn.patrolSpawnChance, 0.0f, 1.0f);
            }

            // Perform the replacement if the random roll succeeds.
            if (level.getRandom().nextFloat() < chance) {
                Pillager pillager = (Pillager) (Object) this;
                RevolverBanditEntity bandit = ModEntities.REVOLVER_BANDIT.get().create(level.getLevel(), spawnReason);

                if (bandit != null) {
                    // Position the Bandit exactly where the Pillager was supposed to spawn.
                    bandit.moveTo(pillager.getX(), pillager.getY(), pillager.getZ(), pillager.getYRot(), pillager.getXRot());

                    // Initialize the Bandit and spawn it into the world.
                    SpawnGroupData newSpawnGroupData = bandit.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
                    level.addFreshEntity(bandit);

                    // Discard the original Pillager entity and return the Bandit's initialization data.
                    pillager.discard();
                    cir.setReturnValue(newSpawnGroupData);
                }
            }
        }
    }
}
