package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombieVillager;
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

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZombieVillager state) {
        if ((BuiltInRegistries.VILLAGER_PROFESSION.getKey(state.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location()))) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTextureLocation(state);
    }
}
