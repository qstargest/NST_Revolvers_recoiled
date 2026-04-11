package org.stargest.nst_revrecoiled.Items.Revolvers;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Iron-tier revolver implementation.
 * Deals 5.0 base damage plus bullet damage.
 */
public class IronRevolverItem extends BaseRevolverItem {
    public IronRevolverItem(Item.Properties properties) {
        super(properties, 9.0f, 250);
    }
}
