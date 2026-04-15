package org.stargest.nst_revrecoiled.Items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.stargest.nst_revrecoiled.util.ModConfig;

/**
 * Base class for all bullet items.
 * Stores bullet-specific properties like damage.
 * Adapted for Forge 1.20.1.
 */
public class BaseBulletItem extends Item {

    private final float fallbackDamage;

    public BaseBulletItem(Item.Properties properties, float damage) {
        super(properties);
        this.fallbackDamage = damage;
    }

    /**
     * @return The damage value of this bullet, fetched from configuration if available.
     */
    public float getDamage() {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        Float configDamage = ModConfig.get().bullets.damage.get(id);
        return configDamage != null ? configDamage : fallbackDamage;
    }
}
