package org.stargest.nst_revrecoiled.Items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.util.ModItems;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base class for all revolver weapons.
 * Uses crossbow mechanics: charge before shooting.
 * Integrates with GeckoLib for animations.
 */
public abstract class BaseRevolverItem extends RangedWeaponItem implements GeoItem {

    private static final int MAX_DURABILITY = 500;
    private static final int CHARGE_TIME_TICKS = 25; // 1.25 seconds
    private static final float PROJECTILE_VELOCITY = 3.15f;
    private static final float PROJECTILE_DIVERGENCE = 1.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final float baseDamage;

    public BaseRevolverItem(Settings settings, float baseDamage) {
        super(settings.maxDamage(MAX_DURABILITY));
        this.baseDamage = baseDamage;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public boolean isUsedOnRelease(ItemStack stack) {
        return true; // Required for crossbow-style mechanics
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return CHARGE_TIME_TICKS;
    }

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return stack -> stack.getItem() instanceof BaseBulletItem;
    }

    @Override
    public int getRange() {
        return 15;
    }

    /**
     * Called when player right-clicks with the revolver.
     * If charged: shoots. Otherwise: starts charging.
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isCharged(stack)) {
            shoot(world, user, hand, stack, PROJECTILE_VELOCITY, PROJECTILE_DIVERGENCE);
            return ActionResult.SUCCESS;
        }

        ItemStack ammo = user.getProjectileType(stack);
        if (!user.getAbilities().creativeMode && ammo.isEmpty()) {
            return ActionResult.FAIL;
        }

        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    /**
     * Called when player releases right-click or gets interrupted.
     * Loads the bullet if charging completed successfully.
     */
    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        int chargedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float chargeProgress = (float) chargedTicks / CHARGE_TIME_TICKS;

        if (chargeProgress >= 1.0f && !isCharged(stack)) {
            if (loadBullet(user, stack)) {
                world.playSound(
                        null,
                        user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ITEM_CROSSBOW_LOADING_END,
                        SoundCategory.PLAYERS,
                        1.0f, 1.0f
                );
            }
        }

        return false;
    }

    /**
     * Loads a bullet from the user's inventory into the revolver.
     * Uses vanilla ChargedProjectilesComponent for compatibility.
     */
    private boolean loadBullet(LivingEntity shooter, ItemStack revolver) {
        ItemStack ammo = shooter.getProjectileType(revolver);

        if (ammo.isEmpty() && !(shooter instanceof PlayerEntity p && p.getAbilities().creativeMode)) {
            return false;
        }

        List<ItemStack> projectiles = new ArrayList<>();

        if (!ammo.isEmpty()) {
            ItemStack bulletCopy = ammo.copy();
            bulletCopy.setCount(1);
            projectiles.add(bulletCopy);

            if (shooter instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
                ammo.decrement(1);
            }
        } else {
            // Creative mode fallback
            projectiles.add(new ItemStack(ModItems.STONE_BULLET));
        }

        revolver.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(projectiles));
        return true;
    }

    /**
     * Fires the loaded bullet as a projectile entity.
     * Combines revolver base damage with bullet damage.
     */
    private void shoot(World world, LivingEntity shooter, Hand hand, ItemStack stack,
                       float velocity, float divergence) {
        if (world.isClient) {
            return;
        }

        ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (component == null || component.isEmpty()) {
            return;
        }

        ItemStack bulletStack = component.getProjectiles().get(0);

        float bulletDamage = 0.0f;
        if (bulletStack.getItem() instanceof BaseBulletItem bulletItem) {
            bulletDamage = bulletItem.getDamage();
        }

        float totalDamage = this.baseDamage + bulletDamage;

        BulletProjectileEntity projectile = new BulletProjectileEntity(
                world,
                shooter,
                bulletStack,
                totalDamage
        );

        projectile.setVelocity(
                shooter,
                shooter.getPitch(),
                shooter.getYaw(),
                0.0f,
                velocity,
                divergence
        );

        world.spawnEntity(projectile);

        stack.damage(1, shooter, LivingEntity.getSlotForHand(hand));

        world.playSound(
                null,
                shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );

        stack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
    }

    /**
     * Checks if the revolver currently has a loaded bullet.
     */
    public static boolean isCharged(ItemStack stack) {
        ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        return component != null && !component.isEmpty();
    }

    // GeckoLib animation setup

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<?> state) {
        state.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}