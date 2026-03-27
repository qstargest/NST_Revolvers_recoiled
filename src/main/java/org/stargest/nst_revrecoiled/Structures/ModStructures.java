package org.stargest.nst_revrecoiled.Structures;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
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

    /** How often the structure spawns relative to other houses. 1 = rare, 5+ = common. */
    private static final int WEIGHT = 5;

    /**
     * Injects the revolvermaker house into the configured village biome house pools.
     * Call exactly once; subsequent calls will add duplicate entries.
     */
    public static void init(MinecraftServer server) {
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
                newWeights.add(Pair.of(element, WEIGHT));
                accessor.setElementWeights(newWeights);

                for (int i = 0; i < WEIGHT; i++) {
                    accessor.getElements().add(element);
                }

                Main.LOGGER.info("Injected revolvermaker_house into {}", poolId);

            } catch (Exception e) {
                Main.LOGGER.error("Failed to inject into pool {}: {}", poolId, e.getMessage(), e);
            }
        }
    }

    public static void clearInjectedPools() {
        // no-op: pool objects are recreated on each reload, no deduplication needed
    }

    public static void reinit(MinecraftServer server) {
        clearInjectedPools();
        init(server);
    }
}
