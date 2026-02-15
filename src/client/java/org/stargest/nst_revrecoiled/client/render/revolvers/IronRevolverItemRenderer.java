package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.IronRevolverItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for the Iron Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class IronRevolverItemRenderer extends GeoItemRenderer<IronRevolverItem> {
    public IronRevolverItemRenderer() {
        super(new IronRevolverItemModel());
    }
}
