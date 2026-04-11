package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Iron bullet implementation.
 * Deals 3.0 damage on hit.
 */
public class IronBulletItem extends BaseBulletItem {
    public IronBulletItem(Item.Properties properties) {
        super(properties, 5.0f);
    }
}
