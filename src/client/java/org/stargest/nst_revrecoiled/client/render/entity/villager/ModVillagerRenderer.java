package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
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
 * - Passes profession state and held item into VillagerEntityRenderState via
 *   VillagerRenderStateAccess so downstream feature renderers can access both
 *   without re-querying entity data
 * - Replaces the vanilla VillagerHeldItemFeatureRenderer with
 *   ModVillagerHeldItemFeatureRenderer to render revolvers as 2D flat items
 *   instead of triggering the GeckoLib 3D model when displayed during trades
 *
 * Profession detection and held item capture happen once per frame in
 * updateRenderState(), keeping getTexture() and the feature renderer
 * as simple, entity-free reads.
 */
public class ModVillagerRenderer extends VillagerEntityRenderer {

    /**
     * Texture applied to all revolvermaker villagers regardless of biome variant.
     */
    private static final Identifier REVOLVERMAKER_TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/villager/revolvermaker.png");

    /**
     * Removes the vanilla held item feature renderer and replaces it with the
     * mod's implementation that handles revolver items correctly.
     */
    public ModVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.features.removeIf(f -> f instanceof VillagerHeldItemFeatureRenderer);
        this.addFeature(new ModVillagerHeldItemFeatureRenderer<>(this));
    }

    /**
     * Populates render state with the revolvermaker flag and current held item.
     * Called once per frame before rendering; results are consumed by getTexture()
     * and ModVillagerHeldItemFeatureRenderer to avoid repeated entity lookups
     * during the render pass.
     */
    @Override
    public void updateRenderState(VillagerEntity entity, VillagerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        VillagerRenderStateAccess access = (VillagerRenderStateAccess) state;
        access.nst$setIsRevolvermaker(
                entity.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER
        );
        access.nst$setHeldItem(entity.getMainHandStack());
    }

    /**
     * Returns the revolvermaker texture when the render state flag is set,
     * otherwise delegates to vanilla texture resolution (handles biome variants, etc.).
     */
    @Override
    public Identifier getTexture(VillagerEntityRenderState state) {
        if (((VillagerRenderStateAccess) state).nst$isRevolvermaker()) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTexture(state);
    }
}
