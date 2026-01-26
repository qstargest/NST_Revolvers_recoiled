package org.stargest.nst_revrecoiled.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Items.Revolvers.DioriteRevolverItem;
import org.stargest.nst_revrecoiled.client.render.revolvers.DioriteRevolverItemRenderer;
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
        DioriteRevolverItem revolverItem = (DioriteRevolverItem) ModItems.DIORITE_REVOLVER;

        revolverItem.renderProvider.setValue(new GeoRenderProvider() {
            private DioriteRevolverItemRenderer renderer;

            @Override
            public @NotNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new DioriteRevolverItemRenderer();
                }
                return renderer;
            }
        });
    }
}

