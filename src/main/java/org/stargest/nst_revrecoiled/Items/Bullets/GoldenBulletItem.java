package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Golden bullet implementation.
 * Deals 4.0 damage on hit.
 */
public class GoldenBulletItem extends BaseBulletItem {
    public GoldenBulletItem(Item.Properties properties) {
        super(properties, 6.0f);
    }
}
