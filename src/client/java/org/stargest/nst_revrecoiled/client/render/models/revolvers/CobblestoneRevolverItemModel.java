package org.stargest.nst_revrecoiled.client.render.models.revolvers;


import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * GeckoLib model definition for the Cobblestone Revolver.
 * Links the item to its .geo.json model, texture, and animations.
 */
public class CobblestoneRevolverItemModel extends GeoModel<CobblestoneRevolverItem> {

    @Override
    public Identifier getModelResource(CobblestoneRevolverItem animatable, GeoRenderer<CobblestoneRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "geo/revolver.geo.json");
    }

    @Override
    public Identifier getTextureResource(CobblestoneRevolverItem animatable, GeoRenderer<CobblestoneRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "textures/item/cobblestone_revolver.png");
    }

    @Override
    public Identifier getAnimationResource(CobblestoneRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "animations/revolver.animation.json");
    }
}
