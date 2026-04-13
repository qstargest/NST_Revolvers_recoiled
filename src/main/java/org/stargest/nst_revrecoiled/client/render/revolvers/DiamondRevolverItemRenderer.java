package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Diamond Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class DiamondRevolverItemRenderer extends BaseRevolverItemRenderer<DiamondRevolverItem> {
    public DiamondRevolverItemRenderer() {
        super(new RevolverItemModel<>("diamond_revolver"));
    }
}
