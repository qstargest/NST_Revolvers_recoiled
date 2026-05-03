package org.stargest.nst_revrecoiled.util;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for all custom entities.json in the mod.
 * Configures entity tracking and synchronization settings.
 */
public class ModEntities {

    public static EntityType<BulletProjectileEntity> BULLET_PROJECTILE;

    /** Revolver-wielding hostile humanoid mob. */
    public static EntityType<RevolverBanditEntity> REVOLVER_BANDIT;

    /**
     * Initializes and registers all custom entities.json.
     *
     * Bullet projectile tracking settings:
     * - maxTrackingRange: 8 blocks (short range for fast-moving projectiles)
     * - trackingTickInterval: 1 (update every tick for smooth client-side rendering)
     * - dimensions: 0.25x0.25 (small hitbox for bullet)
     */
    public static void init() {
        Identifier bulletId = Identifier.of(Main.MOD_ID, "bullet_projectile");
        RegistryKey<EntityType<?>> bulletKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, bulletId);

        BULLET_PROJECTILE = Registry.register(
                Registries.ENTITY_TYPE,
                bulletId,
                EntityType.Builder.<BulletProjectileEntity>create(
                                BulletProjectileEntity::new,
                                SpawnGroup.MISC
                        )
                        .dimensions(0.25f, 0.25f)
                        .maxTrackingRange(8)          // Track up to 8 blocks away
                        .trackingTickInterval(1)      // Update every tick for smooth movement
                        .build()
        );

        Identifier banditId = Identifier.of(Main.MOD_ID, "revolver_bandit");
        RegistryKey<EntityType<?>> banditKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, banditId);

        REVOLVER_BANDIT = Registry.register(
                Registries.ENTITY_TYPE,
                banditId,
                EntityType.Builder.create(
                                RevolverBanditEntity::new,
                                SpawnGroup.MONSTER
                        )
                        .dimensions(0.6f, 1.95f)     // Pillager-like humanoid size
                        .maxTrackingRange(8)
                        .trackingTickInterval(3)
                        .build()
        );

        // Link entity type to its attribute set (Fabric API wrapper)
        FabricDefaultAttributeRegistry.register(REVOLVER_BANDIT, RevolverBanditEntity.createAttributes());
    }
}
