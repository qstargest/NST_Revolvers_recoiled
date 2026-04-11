package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
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
public class ModZombieVillagerRenderer extends ZombieVillagerRenderer {

    /**
     * Texture applied to all revolvermaker zombie villagers regardless of biome variant.
     * Reuses the same asset as the living revolvermaker villager.
    */
    private static final ResourceLocation REVOLVERMAKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "textures/entity/zombie_villager/profession/revolvermaker.png");

    public ModZombieVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Propagates the revolvermaker flag into the render state once per frame.
     * Consumed by getTexture() and VillagerClothingFeatureRendererMixin.
    */
    @Override
    public void extractRenderState(@NotNull ZombieVillager entity, @NotNull ZombieVillagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((ZombieVillagerRenderStateAccess) state).nst$setIsRevolvermaker(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(entity.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())
        );
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZombieVillagerRenderState state) {
        if (((ZombieVillagerRenderStateAccess) state).nst$isRevolvermaker()) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTextureLocation(state);
    }
}
