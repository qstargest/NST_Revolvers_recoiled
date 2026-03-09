package org.stargest.nst_revrecoiled.util;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Items.Bullets.DiamondBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.GoldenBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.IronBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.StoneBulletItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
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

   public static final Item IRON_BULLET = register(
           "iron_bullet",
           IronBulletItem::new,
           new Item.Settings()
   );

    public static final Item GOLDEN_BULLET = register(
            "golden_bullet",
            GoldenBulletItem::new,
            new Item.Settings()
    );

   public static final Item DIAMOND_BULLET = register(
           "diamond_bullet",
           DiamondBulletItem::new,
           new Item.Settings()
   );

    // Revolvers
    public static final Item COBBLESTONE_REVOLVER = register(
            "cobblestone_revolver",
            CobblestoneRevolverItem::new,
            new Item.Settings().maxCount(1)
    );

    public static final Item IRON_REVOLVER = register(
            "iron_revolver",
            IronRevolverItem::new,
            new Item.Settings().maxCount(1)
    );

    public static final Item GOLDEN_REVOLVER = register(
            "golden_revolver",
            GoldenRevolverItem::new,
            new Item.Settings().maxCount(1)
    );

    public static final Item DIAMOND_REVOLVER = register(
            "diamond_revolver",
            DiamondRevolverItem::new,
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
            entries.add(COBBLESTONE_REVOLVER);
            entries.add(IRON_BULLET);
            entries.add(IRON_REVOLVER);
            entries.add(GOLDEN_BULLET);
            entries.add(GOLDEN_REVOLVER);
            entries.add(DIAMOND_BULLET);
            entries.add(DIAMOND_REVOLVER);
        });
    }
}
