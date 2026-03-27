package org.stargest.nst_revrecoiled.Items;

import org.stargest.nst_revrecoiled.util.ModConfig;
import net.minecraft.item.Item;

/**
 * Base class for all bullet items.
 * Stores bullet-specific properties like damage.
 */
public class BaseBulletItem extends Item {

    private final float fallbackDamage;

    /**
     * @param settings Item settings
     * @param damage Damage dealt by this bullet type
     */
    public BaseBulletItem(Settings settings, float damage) {
        super(settings);
        this.fallbackDamage = damage;
    }

    /**
     * @return The damage value of this bullet, fetched from configuration if available.
     */
    public float getDamage() {
        String id = net.minecraft.registry.Registries.ITEM.getId(this).getPath();
        Float configDamage = ModConfig.get().bullets.damage.get(id);
        return configDamage != null ? configDamage : fallbackDamage;
    }
}
