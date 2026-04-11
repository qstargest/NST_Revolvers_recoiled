package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Basic Diamond bullet implementation.
 * Deals 5.0 damage on hit.
 */
public class DiamondBulletItem extends BaseBulletItem {
    public DiamondBulletItem(Item.Properties properties) {
        super(properties, 11.0f);
    }
}
