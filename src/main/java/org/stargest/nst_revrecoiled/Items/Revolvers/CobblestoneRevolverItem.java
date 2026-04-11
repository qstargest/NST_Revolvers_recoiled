package org.stargest.nst_revrecoiled.Items.Revolvers;

import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Cobblestone-tier revolver implementation.
 * Deals 4.0 base damage plus bullet damage.
 */
public class CobblestoneRevolverItem extends BaseRevolverItem {
    public CobblestoneRevolverItem(Item.Properties properties) {
        super(properties, 6.0f, 131);
    }
}
