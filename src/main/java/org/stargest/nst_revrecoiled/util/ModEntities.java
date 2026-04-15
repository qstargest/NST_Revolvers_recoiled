package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.NstRevRecoiled;

import java.util.function.Supplier;

/**
 * Registry for all custom entities in the mod.
 * Configures entity tracking and synchronization settings.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NstRevRecoiled.MOD_ID);

    public static final Supplier<EntityType<BulletProjectileEntity>> BULLET_PROJECTILE =
            ENTITY_TYPES.register("bullet_projectile",
                    () -> EntityType.Builder.<BulletProjectileEntity>of(
                                    BulletProjectileEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("bullet_projectile")
            );
}
