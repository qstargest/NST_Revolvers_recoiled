package org.stargest.nst_revrecoiled.Structures;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Mixins.StructurePoolAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects the revolvermaker house into vanilla village house pools
 * so it spawns alongside regular village houses with road connections.
 *
 * Called once from Main on SERVER_STARTING, after registries are ready.
 * Road pathing is handled entirely by vanilla — no extra code needed.
 */
public class ModStructures {

    /** Village house pools to inject into. Add/remove biomes as needed. */
    private static final String[] VILLAGE_POOLS = {
            "minecraft:village/taiga/houses"
    };

    /**
     * Injects the revolvermaker house into the configured village biome house pools.
     * Call exactly once; subsequent calls will add duplicate entries.
     */
    public static void init(MinecraftServer server) {
        ModConfig config = org.stargest.nst_revrecoiled.util.ModConfig.get();
        if (!config.worldGen.spawnHouse) {
            Main.LOGGER.info("Village house spawn is disabled in config. Skipping injection.");
            return;
        }

        int weight = config.worldGen.houseWeight;

        var poolRegistry = server.getRegistryManager().getOrThrow(RegistryKeys.TEMPLATE_POOL);

        StructurePoolElement element = StructurePoolElement
                .ofSingle(Main.MOD_ID + ":revolvermaker_house")
                .apply(StructurePool.Projection.RIGID);

        for (String poolId : VILLAGE_POOLS) {
            try {
                RegistryKey<StructurePool> key =
                        RegistryKey.of(RegistryKeys.TEMPLATE_POOL, Identifier.of(poolId));

                StructurePool pool = poolRegistry.get(key);
                if (pool == null) {
                    Main.LOGGER.warn("Template pool {} not found! Skipping injection.", poolId);
                    continue;
                }

                StructurePoolAccessor accessor = (StructurePoolAccessor) pool;

                List<Pair<StructurePoolElement, Integer>> newWeights =
                        new ArrayList<>(accessor.getElementWeights());
                newWeights.add(Pair.of(element, weight));
                accessor.setElementWeights(newWeights);

                for (int i = 0; i < weight; i++) {
                    accessor.getElements().add(element);
                }

                Main.LOGGER.info("Injected revolvermaker_house into {} with weight {}", poolId, weight);

            } catch (Exception e) {
                Main.LOGGER.error("Failed to inject into pool {}: {}", poolId, e.getMessage(), e);
            }
        }
    }

    public static void clearInjectedPools(MinecraftServer server) {
        var poolRegistry = server.getRegistryManager().getOrThrow(RegistryKeys.TEMPLATE_POOL);
        String targetId = Main.MOD_ID + ":revolvermaker_house";

        for (String poolId : VILLAGE_POOLS) {
            try {
                RegistryKey<StructurePool> key =
                        RegistryKey.of(RegistryKeys.TEMPLATE_POOL, Identifier.of(poolId));

                StructurePool pool = poolRegistry.get(key);
                if (pool == null) continue;

                StructurePoolAccessor accessor = (StructurePoolAccessor) pool;

                // Remove from elementWeights list
                accessor.getElementWeights().removeIf(pair -> {
                    StructurePoolElement element = pair.getFirst();
                    return element.toString().contains(targetId);
                });

                // Remove from flattend elements list
                accessor.getElements().removeIf(element -> element.toString().contains(targetId));

                Main.LOGGER.info("Cleared revolvermaker_house from {}", poolId);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to clear pool {}: {}", poolId, e.getMessage());
            }
        }
    }

    public static void reinit(MinecraftServer server) {
        clearInjectedPools(server);
        init(server);
    }
}
