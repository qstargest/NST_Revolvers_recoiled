package org.stargest.nst_revrecoiled.Items.Revolvers;

import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Diamond-tier revolver implementation.
 * Deals 7.0 base damage plus bullet damage.
 */
public class DiamondRevolverItem extends BaseRevolverItem {

    private static final float DIAMOND_REVOLVER_DAMAGE = 7.0f;

    public DiamondRevolverItem(Settings settings) {
        super(settings, DIAMOND_REVOLVER_DAMAGE);
    }
}
