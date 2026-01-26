package org.stargest.nst_revrecoiled.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.Main;

/**
 * Registry for all custom entities in the mod.
 */
public class ModEntities {

    public static EntityType<BulletProjectileEntity> BULLET_PROJECTILE;

    /**
     * Initializes and registers all custom entities.
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
                        .maxTrackingRange(4)
                        .trackingTickInterval(20)
                        .build(bulletKey)
        );
    }
}
