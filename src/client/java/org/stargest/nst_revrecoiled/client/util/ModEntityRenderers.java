package org.stargest.nst_revrecoiled.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import org.stargest.nst_revrecoiled.util.ModEntities;

/**
 * Registers all custom entity renderers for the mod.
 * Called during client initialization.
 */
public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    /**
     * Registers entity renderers.
     * BulletProjectileEntity uses FlyingItemEntityRenderer to display as the bullet item.
     */
    public static void init() {
        EntityRendererRegistry.register(
                ModEntities.BULLET_PROJECTILE,
                FlyingItemEntityRenderer::new
        );
    }
}
