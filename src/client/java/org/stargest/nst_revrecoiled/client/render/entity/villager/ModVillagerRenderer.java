package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for the revolvermaker villager profession.
 * Extends the vanilla VillagerEntityRenderer to override texture selection,
 * suppress vanilla clothing layers, and fix revolver rendering in the villager's hand.
 *
 * Key features:
 * - Custom texture applied when the villager's profession is revolvermaker
 * - Falls back to vanilla texture resolution for all other professions
 * - Replaces the vanilla VillagerHeldItemFeatureRenderer with
 *   ModVillagerHeldItemFeatureRenderer to render revolvers as 2D flat items
 *   instead of triggering the GeckoLib 3D model when displayed during trades
 */
public class ModVillagerRenderer extends VillagerEntityRenderer {

    private static final Identifier REVOLVERMAKER_TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/villager/revolvermaker.png");

    public ModVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.features.removeIf(f -> f instanceof VillagerHeldItemFeatureRenderer);
        this.addFeature(new ModVillagerHeldItemFeatureRenderer<>(this));
    }

    @Override
    public Identifier getTexture(VillagerEntity entity) {
        if (entity.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTexture(entity);
    }
}
