package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Cobblestone Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class CobblestoneRevolverItemRenderer extends BaseRevolverItemRenderer<CobblestoneRevolverItem> {
    public CobblestoneRevolverItemRenderer() {
        super(new RevolverItemModel<>("cobblestone_revolver"));
    }
}
