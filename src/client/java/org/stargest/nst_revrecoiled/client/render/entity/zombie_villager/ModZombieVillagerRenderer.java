package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieVillagerEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieVillagerRenderState;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for revolvermaker zombie villagers.
 * Extends ZombieVillagerEntityRenderer to override texture selection and
 * propagate the revolvermaker flag into ZombieVillagerEntityRenderState so
 * that VillagerClothingFeatureRendererMixin can suppress the clothing layer.
 *
 * Texture and flag logic mirrors ModVillagerRenderer. Held item is not
 * captured here because zombie villagers carry no trade-display items.
 */
public class ModZombieVillagerRenderer extends ZombieVillagerEntityRenderer {

    /**
     * Texture applied to all revolvermaker zombie villagers regardless of biome variant.
     * Reuses the same asset as the living revolvermaker villager.
     */
    private static final Identifier REVOLVERMAKER_TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/zombie_villager/profession/revolvermaker.png");

    public ModZombieVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    /**
     * Propagates the revolvermaker flag into the render state once per frame.
     * Consumed by getTexture() and VillagerClothingFeatureRendererMixin.
     */
    @Override
    public void updateRenderState(ZombieVillagerEntity entity,
                                  ZombieVillagerRenderState state,
                                  float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        ((ZombieVillagerRenderStateAccess) state).nst$setIsRevolvermaker(
                entity.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER
        );
    }

    /**
     * Returns the revolvermaker texture when the flag is set,
     * otherwise delegates to vanilla zombie villager texture resolution.
     */
    @Override
    public Identifier getTexture(ZombieVillagerRenderState state) {
        if (((ZombieVillagerRenderStateAccess) state).nst$isRevolvermaker()) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTexture(state);
    }
}
