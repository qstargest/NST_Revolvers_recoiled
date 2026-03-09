package org.stargest.nst_revrecoiled.client.util;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.render.revolvers.CobblestoneRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.DiamondRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.GoldenRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.IronRevolverItemRenderer;
import org.stargest.nst_revrecoiled.util.ModItems;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

/**
 * Registers GeckoLib renderers for all animated revolver items.
 * Each revolver has its own renderer and model for custom 3D animations.
 * Called during client initialization.
 */
public final class ModItemRenderers {

    private ModItemRenderers() {}

    /**
     * Initializes all item renderers.
     * Binds each revolver item to its corresponding GeckoLib renderer.
     */
    public static void init() {
        bind(((BaseRevolverItem) ModItems.COBBLESTONE_REVOLVER).renderProvider,
                CobblestoneRevolverItemRenderer::new);
        bind(((BaseRevolverItem) ModItems.IRON_REVOLVER).renderProvider,
                IronRevolverItemRenderer::new);
        bind(((BaseRevolverItem) ModItems.GOLDEN_REVOLVER).renderProvider,
                GoldenRevolverItemRenderer::new);
        bind(((BaseRevolverItem) ModItems.DIAMOND_REVOLVER).renderProvider,
                DiamondRevolverItemRenderer::new);
    }

    /**
     * Binds a renderer factory to an item's render provider.
     * Uses lazy initialization — renderer is only created when first needed.
     *
     * @param renderProvider The item's render provider container
     * @param factory Supplier that creates the renderer instance
     */
    private static <R extends GeoItemRenderer<?>> void bind(
            MutableObject<GeoRenderProvider> renderProvider,
            Supplier<R> factory
    ) {
        renderProvider.setValue(new GeoRenderProvider() {
            private R renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = factory.get();
                }
                return renderer;
            }
        });
    }
}
