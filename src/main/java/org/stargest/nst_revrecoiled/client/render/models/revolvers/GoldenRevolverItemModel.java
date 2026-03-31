package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for the Golden Revolver.
 * Links the item to Cobblestone Revolver .geo.json model and animations, links texture to Golden Revolver.
 */
public class GoldenRevolverItemModel extends GeoModel<GoldenRevolverItem> {

    @Override
    public Identifier getModelResource(GoldenRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "geo/cobblestone_revolver.geo.json");
    }

    @Override
    public Identifier getTextureResource(GoldenRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "textures/item/golden_revolver.png");
    }

    @Override
    public Identifier getAnimationResource(GoldenRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "animations/cobblestone_revolver.animation.json");
    }
}
