package org.stargest.nst_revrecoiled.Items;

import net.minecraft.item.Item;

/**
 * Base class for all bullet items.
 * Stores bullet-specific properties like damage.
 */
public class BaseBulletItem extends Item {

    private final float damage;

    /**
     * @param settings Item settings
     * @param damage Damage dealt by this bullet type
     */
    public BaseBulletItem(Settings settings, float damage) {
        super(settings);
        this.damage = damage;
    }

    /**
     * @return The damage value of this bullet
     */
    public float getDamage() {
        return damage;
    }
}
