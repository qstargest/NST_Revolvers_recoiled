package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;

/**
 * Custom bullet projectile entity.
 * Renders as the bullet item using FlyingItemEntityRenderer.
 * Deals fixed damage determined at creation time.
 */
public class BulletProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {

    private static final int MAX_AGE_TICKS = 200; // 10 seconds

    private ItemStack bulletStack = ItemStack.EMPTY;
    private float fixedDamage;

    /**
     * Constructor for deserialization (called by Minecraft internals).
     */
    public BulletProjectileEntity(EntityType<? extends BulletProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Main constructor used when spawning bullets from revolvers.
     *
     * @param world The world to spawn in
     * @param owner The entity that fired the bullet
     * @param bulletStack The bullet item being fired
     * @param damage Total damage (revolver base + bullet damage)
     */
    public BulletProjectileEntity(World world, LivingEntity owner, ItemStack bulletStack, float damage) {
        super(ModEntities.BULLET_PROJECTILE, owner, world, bulletStack, null);
        this.bulletStack = bulletStack.copy();
        this.fixedDamage = damage;

        // Set damage to 0 in parent to prevent vanilla damage calculation
        this.setDamage(0);
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
     * Returns the item stack for rendering.
     * FlyingItemEntityRenderer uses this to display the bullet.
     */
    @Override
    public ItemStack getStack() {
        return this.bulletStack.isEmpty() ? this.getItemStack() : this.bulletStack;
    }

    /**
     * Fallback item stack if no bullet data is available.
     */
    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.STONE_BULLET);
    }

    @Override
    public void tick() {
        super.tick();

        // Remove bullet after max age to prevent entity buildup
        if (this.age >= MAX_AGE_TICKS) {
            this.discard();
        }
    }
}
