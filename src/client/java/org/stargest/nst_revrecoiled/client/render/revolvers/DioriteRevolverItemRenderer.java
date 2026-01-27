package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.DioriteRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.DioriteRevolverItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for the Diorite Revolver item.
 * Handles 3D model rendering in hand and ground.
 */
public class DioriteRevolverItemRenderer extends GeoItemRenderer<DioriteRevolverItem> {
    public DioriteRevolverItemRenderer() {
        super(new DioriteRevolverItemModel());
    }
}
