package org.stargest.nst_revrecoiled.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Main;

import java.util.function.Supplier;

/**
 * Registry for all custom entities in the mod.
 * Configures entity tracking and synchronization settings.
 * Adapted for Forge 1.20.1.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Main.MODID);

    public static final RegistryObject<EntityType<BulletProjectileEntity>> BULLET_PROJECTILE =
            ENTITY_TYPES.register("bullet_projectile",
                    () -> EntityType.Builder.<BulletProjectileEntity>of(
                                    BulletProjectileEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("bullet_projectile")
            );
    public static final Supplier<EntityType<RevolverBanditEntity>> REVOLVER_BANDIT =
            ENTITY_TYPES.register("revolver_bandit",
                    () -> EntityType.Builder.of(
                                    RevolverBanditEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build("revolver_bandit")
            );
}
