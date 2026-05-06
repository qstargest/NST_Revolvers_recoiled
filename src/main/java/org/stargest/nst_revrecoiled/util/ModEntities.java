package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
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
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "bullet_projectile")))
            );

    public static final Supplier<EntityType<RevolverBanditEntity>> REVOLVER_BANDIT =
            ENTITY_TYPES.register("revolver_bandit",
                    () -> EntityType.Builder.<RevolverBanditEntity>of(
                                    RevolverBanditEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(NstRevRecoiled.MOD_ID, "revolver_bandit")))
            );
}
