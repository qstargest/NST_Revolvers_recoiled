package org.stargest.nst_revrecoiled.Items.Bullets;

import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Iron bullet implementation.
 * Deals 3.0 damage on hit.
 */
public class IronBulletItem extends BaseBulletItem {

    private static final float IRON_BULLET_DAMAGE = 3.0f;

    public IronBulletItem(Settings settings) {
        super(settings, IRON_BULLET_DAMAGE);
    }
}
