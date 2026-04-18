package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Cobblestone Revolver item.
 * Uses the BaseRevolverItemRenderer logic for model/icon switching.
 * Adapted for Forge 1.20.1.
 */
public class CobblestoneRevolverItemRenderer extends BaseRevolverItemRenderer<CobblestoneRevolverItem> {
    public CobblestoneRevolverItemRenderer() {
        super(new RevolverItemModel<>());
    }
}
