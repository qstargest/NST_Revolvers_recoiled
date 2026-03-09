package org.stargest.nst_revrecoiled.Items.Revolvers;

import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Golden-tier revolver implementation.
 * Deals 6.0 base damage plus bullet damage.
 */
public class GoldenRevolverItem extends BaseRevolverItem {

    private static final float GOLDEN_REVOLVER_DAMAGE = 6.0f;

    public GoldenRevolverItem(Settings settings) {
        super(settings, GOLDEN_REVOLVER_DAMAGE);
    }
}
