package org.stargest.nst_revrecoiled.Items.Bullets;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;

/**
 * Golden bullet implementation.
 * Deals 5.0 damage on hit (base).
 */
public class GoldenBulletItem extends BaseBulletItem {
    public GoldenBulletItem(Item.Properties properties) {
        super(properties, 5.0f);
    }
}
