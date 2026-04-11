package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic stone bullet implementation.
 * Deals 2.0 damage on hit.
 */
public class StoneBulletItem extends BaseBulletItem {
    public StoneBulletItem(Item.Properties properties) {
        super(properties, 4.0f);
    }
}
