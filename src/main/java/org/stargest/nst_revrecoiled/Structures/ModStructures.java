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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.mixins.StructurePoolAccessor;
import org.stargest.nst_revrecoiled.util.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Injects the revolvermaker house into vanilla village house pools.
 * Adapted for Forge 1.20.1.
 */
public class ModStructures {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.MODID);

    private static final ResourceKey<StructureTemplatePool> TAIGA_VILLAGE_HOUSES =
            ResourceKey.create(Registries.TEMPLATE_POOL,
                    new ResourceLocation("village/taiga/houses"));

    private static final ResourceLocation REVOLVERMAKER_HOUSE_ELEM =
            new ResourceLocation(Main.MODID, "revolvermaker_house");

    public static void reinit(MinecraftServer server) {
        clearInjectedPools(server);
        init(server);
    }

    private static void init(MinecraftServer server) {
        if (!ModConfig.get().worldGen.spawnHouse) {
            LOGGER.info("Revolvermaker house generation disabled via config.");
            return;
        }

        int weight = ModConfig.get().worldGen.houseWeight;

        Optional<Holder.Reference<StructureTemplatePool>> poolOpt = server.registryAccess()
                .lookupOrThrow(Registries.TEMPLATE_POOL)
                .get(TAIGA_VILLAGE_HOUSES);

        if (poolOpt.isEmpty()) {
            LOGGER.warn("Template pool {} not found! Skipping injection.",
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

        LOGGER.info("Injected {} into {} with weight {}",
                REVOLVERMAKER_HOUSE_ELEM, TAIGA_VILLAGE_HOUSES.location(), weight);
    }

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

        LOGGER.info("Cleared {} from {}", targetId, TAIGA_VILLAGE_HOUSES.location());
    }
}
