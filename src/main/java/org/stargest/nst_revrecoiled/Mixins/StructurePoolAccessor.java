package org.stargest.nst_revrecoiled.Mixins;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructurePool.class)
public interface StructurePoolAccessor {
    /** Weighted list of (element, weight) pairs — drives piece selection display/serialisation. */
    @Accessor("elementCounts")
    List<Pair<StructurePoolElement, Integer>> getElementWeights();

    @Mutable
    @Accessor("elementCounts")
    void setElementWeights(List<Pair<StructurePoolElement, Integer>> elementWeights);

    /** Flat list with each element repeated by its weight — used for random selection. */
    @Accessor("elements")
    ObjectArrayList<StructurePoolElement> getElements();

    @Mutable
    @Accessor("elements")
    void setElements(ObjectArrayList<StructurePoolElement> elements);
}
