package org.stargest.nst_revrecoiled.client.render.entity.villager;

import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import net.minecraft.world.entity.npc.Villager;

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
public class ModVillagerRenderer extends VillagerRenderer {

    /**
     * Texture applied to all revolvermaker villagers regardless of biome variant.
     */
    private static final ResourceLocation REVOLVERMAKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "textures/entity/villager/revolvermaker.png");

    public ModVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.removeIf(f -> f instanceof CrossedArmsItemLayer);
        this.addLayer(new ModVillagerHeldItemFeatureRenderer(this));
    }

    /**
     * Populates render state with the revolvermaker flag and current held item.
     * Called once per frame before rendering; results are consumed by getTexture()
     * and ModVillagerHeldItemFeatureRenderer to avoid repeated entity lookups
     * during the render pass.
     */
    @Override
    public void extractRenderState(@NotNull Villager entity, @NotNull VillagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        VillagerRenderStateAccess access = (VillagerRenderStateAccess) state;
        access.nst$setIsRevolvermaker(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(entity.getVillagerData().getProfession()).equals(ModVillagers.REVOLVERMAKER_KEY.location())
        );
        access.nst$setHeldItem(entity.getMainHandItem().copy());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VillagerRenderState state) {
        if (((VillagerRenderStateAccess) state).nst$isRevolvermaker()) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTextureLocation(state);
    }
}
