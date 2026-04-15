package org.stargest.nst_revrecoiled.mixins;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Accessor mixin exposing the internal element lists of StructureTemplatePool.
 * Used by ModStructures to inject the revolvermaker house into village pools.
 * Adapted for Forge 1.20.1.
 */
@Mixin(StructureTemplatePool.class)
public interface StructurePoolAccessor {

    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> getRawTemplates();

    @org.spongepowered.asm.mixin.Mutable
    @Accessor("rawTemplates")
    void setRawTemplates(List<Pair<StructurePoolElement, Integer>> rawTemplates);

    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> getElements();

    @org.spongepowered.asm.mixin.Mutable
    @Accessor("templates")
    void setElements(ObjectArrayList<StructurePoolElement> elements);
}
