package org.stargest.nst_revrecoiled.Items.Revolvers;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Diamond-tier revolver implementation.
 * Deals 7.0 base damage plus bullet damage.
 */
public class DiamondRevolverItem extends BaseRevolverItem {
    public DiamondRevolverItem(Item.Properties properties) {
        super(properties, 11.0f, 1561);
    }
}
