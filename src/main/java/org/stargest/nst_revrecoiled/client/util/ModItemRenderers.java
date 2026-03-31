package org.stargest.nst_revrecoiled.client.util;

import net.minecraft.item.Item;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.render.revolvers.CobblestoneRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.DiamondRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.GoldenRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.IronRevolverItemRenderer;
import org.stargest.nst_revrecoiled.util.ModItems;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

/**
 * Registers GeckoLib renderers for all animated revolver items.
 * Each revolver has its own renderer and model for custom 3D animations.
 * Called during client initialization.
 */
public final class ModItemRenderers {

    private ModItemRenderers() {}

    public static void init() {
        bind(ModItems.COBBLESTONE_REVOLVER, CobblestoneRevolverItemRenderer::new);
        bind(ModItems.IRON_REVOLVER,        IronRevolverItemRenderer::new);
        bind(ModItems.GOLDEN_REVOLVER,      GoldenRevolverItemRenderer::new);
        bind(ModItems.DIAMOND_REVOLVER,     DiamondRevolverItemRenderer::new);
    }

    private static <R extends GeoItemRenderer<?>> void bind(Item item, Supplier<R> factory) {
        ((BaseRevolverItem) item).setRendererFactory(factory::get);
    }
}
