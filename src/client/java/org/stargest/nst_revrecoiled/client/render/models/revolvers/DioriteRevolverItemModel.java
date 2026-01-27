package org.stargest.nst_revrecoiled.client.render.models.revolvers;


import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Revolvers.DioriteRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * GeckoLib model definition for the Diorite Revolver.
 * Links the item to its .geo.json model, texture, and animations.
 */
public class DioriteRevolverItemModel extends GeoModel<DioriteRevolverItem> {

    @Override
    public Identifier getModelResource(DioriteRevolverItem animatable, GeoRenderer<DioriteRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "geo/diorite_revolver.geo.json");
    }

    @Override
    public Identifier getTextureResource(DioriteRevolverItem animatable, GeoRenderer<DioriteRevolverItem> renderer) {
        return Identifier.of(Main.MOD_ID, "textures/item/diorite_revolver.png");
    }

    @Override
    public Identifier getAnimationResource(DioriteRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "animations/diorite_revolver.animation.json");
    }
}
