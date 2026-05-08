package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.Items.Bullets.DiamondBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.GoldenBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.IronBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.StoneBulletItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

/**
 * Registry for all custom items in the mod.
 * Uses DeferredRegister for clean registration in NeoForge.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NstRevRecoiled.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NstRevRecoiled.MOD_ID);

    // --- Bullets ---
    public static final DeferredItem<StoneBulletItem> STONE_BULLET = ITEMS.registerItem(
            "stone_bullet",
            StoneBulletItem::new,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<IronBulletItem> IRON_BULLET = ITEMS.registerItem(
            "iron_bullet",
            IronBulletItem::new,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<GoldenBulletItem> GOLDEN_BULLET = ITEMS.registerItem(
            "golden_bullet",
            GoldenBulletItem::new,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<DiamondBulletItem> DIAMOND_BULLET = ITEMS.registerItem(
            "diamond_bullet",
            DiamondBulletItem::new,
            new Item.Properties().stacksTo(64)
    );

    // --- Revolvers ---
    public static final DeferredItem<CobblestoneRevolverItem> COBBLESTONE_REVOLVER = ITEMS.registerItem(
            "cobblestone_revolver",
            CobblestoneRevolverItem::new,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<IronRevolverItem> IRON_REVOLVER = ITEMS.registerItem(
            "iron_revolver",
            IronRevolverItem::new,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<GoldenRevolverItem> GOLDEN_REVOLVER = ITEMS.registerItem(
            "golden_revolver",
            GoldenRevolverItem::new,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<DiamondRevolverItem> DIAMOND_REVOLVER = ITEMS.registerItem(
            "diamond_revolver",
            DiamondRevolverItem::new,
            new Item.Properties().stacksTo(1)
    );

    // Internal items for 2D GUI icon baking - these ensure [item]_revolver_item.json models are baked.
    // They are not added to creative tabs and remain hidden from players.
    // Uses the registerIcon helper for consistent naming and settings.
    static final DeferredItem<Item> COBBLESTONE_REVOLVER_ICON = registerIcon("cobblestone_revolver");
    static final DeferredItem<Item> IRON_REVOLVER_ICON = registerIcon("iron_revolver");
    static final DeferredItem<Item> GOLDEN_REVOLVER_ICON = registerIcon("golden_revolver");
    static final DeferredItem<Item> DIAMOND_REVOLVER_ICON = registerIcon("diamond_revolver");

    private static DeferredItem<Item> registerIcon(String baseName) {
        return ITEMS.registerItem(baseName + "_item", Item::new, new Item.Properties());
    }

    // Spawn Eggs
    public static final DeferredItem REVOLVER_BANDIT_SPAWN_EGG = ITEMS.registerItem(
            "revolver_bandit_spawn_egg",
            settings -> new SpawnEggItem(ModEntities.REVOLVER_BANDIT.get(), 0x2B5C6B, 0x949C9C, settings),
            new Item.Properties());

    // --- Creative Tab ---
    public static final ResourceKey<CreativeModeTab> MOD_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            new ResourceLocation(NstRevRecoiled.MOD_ID, "main")
    );

    static {
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.nst_revrecoiled.main"))
                .icon(() -> COBBLESTONE_REVOLVER.get().getDefaultInstance())
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                .displayItems((params, output) -> {
                    output.accept(ModBlocks.ASSEMBLY_TABLE.get());
                    output.accept(STONE_BULLET.get());
                    output.accept(COBBLESTONE_REVOLVER.get());
                    output.accept(IRON_BULLET.get());
                    output.accept(IRON_REVOLVER.get());
                    output.accept(GOLDEN_BULLET.get());
                    output.accept(GOLDEN_REVOLVER.get());
                    output.accept(DIAMOND_BULLET.get());
                    output.accept(DIAMOND_REVOLVER.get());
                })
                .build()
        );
    }

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // We handle this inside displayItems above, but kept this for compatibility if needed.
    }
}
