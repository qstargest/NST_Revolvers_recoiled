package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.resources.ResourceLocation;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import software.bernie.geckolib.model.GeoModel;

/**
 * Unified GeckoLib model for all revolvers.
 * Shares the same geometry and animations, differs only in texture.
 */
public class RevolverItemModel<T extends BaseRevolverItem> extends GeoModel<T> {

    public RevolverItemModel() {
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(NstRevRecoiled.MOD_ID, "geo/revolver.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return new ResourceLocation(NstRevRecoiled.MOD_ID, "textures/item/" + animatable.getTextureName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation(NstRevRecoiled.MOD_ID, "animations/revolver.animation.json");
    }
}
