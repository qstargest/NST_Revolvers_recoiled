package org.stargest.nst_revrecoiled.Items.Revolvers;

import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Iron-tier revolver implementation.
 * Deals 5.0 base damage plus bullet damage.
 */
public class IronRevolverItem extends BaseRevolverItem {

    private static final float IRON_REVOLVER_DAMAGE = 5.0f;
    private static final int MAX_DURABILITY = 250;

    public IronRevolverItem(Settings settings) {
        super(settings, IRON_REVOLVER_DAMAGE, MAX_DURABILITY);
    }
}
