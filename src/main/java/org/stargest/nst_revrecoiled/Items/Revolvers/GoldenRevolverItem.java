package org.stargest.nst_revrecoiled.Items.Revolvers;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Golden-tier revolver implementation.
 * Deals 6.0 base damage plus bullet damage.
 */
public class GoldenRevolverItem extends BaseRevolverItem {
    public GoldenRevolverItem(Item.Properties properties) {
        super(properties, 8.0f, 32);
    }
}
