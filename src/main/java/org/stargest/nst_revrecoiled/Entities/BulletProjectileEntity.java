package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;

public class BulletProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private ItemStack bulletStack = ItemStack.EMPTY;
    private float fixedDamage;

    public BulletProjectileEntity(EntityType<? extends BulletProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public BulletProjectileEntity(World world, LivingEntity owner, ItemStack bulletStack, float damage) {
        // Передаем bulletStack в супер-конструктор, чтобы ванилла тоже знала о предмете
        super(ModEntities.BULLET_PROJECTILE, owner, world, bulletStack, null);
        this.bulletStack = bulletStack.copy();
        this.fixedDamage = damage;
        this.setDamage(damage);
        this.setDamage(0);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        World world = this.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            DamageSource damageSource = this.getDamageSources().arrow(this, owner);

            // Используем именно fixedDamage
            if (target.damage(serverWorld, damageSource, this.fixedDamage)) {
                if (target instanceof LivingEntity livingTarget) {
                    this.onHit(livingTarget);
                }
            }
        }

        this.playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

        if (!world.isClient) {
            this.discard();
        }
    }

    // --- СЮДА СМОТРИТ РЕНДЕРЕР ---
    @Override
    public ItemStack getStack() {
        // Если поле пустое, берем ItemStack из системы данных PersistentProjectileEntity
        return this.bulletStack.isEmpty() ? this.getItemStack() : this.bulletStack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        // Возвращаем любой дефолт, если данных нет совсем
        return new ItemStack(ModItems.STONE_BULLET);
    }

    @Override
    public void tick() {
        super.tick();
        // Пуля исчезает через 10 секунд
        if (this.age >= 200) {
            this.discard();
        }
    }
}
