package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Diamond Revolver item.
 * Uses the BaseRevolverItemRenderer logic for model/icon switching.
 *
 * Adapted for Forge 1.20.1.
 */
public class DiamondRevolverItemRenderer extends BaseRevolverItemRenderer<DiamondRevolverItem> {
    public DiamondRevolverItemRenderer() {
        super(new RevolverItemModel<>());
    }
}
