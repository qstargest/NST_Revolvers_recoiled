package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.GoldenRevolverItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for the Golden Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class GoldenRevolverItemRenderer extends BaseRevolverItemRenderer<GoldenRevolverItem> {
    public GoldenRevolverItemRenderer() {
        super(new GoldenRevolverItemModel());
    }
}
