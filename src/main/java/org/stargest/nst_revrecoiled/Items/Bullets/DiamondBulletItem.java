package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Diamond bullet implementation.
 * Deals 8.0 damage on hit (base).
 */
public class DiamondBulletItem extends BaseBulletItem {
    public DiamondBulletItem(Item.Properties properties) {
        super(properties, 8.0f);
    }
}
