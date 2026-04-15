package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Items.Bullets.DiamondBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.GoldenBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.IronBulletItem;
import org.stargest.nst_revrecoiled.Items.Bullets.StoneBulletItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for all custom items in the mod.
 * Uses DeferredRegister for clean registration in Forge.
 * Adapted for Forge 1.20.1.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    // --- Bullets ---
    public static final RegistryObject<StoneBulletItem> STONE_BULLET = ITEMS.register(
            "stone_bullet",
            () -> new StoneBulletItem(new Item.Properties().stacksTo(64))
    );
    public static final RegistryObject<IronBulletItem> IRON_BULLET = ITEMS.register(
            "iron_bullet",
            () -> new IronBulletItem(new Item.Properties().stacksTo(64))
    );
    public static final RegistryObject<GoldenBulletItem> GOLDEN_BULLET = ITEMS.register(
            "golden_bullet",
            () -> new GoldenBulletItem(new Item.Properties().stacksTo(64))
    );
    public static final RegistryObject<DiamondBulletItem> DIAMOND_BULLET = ITEMS.register(
            "diamond_bullet",
            () -> new DiamondBulletItem(new Item.Properties().stacksTo(64))
    );

    // --- Revolvers ---
    public static final RegistryObject<CobblestoneRevolverItem> COBBLESTONE_REVOLVER = ITEMS.register(
            "cobblestone_revolver",
            () -> new CobblestoneRevolverItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<IronRevolverItem> IRON_REVOLVER = ITEMS.register(
            "iron_revolver",
            () -> new IronRevolverItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<GoldenRevolverItem> GOLDEN_REVOLVER = ITEMS.register(
            "golden_revolver",
            () -> new GoldenRevolverItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<DiamondRevolverItem> DIAMOND_REVOLVER = ITEMS.register(
            "diamond_revolver",
            () -> new DiamondRevolverItem(new Item.Properties().stacksTo(1))
    );

    // Internal items for 2D GUI icon baking - these ensure [item]_revolver_item.json models are baked.
    // They are not added to creative tabs and remain hidden from players.
    // Uses the registerIcon helper for consistent naming and settings.
    static final RegistryObject<Item> COBBLESTONE_REVOLVER_ICON = registerIcon("cobblestone_revolver");
    static final RegistryObject<Item> IRON_REVOLVER_ICON = registerIcon("iron_revolver");
    static final RegistryObject<Item> GOLDEN_REVOLVER_ICON = registerIcon("golden_revolver");
    static final RegistryObject<Item> DIAMOND_REVOLVER_ICON = registerIcon("diamond_revolver");

    private static RegistryObject<Item> registerIcon(String baseName) {
        return ITEMS.register(baseName + "_item", () -> new Item(new Item.Properties()));
    }

    // --- Creative Tab ---
    public static final ResourceKey<CreativeModeTab> MOD_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            new ResourceLocation(Main.MODID, "main")
    );

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
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

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // We handle this inside displayItems above.
    }
}
