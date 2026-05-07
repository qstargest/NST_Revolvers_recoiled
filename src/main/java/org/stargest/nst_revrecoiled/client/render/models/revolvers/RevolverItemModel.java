package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.resources.ResourceLocation;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Unified GeckoLib model for all revolvers.
 * Shares the same geometry and animations, differs only in texture.
 */
public class RevolverItemModel<T extends BaseRevolverItem> extends GeoModel<T> {

    private final String textureName;

    public RevolverItemModel(String textureName) {
        this.textureName = textureName;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "geo/revolver.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "textures/item/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getModelResource(T animatable, GeoRenderer<T> renderer) {
        return ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "geo/revolver.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable, GeoRenderer<T> renderer) {
        return ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "textures/item/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "animations/revolver.animation.json");
    }
}
