package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for the revolvermaker villager profession.
 * Extends the vanilla VillagerRenderer to override texture selection,
 * suppress vanilla clothing layers, and fix revolver rendering in the villager's hand.
 *
 * Key features:
 * - Custom texture applied when the villager's profession is revolvermaker
 * - Falls back to vanilla texture resolution for all other professions
 * - Replaces the vanilla CrossedArmsItemLayer with ModVillagerHeldItemFeatureRenderer
 *   to render revolvers as 2D flat items instead of triggering the GeckoLib 3D
 *   model when displayed during trades.
 *
 * Adapted for Forge 1.20.1.
 */
public class ModVillagerRenderer extends VillagerRenderer {

    private static final ResourceLocation REVOLVERMAKER_TEXTURE =
            new ResourceLocation(Main.MODID, "textures/entity/villager/revolvermaker.png");

    public ModVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.removeIf(f -> f instanceof CrossedArmsItemLayer);
        this.addLayer(new ModVillagerHeldItemFeatureRenderer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Villager entity) {
        if (BuiltInRegistries.VILLAGER_PROFESSION.getKey(entity.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTextureLocation(entity);
    }
}
