package org.stargest.nst_revrecoiled.Entities;

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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;

import java.util.Optional;

/**
 * Custom bullet projectile entity.
 * Renders as the bullet item using FlyingItemEntityRenderer.
 * Deals fixed damage determined at creation time.
 * Uses DataTracker for proper client-server synchronization.
 */
public class BulletProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {

    private static final int MAX_AGE_TICKS = 20; // 1 second lifetime

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
        super(ModEntities.BULLET_PROJECTILE, owner, world, bulletStack, null);
        this.bulletStack = (bulletStack != null) ? bulletStack.copy() : ItemStack.EMPTY;
        this.fixedDamage = damage;

        // Prevent vanilla damage calculation
        this.setDamage(0.0);

        // Set tracked data so spawn packet contains it
        if (this.dataTracker != null) {
            this.dataTracker.set(DATA_BULLET_STACK, this.bulletStack.copy());
            this.dataTracker.set(DATA_FIXED_DAMAGE, this.fixedDamage);
        }
    }

    /**
     * Initializes data tracker with custom fields.
     * Uses Builder pattern from Minecraft 1.21.
     */
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_BULLET_STACK, ItemStack.EMPTY);
        builder.add(DATA_FIXED_DAMAGE, 0.0f);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.STONE_BULLET);
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

        return this.getDefaultItemStack();
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        World world = this.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            if (target.damage(serverWorld, this.getDamageSources().arrow(this, owner), this.fixedDamage)) {
                if (target instanceof LivingEntity livingTarget) {
                    this.onHit(livingTarget);
                }
            }
        }

        this.playSound(
                SoundEvents.ENTITY_ARROW_HIT,
                1.0f,
                1.2f / (this.random.nextFloat() * 0.2f + 0.9f)
        );

        if (!world.isClient) {
            this.discard();
        }
    }

    /**
     * Writes bullet ItemStack to NBT for world saving.
     * Uses NbtElement because ItemStack.toNbt returns NbtElement in 1.21.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        if (this.bulletStack != null && !this.bulletStack.isEmpty()) {
            RegistryWrapper.WrapperLookup lookup = this.getWorld().getRegistryManager();
            NbtElement element = this.bulletStack.toNbt(lookup);
            nbt.put("BulletItem", element);
        }

        nbt.putFloat("FixedDamage", this.fixedDamage);
    }

    /**
     * Reads bullet ItemStack from NBT when loading from world.
     * Uses ItemStack.fromNbt which returns Optional<ItemStack> in 1.21.
     */
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        if (nbt.contains("BulletItem")) {
            RegistryWrapper.WrapperLookup lookup = this.getWorld().getRegistryManager();
            NbtElement element = nbt.get("BulletItem");

            Optional<ItemStack> maybe = ItemStack.fromNbt(lookup, element);
            this.bulletStack = maybe.orElse(ItemStack.EMPTY);

            // Sync tracked data
            if (this.dataTracker != null) {
                this.dataTracker.set(DATA_BULLET_STACK,
                        this.bulletStack.isEmpty() ? ItemStack.EMPTY : this.bulletStack.copy());
            }
        }

        if (nbt.contains("FixedDamage")) {
            this.fixedDamage = nbt.getFloat("FixedDamage");
            if (this.dataTracker != null) {
                this.dataTracker.set(DATA_FIXED_DAMAGE, this.fixedDamage);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Client-side: update local field from tracked data
        if (this.getWorld().isClient) {
            ItemStack tracked = this.dataTracker.get(DATA_BULLET_STACK);
            if (tracked != null && !tracked.isEmpty()) {
                this.bulletStack = tracked.copy();
            }
        }

        // Remove bullet after max age to prevent entity buildup
        if (this.age >= MAX_AGE_TICKS) {
            this.discard();
        }
    }
}
