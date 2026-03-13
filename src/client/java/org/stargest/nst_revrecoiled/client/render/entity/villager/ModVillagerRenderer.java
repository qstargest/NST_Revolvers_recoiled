package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for the revolvermaker villager profession.
 * Extends the vanilla VillagerEntityRenderer to override texture selection
 * and suppress vanilla clothing layers for revolvermaker villagers.
 *
 * Key features:
 * - Custom texture applied when the villager's profession is revolvermaker
 * - Falls back to vanilla texture resolution for all other professions
 * - Passes profession state into VillagerEntityRenderState via VillagerRenderStateAccess
 *   so that downstream feature renderers (e.g. VillagerClothingFeatureRendererMixin)
 *   can suppress the vanilla profession overlay without re-querying entity data
 *
 * Profession detection happens once in updateRenderState() per frame,
 * keeping getTexture() a simple branch with no entity lookups.
 */
public class ModVillagerRenderer extends VillagerEntityRenderer {

    /**
     * Texture applied to all revolvermaker villagers regardless of biome variant.
     */
    private static final Identifier REVOLVERMAKER_TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/villager/revolvermaker.png");

    public ModVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    /**
     * Populates render state with revolvermaker profession flag.
     * Called once per frame before rendering; result is consumed by
     * getTexture() and VillagerClothingFeatureRendererMixin to avoid
     * repeated profession lookups during the render pass.
     */
    @Override
    public void updateRenderState(VillagerEntity entity, VillagerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        ((VillagerRenderStateAccess) state).nst$setIsRevolvermaker(
                entity.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER
        );
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
