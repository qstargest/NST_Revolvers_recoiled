package org.stargest.nst_revrecoiled.Items.Bullets;

import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Diamond bullet implementation.
 * Deals 5.0 damage on hit.
 */
public class DiamondBulletItem extends BaseBulletItem {

    private static final float DIAMOND_BULLET_DAMAGE = 5.0f;

    public DiamondBulletItem(Settings settings) {
        super(settings, DIAMOND_BULLET_DAMAGE);
    }
}
