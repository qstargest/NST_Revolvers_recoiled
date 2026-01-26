package org.stargest.nst_revrecoiled.util;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Bullets.StoneBulletItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.DioriteRevolverItem;
import org.stargest.nst_revrecoiled.Main;

import java.util.function.Function;

/**
 * Registry for all custom items in the mod.
 * Uses factory pattern for cleaner registration.
 */
public class ModItems {

    // Bullets
    public static final Item STONE_BULLET = register(
            "stone_bullet",
            StoneBulletItem::new,
            new Item.Settings()
    );

    // Revolvers
    public static final Item DIORITE_REVOLVER = register(
            "diorite_revolver",
            DioriteRevolverItem::new,
            new Item.Settings().maxCount(1)
    );

    /**
     * Registers an item using a factory pattern.
     *
     * @param name The item's registry name
     * @param factory Function that creates the item from settings
     * @param settings Base item settings
     * @return The registered item instance
     */
    private static Item register(String name,
                                 Function<Item.Settings, Item> factory,
                                 Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(
                RegistryKeys.ITEM,
                Identifier.of(Main.MOD_ID, name)
        );

        Item item = factory.apply(settings.registryKey(key));
        Registry.register(Registries.ITEM, key, item);

        return item;
    }

    /**
     * Initializes all items and adds them to creative tabs.
     */
    public static void init() {
        Main.LOGGER.info("Registering items for {}", Main.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(STONE_BULLET);
            entries.add(DIORITE_REVOLVER);
        });
    }
}
