package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModItems;

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
public class BulletProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {

    // Tracked data for client-server synchronization
    private static final TrackedData<ItemStack> DATA_BULLET_STACK =
            DataTracker.registerData(BulletProjectileEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Float> DATA_FIXED_DAMAGE =
            DataTracker.registerData(BulletProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private ItemStack bulletStack = ItemStack.EMPTY;
    private float fixedDamage;

    /**
     * Constructor for deserialization (called by Minecraft on client side).
     */
    public BulletProjectileEntity(EntityType<? extends BulletProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Server-side constructor used when spawning bullets from revolvers.
     *
     * @param world The world to spawn in
     * @param owner The entity that fired the bullet
     * @param bulletStack The bullet item being fired
     * @param damage Total damage (revolver base + bullet damage)
     */
    public BulletProjectileEntity(World world, LivingEntity owner, ItemStack bulletStack, float damage) {
        super(ModEntities.BULLET_PROJECTILE, owner, world);
        this.bulletStack = bulletStack != null ? bulletStack.copy() : ItemStack.EMPTY;
        this.fixedDamage = damage;
        this.setDamage(0.0);
        this.dataTracker.set(DATA_BULLET_STACK, this.bulletStack.copy());
        this.dataTracker.set(DATA_FIXED_DAMAGE, this.fixedDamage);
    }

    /**
     * Initializes data tracker with custom fields.
     */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DATA_BULLET_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(DATA_FIXED_DAMAGE, 0.0f);
    }

    @Override
    public ItemStack asItemStack() {
        return this.getStack();
    }

    /**
     * Returns the item stack for rendering.
     * FlyingItemEntityRenderer uses this to display the bullet.
     * Prioritizes tracked data over local field for client-side accuracy.
     */
    @Override
    public ItemStack getStack() {
        ItemStack tracked = this.dataTracker.get(DATA_BULLET_STACK);
        if (tracked != null && !tracked.isEmpty()) {
            return tracked;
        }

        if (this.bulletStack != null && !this.bulletStack.isEmpty()) {
            return this.bulletStack;
        }

        return new ItemStack(ModItems.STONE_BULLET);
    }

    /**
     * Called when bullet hits an entity.
     * Applies fixed damage and spawns impact particles.
     */
    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        World world = this.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            boolean damaged = target.damage(this.getDamageSources().arrow(this, owner), this.fixedDamage);

            if (damaged && target instanceof LivingEntity livingTarget) {
                this.onHit(livingTarget);
            }

            spawnImpactParticles(serverWorld, this.getPos());
        }

        this.playSound(
                SoundEvents.ENTITY_ARROW_HIT,
                1.0f,
                1.2f / (this.random.nextFloat() * 0.2f + 0.9f)
        );

        this.discard();
    }

    /**
     * Called when bullet hits a block.
     * Spawns impact particles and removes bullet.
     * If the block is a glass pane — breaks it.
     */
    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        World world = this.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);

            // Check if the hit block is any glass pane
            if (blockState.getBlock() instanceof PaneBlock) {
                world.playSound(
                        null,
                        blockPos,
                        SoundEvents.BLOCK_GLASS_BREAK,
                        SoundCategory.BLOCKS,
                        1.0f,
                        0.9f + this.random.nextFloat() * 0.2f
                );


                world.playSound(
                        null,
                        blockPos,
                        SoundEvents.ENTITY_ARROW_HIT,
                        SoundCategory.BLOCKS,
                        1.0f,
                        1.2f + this.random.nextFloat() * 0.2f + 0.9f
                );

                world.breakBlock(blockPos, false, this.getOwner());
                spawnImpactParticles(serverWorld, hitResult.getPos());
                this.discard();
                return;
            }

            spawnImpactParticles(serverWorld, hitResult.getPos());
        }

        super.onBlockHit(hitResult);
        this.discard();
    }

    /**
     * Spawns item-break particles at impact point.
     * ServerWorld.spawnParticles sends packet to all nearby clients.
     *
     * @param world Server world
     * @param pos Impact position
     */
    protected void spawnImpactParticles(ServerWorld world, Vec3d pos) {
        ItemStack displayStack = this.getStack();

        world.spawnParticles(
                new ItemStackParticleEffect(ParticleTypes.ITEM, displayStack),
                pos.x, pos.y, pos.z,
                12,           // Particle count
                0.1, 0.1, 0.1, // Spread in X Y Z
                0.15          // Velocity
        );
    }

    /**
     * Writes bullet ItemStack to NBT for world saving.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (!this.bulletStack.isEmpty()) {
            nbt.put("BulletItem", this.bulletStack.writeNbt(new NbtCompound()));
        }
        nbt.putFloat("FixedDamage", this.fixedDamage);
    }

    /**
     * Reads bullet ItemStack from NBT when loading from world.
     */
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("BulletItem", NbtElement.COMPOUND_TYPE)) {
            this.bulletStack = ItemStack.fromNbt(nbt.getCompound("BulletItem"));
            this.dataTracker.set(DATA_BULLET_STACK,
                    this.bulletStack.isEmpty() ? ItemStack.EMPTY : this.bulletStack.copy());
        }
        if (nbt.contains("FixedDamage")) {
            this.fixedDamage = nbt.getFloat("FixedDamage");
            this.dataTracker.set(DATA_FIXED_DAMAGE, this.fixedDamage);
        }
    }

    /**
     * Tick method with client-server split for authoritative physics.
     * Client: Only updates previous position for render interpolation.
     * Server: Full physics simulation and age tracking.
     */
    @Override
    public void tick() {
        if (this.getWorld().isClient) {
            // Client-side: only track previous position for smooth rendering
            this.prevX = this.getX();
            this.prevY = this.getY();
            this.prevZ = this.getZ();

            // Update local field from DataTracker
            ItemStack tracked = this.dataTracker.get(DATA_BULLET_STACK);
            if (tracked != null && !tracked.isEmpty()) {
                this.bulletStack = tracked.copy();
            }

            this.age++;
            // No physics on client - server handles all movement
        } else {
            // Server-side: full physics simulation
            super.tick();

            float targetGravity = ModConfig.get().bullets.gravity;
            float defaultGravity = 0.05f;
            this.setVelocity(
                    this.getVelocity().add(0, defaultGravity - targetGravity, 0)
            );
            // Auto-despawn after max age
            if (this.age >= ModConfig.get().bullets.maxAgeTicks) {
                this.discard();
            }
        }
    }
}
