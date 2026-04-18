package org.stargest.nst_revrecoiled.client.render.revolvers;

import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.client.render.models.revolvers.RevolverItemModel;

/**
 * GeckoLib renderer for the Iron Revolver item.
 * Uses the BaseRevolverItemRenderer logic for model/icon switching.
 * Adapted for Forge 1.20.1.
 */
public class IronRevolverItemRenderer extends BaseRevolverItemRenderer<IronRevolverItem> {
    public IronRevolverItemRenderer() {
        super(new RevolverItemModel<>());
    }
}
