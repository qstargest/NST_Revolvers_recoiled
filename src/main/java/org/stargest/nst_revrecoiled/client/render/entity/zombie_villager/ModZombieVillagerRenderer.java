package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for revolvermaker zombie villagers.
 * Overrides texture selection to apply the custom revolvermaker profession
 * texture to zombie villagers when applicable, falling back to vanilla
 * variant-based textures for all other professions.
 *
 * Adapted for Forge 1.20.1.
 */
public class ModZombieVillagerRenderer extends ZombieVillagerRenderer {

    private static final ResourceLocation REVOLVERMAKER_TEXTURE =
            new ResourceLocation(Main.MODID, "textures/entity/zombie_villager/profession/revolvermaker.png");

    public ModZombieVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZombieVillager entity) {
        if (BuiltInRegistries.VILLAGER_PROFESSION.getKey(entity.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
