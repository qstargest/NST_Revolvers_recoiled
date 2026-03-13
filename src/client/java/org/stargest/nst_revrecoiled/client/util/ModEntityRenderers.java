package org.stargest.nst_revrecoiled.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.EntityType;
import org.stargest.nst_revrecoiled.client.render.entity.villager.ModVillagerRenderer;
import org.stargest.nst_revrecoiled.util.ModEntities;

/**
 * Registers all custom entity renderers for the mod.
 * Called during client initialization from Nst_revolvers_recoiledClient.
 *
 * Registered renderers:
 * - BulletProjectileEntity — uses FlyingItemEntityRenderer to display the bullet as its item stack
 * - VillagerEntity — replaced with ModVillagerRenderer to support the revolvermaker profession
 *   (custom texture selection and clothing layer suppression)
 *
 * Villager renderer registration overrides the vanilla renderer globally,
 * so ModVillagerRenderer delegates back to super for all non-revolvermaker villagers
 * to preserve vanilla appearance and biome variant behavior.
 */
public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    /**
     * Registers entity renderers with the EntityRendererRegistry.
     * Must be called on the client thread during client initialization.
     */
    public static void init() {
        // Renders bullet projectiles as their carried item stack using vanilla flying item logic
        EntityRendererRegistry.register(
                ModEntities.BULLET_PROJECTILE,
                FlyingItemEntityRenderer::new
        );

        // Overrides the vanilla villager renderer to support the revolvermaker profession —
        // custom texture and suppressed clothing overlay for revolvermakers,
        // full vanilla fallback for all other professions
        EntityRendererRegistry.register(EntityType.VILLAGER, ModVillagerRenderer::new);
    }
}
