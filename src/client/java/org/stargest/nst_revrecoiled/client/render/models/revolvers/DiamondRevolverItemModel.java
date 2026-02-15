package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * GeckoLib model definition for the Diamond Revolver.
 * Links the item to Cobblestone Revolver .geo.json model and animations, links texture to Diamond Revolver.
 */
public class DiamondRevolverItemModel extends GeoModel<DiamondRevolverItem> {

    @Override
    public Identifier getModelResource(DiamondRevolverItem animatable, GeoRenderer<DiamondRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "geo/cobblestone_revolver.geo.json");
    }

    @Override
    public Identifier getTextureResource(DiamondRevolverItem animatable, GeoRenderer<DiamondRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "textures/item/diamond_revolver.png");
    }

    @Override
    public Identifier getAnimationResource(DiamondRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "animations/cobblestone_revolver.animation.json");
    }
}
