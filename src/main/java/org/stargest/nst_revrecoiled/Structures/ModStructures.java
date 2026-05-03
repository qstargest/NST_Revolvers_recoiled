package org.stargest.nst_revrecoiled.Structures;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.Registry;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Injects the revolvermaker house into vanilla village house pools
 * so it spawns alongside regular village houses with road connections.
 *
 * Called once from Main on SERVER_STARTING, after registries are ready.
 * Road pathing is handled entirely by vanilla — no extra code needed.
 */
public class ModStructures {

    private static final RegistryKey<StructurePool> TAIGA_VILLAGE_HOUSES =
            RegistryKey.of(RegistryKeys.TEMPLATE_POOL,
                    new Identifier("village/taiga/houses"));

    private static final Identifier REVOLVERMAKER_HOUSE_ELEM =
            Identifier.of(Main.MOD_ID, "revolvermaker_house");

    /**
     * Clears and re-injects the structures based on the current configuration.
     * Safe to call multiple times (e.g. on /nst_rev reload).
     */
    public static void reinit(MinecraftServer server) {
        clearInjectedPools(server);
        init(server);
    }

    /**
     * Injects the revolvermaker house into the configured village biome house pools.
     * Uses the weight specified in ModConfig.
     */
    private static void init(MinecraftServer server) {
        if (!ModConfig.get().worldGen.spawnRevolvermakerHouse) {
            Main.LOGGER.info("Revolvermaker house generation disabled via config.");
            return;
        }

        int weight = ModConfig.get().worldGen.houseWeight;

        Registry<StructurePool> poolRegistry = server.getRegistryManager()
                .getOptional(RegistryKeys.TEMPLATE_POOL)
                .orElse(null);
        if (poolRegistry == null) return;

        StructurePool pool = poolRegistry.get(TAIGA_VILLAGE_HOUSES);
        if (pool == null) {
            Main.LOGGER.warn("Template pool {} not found! Skipping injection.",
                    TAIGA_VILLAGE_HOUSES.getValue());
            return;
        }


        StructurePoolElement element = StructurePoolElement
                .ofSingle(Objects.requireNonNull(REVOLVERMAKER_HOUSE_ELEM).toString())
                .apply(StructurePool.Projection.RIGID);

        StructurePoolAccessor accessor = (StructurePoolAccessor) pool;

        List<Pair<StructurePoolElement, Integer>> newWeights =
                new ArrayList<>(accessor.getElementWeights());
        newWeights.add(Pair.of(element, weight));
        accessor.setElementWeights(newWeights);

        ObjectArrayList<StructurePoolElement> newElements =
                new ObjectArrayList<>(accessor.getElements());
        for (int i = 0; i < weight; i++) {
            newElements.add(element);
        }
        accessor.setElements(newElements);

        Main.LOGGER.info("Injected {} into {} with weight {}",
                REVOLVERMAKER_HOUSE_ELEM, TAIGA_VILLAGE_HOUSES.getValue(), weight);
    }

    /**
     * Reverts structural injection by filtering out any injected revolvermaker house
     * entries from both the raw templates list and the unrolled elements list.
     */
    private static void clearInjectedPools(MinecraftServer server) {
        Registry<StructurePool> poolRegistry = server.getRegistryManager()
                .getOptional(RegistryKeys.TEMPLATE_POOL)
                .orElse(null);
        if (poolRegistry == null) return;

        StructurePool pool = poolRegistry.get(TAIGA_VILLAGE_HOUSES);
        if (pool == null) return;

        StructurePoolAccessor accessor = (StructurePoolAccessor) pool;
        String targetId = Objects.requireNonNull(REVOLVERMAKER_HOUSE_ELEM).toString();

        List<Pair<StructurePoolElement, Integer>> filteredWeights = accessor.getElementWeights()
                .stream()
                .filter(pair -> !pair.getFirst().toString().contains(targetId))
                .collect(Collectors.toCollection(ArrayList::new));
        accessor.setElementWeights(filteredWeights);

        ObjectArrayList<StructurePoolElement> filteredElements = accessor.getElements()
                .stream()
                .filter(e -> !e.toString().contains(targetId))
                .collect(Collectors.toCollection(ObjectArrayList::new));
        accessor.setElements(filteredElements);

        Main.LOGGER.info("Cleared {} from {}", targetId, TAIGA_VILLAGE_HOUSES.getValue());
    }
}
