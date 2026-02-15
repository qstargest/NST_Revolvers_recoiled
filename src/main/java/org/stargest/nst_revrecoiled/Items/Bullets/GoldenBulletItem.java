package org.stargest.nst_revrecoiled.Items.Bullets;

import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Golden bullet implementation.
 * Deals 4.0 damage on hit.
 */
public class GoldenBulletItem extends BaseBulletItem {

    private static final float GOLDEN_BULLET_DAMAGE = 4.0f;

    public GoldenBulletItem(Settings settings) {
        super(settings, GOLDEN_BULLET_DAMAGE);
    }
}
