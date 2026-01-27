package org.stargest.nst_revrecoiled.Items;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
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
 * Integrates with GeckoLib for custom animations (fire, reload, draw).
 */
public abstract class BaseRevolverItem extends RangedWeaponItem implements GeoItem {

    private static final int MAX_DURABILITY = 500;
    private static final int CHARGE_TIME_TICKS = 25; // 1.25 seconds
    private static final float PROJECTILE_VELOCITY = 6.0f;
    private static final float PROJECTILE_DIVERGENCE = 1.0f;

    // Animation definitions
    private static final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlay("animation.model.fireright");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("animation.model.reloademptyright");
    private static final RawAnimation DRAW_ANIM = RawAnimation.begin().thenPlay("animation.model.drawright");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    // NBT key for tracking draw animation state
    private static final String DRAW_PLAYED_KEY = "DrawAnimPlayed";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final float baseDamage;

    public BaseRevolverItem(Settings settings, float baseDamage) {
        super(settings.maxDamage(MAX_DURABILITY));
        this.baseDamage = baseDamage;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        // NONE prevents eating/drinking animation during charging
        return UseAction.NONE;
    }

    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        // Prevent item switch animation when charge state changes
        return false;
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        // Prevent vanilla hand swing animation
        return false;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        // Prevent vanilla swing animation when breaking blocks
        return false;
    }

    @Override
    public boolean isUsedOnRelease(ItemStack stack) {
        return true;
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
     * If charged: shoots. Otherwise: starts charging and plays reload animation.
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isCharged(stack)) {
            // Prevent hand swing on client side
            if (world.isClient) {
                user.stopUsingItem();
                return ActionResult.SUCCESS;
            }

            // Shoot only on server
            shoot(world, user, hand, stack, PROJECTILE_VELOCITY, PROJECTILE_DIVERGENCE);
            return ActionResult.CONSUME;
        }

        ItemStack ammo = user.getProjectileType(stack);
        if (!user.getAbilities().creativeMode && ammo.isEmpty()) {
            return ActionResult.FAIL;
        }

        // Trigger reload animation when starting to charge
        if (!world.isClient && user instanceof PlayerEntity player) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(player, instanceId, "controller", "animation.model.reloademptyright");
        }

        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // Called every tick while item is being used
        // Override to prevent vanilla behavior
        super.usageTick(world, user, stack, remainingUseTicks);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        // Called when use duration completes
        return stack;
    }

    /**
     * Called when player releases right-click or gets interrupted.
     * Loads the bullet if charging completed successfully.
     * Stops reload animation if player releases early.
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
        } else {
            // Stop reload animation immediately when player releases before full charge
            if (!world.isClient && user instanceof PlayerEntity player) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
                triggerAnim(player, instanceId, "controller", "stop");
            }
        }

        return false;
    }

    /**
     * Called every tick while item is in inventory.
     * Triggers draw animation when item is first selected.
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!world.isClient && selected && entity instanceof PlayerEntity player) {
            // Trigger draw animation only once when first selected
            if (!hasDrawAnimationPlayed(stack)) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
                triggerAnim(player, instanceId, "controller", "animation.model.drawright");
                markDrawAnimationPlayed(stack);
            }
        }

        // Reset draw animation flag when item is no longer selected
        if (!selected) {
            clearDrawAnimationFlag(stack);
        }
    }

    /**
     * Checks if draw animation has already been played for this item stack.
     */
    private boolean hasDrawAnimationPlayed(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getBoolean(DRAW_PLAYED_KEY);
    }

    /**
     * Marks that draw animation has been played for this item stack.
     */
    private void markDrawAnimationPlayed(ItemStack stack) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, nbt -> {
            NbtCompound compound = nbt.copyNbt();
            compound.putBoolean(DRAW_PLAYED_KEY, true);
            return NbtComponent.of(compound);
        });
    }

    /**
     * Clears the draw animation flag from this item stack.
     */
    private void clearDrawAnimationFlag(ItemStack stack) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, nbt -> {
            NbtCompound compound = nbt.copyNbt();
            compound.remove(DRAW_PLAYED_KEY);
            return NbtComponent.of(compound);
        });
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
     * Triggers fire animation and plays shoot sound.
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
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS,
                0.35f,
                1.5f / (world.getRandom().nextFloat() * 0.4f + 0.8f)
        );

        // Trigger fire animation
        if (shooter instanceof PlayerEntity player) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(player, instanceId, "controller", "animation.model.fireright");
        }

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
        // Controller for triggered animations (fire, reload, draw)
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.STOP)
                .triggerableAnim("animation.model.fireright", FIRE_ANIM)
                .triggerableAnim("animation.model.reloademptyright", RELOAD_ANIM)
                .triggerableAnim("animation.model.drawright", DRAW_ANIM)
                .triggerableAnim("stop", RawAnimation.begin().thenPlay("animation.model.idle")));

        // Separate controller for idle animation
        controllers.add(new AnimationController<>(this, "idle_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("idle", IDLE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
