package org.stargest.nst_revrecoiled.Structures;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.mixins.StructurePoolAccessor;
import org.stargest.nst_revrecoiled.util.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Injects the revolvermaker house into vanilla village house pools
 * so it spawns alongside regular village houses with road connections.
 *
 * Implements dynamic injection logic that allows adding and removing the structure
 * live when the configuration file changes, without requiring a game restart.
 * Depends on StructurePoolAccessor mixins.
 */
public class ModStructures {

    /** Village house pools to inject into. Add/remove biomes as needed. */
    private static final ResourceKey<StructureTemplatePool> TAIGA_VILLAGE_HOUSES =
            ResourceKey.create(Registries.TEMPLATE_POOL,
                    new ResourceLocation("village/taiga/houses"));

    private static final ResourceLocation REVOLVERMAKER_HOUSE_ELEM =
            new ResourceLocation(NstRevRecoiled.MOD_ID, "revolvermaker_house");

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
            NstRevRecoiled.LOGGER.info("Revolvermaker house generation disabled via config.");
            return;
        }

        int weight = ModConfig.get().worldGen.houseWeight;

        Optional<Holder.Reference<StructureTemplatePool>> poolOpt = server.registryAccess()
                .lookupOrThrow(Registries.TEMPLATE_POOL)
                .get(TAIGA_VILLAGE_HOUSES);

        if (poolOpt.isEmpty()) {
            NstRevRecoiled.LOGGER.warn("Template pool {} not found! Skipping injection.",
                    TAIGA_VILLAGE_HOUSES.location());
            return;
        }

        StructureTemplatePool pool = poolOpt.get().value();
        StructurePoolElement element = StructurePoolElement
                .single(REVOLVERMAKER_HOUSE_ELEM.toString())
                .apply(StructureTemplatePool.Projection.RIGID);

        StructurePoolAccessor accessor = (StructurePoolAccessor) pool;

        List<Pair<StructurePoolElement, Integer>> newRaw = new ArrayList<>(accessor.getRawTemplates());
        newRaw.add(Pair.of(element, weight));
        accessor.setRawTemplates(newRaw);

        ObjectArrayList<StructurePoolElement> newElements = new ObjectArrayList<>(accessor.getElements());
        for (int i = 0; i < weight; i++) {
            newElements.add(element);
        }
        accessor.setElements(newElements);

        NstRevRecoiled.LOGGER.info("Injected {} into {} with weight {}",
                REVOLVERMAKER_HOUSE_ELEM, TAIGA_VILLAGE_HOUSES.location(), weight);
    }

    /**
     * Reverts structural injection by filtering out any injected revolvermaker house
     * entries from both the raw templates list and the unrolled elements list.
     */
    private static void clearInjectedPools(MinecraftServer server) {
        Optional<Holder.Reference<StructureTemplatePool>> poolOpt = server.registryAccess()
                .lookupOrThrow(Registries.TEMPLATE_POOL)
                .get(TAIGA_VILLAGE_HOUSES);

        if (poolOpt.isEmpty()) return;

        StructureTemplatePool pool = poolOpt.get().value();
        StructurePoolAccessor accessor = (StructurePoolAccessor) pool;
        String targetId = REVOLVERMAKER_HOUSE_ELEM.toString();

        List<Pair<StructurePoolElement, Integer>> rawFiltered = accessor.getRawTemplates()
                .stream()
                .filter(pair -> !pair.getFirst().toString().contains(targetId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        accessor.setRawTemplates(rawFiltered);

        ObjectArrayList<StructurePoolElement> elemFiltered = accessor.getElements()
                .stream()
                .filter(e -> !e.toString().contains(targetId))
                .collect(java.util.stream.Collectors.toCollection(ObjectArrayList::new));
        accessor.setElements(elemFiltered);

        NstRevRecoiled.LOGGER.info("Cleared {} from {}", targetId, TAIGA_VILLAGE_HOUSES.location());
    }
}
