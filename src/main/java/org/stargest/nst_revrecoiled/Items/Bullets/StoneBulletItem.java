package org.stargest.nst_revrecoiled.Items.Bullets;

import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic stone bullet implementation.
 * Deals 2.0 damage on hit.
 */
public class StoneBulletItem extends BaseBulletItem {

    private static final float STONE_BULLET_DAMAGE = 2.0f;

    public StoneBulletItem(Settings settings) {
        super(settings, STONE_BULLET_DAMAGE);
    }
}
