package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.resources.ResourceLocation;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;

/**
 * Unified GeckoLib model for all revolvers.
 * Shares the same geometry and animations, differs only in texture.
 * Adapted for Forge 1.20.1.
 */
public class RevolverItemModel<T extends BaseRevolverItem> extends GeoModel<T> {

    public RevolverItemModel() {
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(Main.MODID, "geo/revolver.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return new ResourceLocation(Main.MODID, "textures/item/" + animatable.getTextureName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation(Main.MODID, "animations/revolver.animation.json");
    }
}
