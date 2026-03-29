package org.stargest.nst_revrecoiled.client.render.models.revolvers;

import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for the Iron Revolver.
 * Links the item to Cobblestone Revolver .geo.json model and animations, links texture to Golden Revolver.
 */
public class IronRevolverItemModel extends GeoModel<IronRevolverItem> {

    @Override
    public Identifier getModelResource(IronRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "geo/cobblestone_revolver.geo.json");
    }

    @Override
    public Identifier getTextureResource(IronRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "textures/item/iron_revolver.png");
    }

    @Override
    public Identifier getAnimationResource(IronRevolverItem animatable) {
        return Identifier.of(Main.MOD_ID, "animations/cobblestone_revolver.animation.json");
    }
}
