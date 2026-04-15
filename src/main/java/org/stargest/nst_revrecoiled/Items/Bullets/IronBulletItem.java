package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Iron bullet implementation.
 * Deals 6.0 damage on hit (base).
 */
public class IronBulletItem extends BaseBulletItem {
    public IronBulletItem(Item.Properties properties) {
        super(properties, 6.0f);
    }
}
