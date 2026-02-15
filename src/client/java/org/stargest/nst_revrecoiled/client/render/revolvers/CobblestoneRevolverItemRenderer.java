package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.CobblestoneRevolverItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for the Cobblestone Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class CobblestoneRevolverItemRenderer extends GeoItemRenderer<CobblestoneRevolverItem> {
    public CobblestoneRevolverItemRenderer() {
        super(new CobblestoneRevolverItemModel());
    }
}
