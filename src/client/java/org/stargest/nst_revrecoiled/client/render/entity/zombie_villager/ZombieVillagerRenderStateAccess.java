package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

/**
 * Mixin accessor interface that extends ZombieVillagerEntityRenderState with
 * revolvermaker profession state. Mirrors VillagerRenderStateAccess for the
 * zombie villager render pipeline.
 *
 * Implemented by ZombieVillagerEntityRenderStateMixin via @Mixin + @Unique injection.
 */
public interface ZombieVillagerRenderStateAccess {

    boolean nst$isRevolvermaker();

    void nst$setIsRevolvermaker(boolean value);
}
