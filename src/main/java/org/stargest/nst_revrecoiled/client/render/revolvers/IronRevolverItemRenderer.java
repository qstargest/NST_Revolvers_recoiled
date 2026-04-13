package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Iron Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class IronRevolverItemRenderer extends BaseRevolverItemRenderer<IronRevolverItem> {
    public IronRevolverItemRenderer() {
        super(new RevolverItemModel<>("iron_revolver"));
    }
}
