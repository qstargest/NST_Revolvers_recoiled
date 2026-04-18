package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Golden Revolver item.
 * Uses the BaseRevolverItemRenderer logic for model/icon switching.
 *
 * Adapted for Forge 1.20.1.
 */
public class GoldenRevolverItemRenderer extends BaseRevolverItemRenderer<GoldenRevolverItem> {
    public GoldenRevolverItemRenderer() {
        super(new RevolverItemModel<>());
    }
}
