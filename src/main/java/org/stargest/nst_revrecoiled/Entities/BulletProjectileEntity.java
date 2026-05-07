package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;

import java.util.Objects;
import java.util.Optional;

/**
 * Custom bullet projectile entity with ballistic physics.
 * Renders as the bullet item using FlyingItemEntityRenderer.
 * Deals fixed damage determined at creation time.
 * Uses DataTracker for proper client-server synchronization.
 *
 * Key features:
 * - Ballistic trajectory with gravity (0.15 per tick)
 * - ~30 block effective range on horizontal shots
 * - Server-authoritative physics (client only renders)
 * - Impact particles on hit
 * - Auto-despawn after 1 second or on impact
 */
public class BulletProjectileEntity extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DATA_BULLET_STACK =
            SynchedEntityData.defineId(BulletProjectileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_FIXED_DAMAGE =
            SynchedEntityData.defineId(BulletProjectileEntity.class, EntityDataSerializers.FLOAT);

    private ItemStack bulletStack = ItemStack.EMPTY;
    private float fixedDamage;

    /**
     * Constructor for deserialization (called by Minecraft on client side).
     */
    public BulletProjectileEntity(EntityType<? extends BulletProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Server-side constructor used when spawning bullets from revolvers.
     *
     * @param level The world to spawn in
     * @param owner The entity that fired the bullet
     * @param bulletStack The bullet item being fired
     * @param damage Total damage (revolver base + bullet damage)
     */
    public BulletProjectileEntity(Level level, LivingEntity owner, ItemStack bulletStack, float damage) {
        super(ModEntities.BULLET_PROJECTILE.get(), owner, level, bulletStack, null);
        this.bulletStack = (bulletStack != null) ? bulletStack.copy() : ItemStack.EMPTY;
        this.fixedDamage = damage;

        this.setBaseDamage(0.0);

        this.entityData.set(DATA_BULLET_STACK, this.bulletStack.copy());
        this.entityData.set(DATA_FIXED_DAMAGE, this.fixedDamage);
    }

    /**
     * Gravity applied per tick for ballistic trajectory.
     * 0.15 gives ~30 blocks effective range on horizontal shots.
     * Angled shots naturally travel farther due to ballistic arc.
     */
    @Override
    protected double getDefaultGravity() {
        return ModConfig.get().bullets.gravity;
    }

    /**
     * Initializes data tracker with custom fields.
     * Uses Builder pattern from Minecraft 1.21.
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BULLET_STACK, ItemStack.EMPTY);
        builder.define(DATA_FIXED_DAMAGE, 0.0f);
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.STONE_BULLET.get());
    }

    /**
     * Returns the item stack for rendering.
     * FlyingItemEntityRenderer uses this to display the bullet.
     * Prioritizes tracked data over local field for client-side accuracy.
     */
    @Override
    public @NotNull ItemStack getItem() {
        ItemStack tracked = this.entityData.get(DATA_BULLET_STACK);
        if (tracked != null && !tracked.isEmpty()) {
            return tracked;
        }
        if (this.bulletStack != null && !this.bulletStack.isEmpty()) {
            return this.bulletStack;
        }
        return this.getDefaultPickupItem();
    }

    /**
     * Called when bullet hits an entity.
     * Applies fixed damage and spawns impact particles.
     */
    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        Level level = this.level();

        if (level instanceof ServerLevel serverLevel) {
            if (target.hurt(this.damageSources().arrow(this, owner), this.fixedDamage)) {
                if (target instanceof LivingEntity livingTarget) {
                    this.doPostHurtEffects(livingTarget); // equivalent of onHit
                }
            }

            spawnImpactParticles(serverLevel, this.position());
        }

        this.playSound(
                SoundEvents.ARROW_HIT,
                1.0f,
                1.2f / (this.random.nextFloat() * 0.2f + 0.9f)
        );

        this.discard();
    }

    /**
     * Called when bullet hits a block.
     * Spawns impact particles and removes bullet.
     */
    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        Level level = this.level();

        if (level instanceof ServerLevel serverLevel) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = level.getBlockState(blockPos);

            if (blockState.getBlock() instanceof IronBarsBlock && blockState.getSoundType() == SoundType.GLASS){
                level.playSound(null,
                        blockPos,
                        SoundEvents.GLASS_BREAK,
                        SoundSource.BLOCKS,
                        1.0f,
                        0.9f + this.random.nextFloat() * 0.2f);

                level.destroyBlock(blockPos, false, this.getOwner());
                spawnImpactParticles(serverLevel, hitResult.getLocation());
                this.discard();
                return;
            }

            spawnImpactParticles(serverLevel, hitResult.getLocation());
        }

        super.onHitBlock(hitResult);
        this.discard();
    }

    /**
     * Spawns item-break particles at impact point.
     * ServerWorld.spawnParticles sends packet to all nearby clients.
     *
     * @param level Server world
     * @param pos Impact position
     */
    protected void spawnImpactParticles(ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
        ItemStack displayStack = this.getItem();

        level.sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, displayStack),
                pos.x, pos.y, pos.z,
                12,
                0.1, 0.1, 0.1,
                0.15
        );
    }

    /**
     * Writes bullet ItemStack to NBT for world saving.
     * Uses NbtElement because ItemStack.toNbt returns NbtElement in 1.21.
     */
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (this.bulletStack != null && !this.bulletStack.isEmpty()) {
            HolderLookup.Provider lookup = this.level().registryAccess();
            Tag element = this.bulletStack.save(lookup, new CompoundTag());
            tag.put("BulletStack", element);
        }

        tag.putFloat("FixedDamage", this.fixedDamage);
    }

    /**
     * Reads bullet ItemStack from NBT when loading from world.
     * Uses ItemStack.fromNbt which returns Optional<ItemStack> in 1.21.
     */
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("BulletStack")) {
            HolderLookup.Provider lookup = this.level().registryAccess();
            Optional<ItemStack> parsed = ItemStack.parse(lookup, Objects.requireNonNull(tag.get("BulletStack")));
            if (parsed.isPresent()) {
                this.bulletStack = parsed.get();
                this.entityData.set(DATA_BULLET_STACK, this.bulletStack);
            }
        }

        if (tag.contains("FixedDamage")) {
            this.fixedDamage = tag.getFloat("FixedDamage");
            this.entityData.set(DATA_FIXED_DAMAGE, this.fixedDamage);
        }
    }

    /**
     * Tick method with client-server split for authoritative physics.
     * Client: Only updates previous position for render interpolation.
     * Server: Full physics simulation and age tracking.
     */
    @Override
    public void tick() {
        if (this.level().isClientSide) {
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();

            ItemStack tracked = this.entityData.get(DATA_BULLET_STACK);
            if (tracked != null && !tracked.isEmpty()) {
                this.bulletStack = tracked.copy();
            }

            this.tickCount++;
        } else {
            super.tick();

            if (this.tickCount >= ModConfig.get().bullets.maxAgeTicks) {
                this.discard();
            }
        }
    }
}
