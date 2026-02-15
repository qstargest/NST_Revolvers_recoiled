package org.stargest.nst_revrecoiled.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Items.Revolvers.CobblestoneRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.DiamondRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.GoldenRevolverItem;
import org.stargest.nst_revrecoiled.Items.Revolvers.IronRevolverItem;
import org.stargest.nst_revrecoiled.client.render.revolvers.CobblestoneRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.DiamondRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.GoldenRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.IronRevolverItemRenderer;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Client-side initialization for the mod.
 * Registers entity renderers and GeckoLib item renderers.
 */
public class Nst_revolvers_recoiledClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerEntityRenderers();
        registerItemRenderers();
    }

    /**
     * Registers custom entity renderers.
     */
    private void registerEntityRenderers() {
        EntityRendererRegistry.register(
                ModEntities.BULLET_PROJECTILE,
                FlyingItemEntityRenderer::new
        );
    }

    /**
     * Sets up GeckoLib renderers for animated items.
     */
    private void registerItemRenderers() {
        // Cobblestone Revolver
        CobblestoneRevolverItem cobblestoneRevolverItem = (CobblestoneRevolverItem) ModItems.COBBLESTONE_REVOLVER;
        cobblestoneRevolverItem.renderProvider.setValue(new GeoRenderProvider() {
            private CobblestoneRevolverItemRenderer renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new CobblestoneRevolverItemRenderer();
                }
                return renderer;
            }
        });

        // Iron Revolver
        IronRevolverItem ironRevolverItem = (IronRevolverItem) ModItems.IRON_REVOLVER;
        ironRevolverItem.renderProvider.setValue(new GeoRenderProvider() {
            private IronRevolverItemRenderer renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new IronRevolverItemRenderer();
                }
                return renderer;
            }
        });

        // Diamond Revolver
        GoldenRevolverItem goldenRevolverItem = (GoldenRevolverItem) ModItems.GOLDEN_REVOLVER;
        goldenRevolverItem.renderProvider.setValue(new GeoRenderProvider() {
            private GoldenRevolverItemRenderer renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer(){
                if(renderer == null){
                    renderer = new GoldenRevolverItemRenderer();
                }
                return renderer;
            }
        });

        // Diamond Revolver
        DiamondRevolverItem diamondRevolverItem = (DiamondRevolverItem) ModItems.DIAMOND_REVOLVER;
        diamondRevolverItem.renderProvider.setValue(new GeoRenderProvider() {
            private DiamondRevolverItemRenderer renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer(){
                if(renderer == null){
                    renderer = new DiamondRevolverItemRenderer();
                }
                return renderer;
            }
        });
    }
}