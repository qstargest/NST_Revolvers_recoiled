package org.stargest.nst_revrecoiled.Items;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.util.ModItems;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Base class for all revolver weapons.
 * Uses crossbow-style mechanics: charge before shooting.
 * Integrates with GeckoLib for custom animations (fire, reload, draw).
 * Includes perspective-aware animation handling to suppress certain animations in third-person view.
 *
 * Key features:
 * - Charge-based shooting system (hold to reload, release to fire)
 * - GeckoLib animations with particle effects
 * - Perspective-aware rendering (different animations for 1st/3rd person)
 * - Draw animation when equipping
 * - Prevents vanilla animations (hand swing, item switch)
 * - Immediate fire particles (bypasses GeckoLib animation delay)
 * - Ballistic projectiles with gravity (spawns from calculated barrel position)
 * - Networked particle synchronization (all nearby players see fire particles)
 */
public abstract class BaseRevolverItem extends RangedWeaponItem implements GeoItem {

    private static final int MAX_DURABILITY = 500;
    private static final int CHARGE_TIME_TICKS = 50; // 2.5 seconds at 20 TPS
    private static final float PROJECTILE_VELOCITY = 6.0f;
    private static final float PROJECTILE_DIVERGENCE = 0.2f; // Reduced for better accuracy

    // Barrel position offsets — mirror TP_FIRE_* constants in RevolverParticleHandler
    private static final double BARREL_SHOULDER = 0.35; // Body-right offset to shoulder
    private static final double BARREL_FWD      = 1.0;  // Forward from shoulder
    private static final double BARREL_RIGHT    = -0.20;
    private static final double BARREL_UP       = 0.05;

    // Animation definitions
    private static final RawAnimation FIRE_ANIM   = RawAnimation.begin().thenPlay("animation.model.fireright");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("animation.model.reloademptyright");
    private static final RawAnimation DRAW_ANIM   = RawAnimation.begin().thenPlay("animation.model.drawright");
    private static final RawAnimation IDLE_ANIM   = RawAnimation.begin().thenLoop("idle");

    // NBT key for tracking draw animation state
    private static final String DRAW_PLAYED_KEY = "DrawAnimPlayed";

    // GeckoLib — one cache per concrete item type
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * Client-only renderer container. Populated during client initialization.
     * Accessed by ModItemRenderers to bind the GeckoLib renderer for this item.
     */
    public final MutableObject<GeoRenderProvider> renderProvider = new MutableObject<>();

    private final float baseDamage;

    /**
     * Particle handler for animation keyframes.
     * Set during client initialization to handle reload particle spawning.
     * Fire particles use immediate callback instead to avoid animation delay.
     */
    private static Consumer<ParticleKeyframeEvent<BaseRevolverItem>> particleKeyframeHandler = event -> {};

    /**
     * Client-side fire callback for immediate particle spawning.
     * Called directly from use() method to bypass GeckoLib animation delay.
     * This ensures fire particles appear exactly when the shot is fired.
     * Protected by IllegalStateException to prevent accidental double-initialization.
     */
    private static Consumer<LivingEntity> clientFireCallback = null;

    /**
     * Sets the immediate fire callback.
     * Called during client initialization.
     *
     * @param callback Consumer invoked on the client when the revolver is fired
     * @throws IllegalStateException if callback is already set
     */
    public static void setClientFireCallback(Consumer<LivingEntity> callback) {
        if (clientFireCallback != null) {
            throw new IllegalStateException("clientFireCallback is already defined!");
        }
        clientFireCallback = callback;
    }

    public BaseRevolverItem(Settings settings, float baseDamage) {
        super(settings.maxDamage(MAX_DURABILITY));
        this.baseDamage = baseDamage;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // -------------------------------------------------------------------------
    // GeoItem — renderer binding
    // -------------------------------------------------------------------------

    /**
     * GeckoLib calls this on both client and server.
     * Only the client-side renderer is set, so server-side this is a no-op.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        GeoRenderProvider provider = renderProvider.getValue();
        if (provider != null) {
            consumer.accept(provider);
        }
    }

    // -------------------------------------------------------------------------
    // RangedWeaponItem — required abstract method (unused; logic is in shoot())
    // -------------------------------------------------------------------------

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index,
                         float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        // Not used — custom shooting logic is in the private shoot() method below
    }

    // -------------------------------------------------------------------------
    // Item overrides
    // -------------------------------------------------------------------------

    @Override
    public UseAction getUseAction(ItemStack stack) {
        // NONE prevents vanilla eating/drinking animation during charging
        return UseAction.NONE;
    }

    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        // Prevents item switch animation when only charge state changes
        return false;
    }

    @Override
    public boolean allowContinuingBlockBreaking(PlayerEntity player, ItemStack oldStack, ItemStack newStack) {
        // Prevents interruption when item data changes
        return true;
    }

    @Override
    public boolean isPerspectiveAware() {
        // Enables perspective-aware rendering for GeckoLib
        return true;
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

    public float getDamage() {
        return baseDamage;
    }

    /**
     * Called when player right-clicks with the revolver.
     * If charged: shoots, triggers immediate fire particles, and broadcasts to nearby players.
     * Otherwise: starts charging and plays reload animation.
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isCharged(stack)) {
            // Shoot only on server
            if (!world.isClient) {
                shoot(world, user, hand, stack, PROJECTILE_VELOCITY, PROJECTILE_DIVERGENCE);
            }

            // Immediate fire particles on client (bypasses GeckoLib animation delay)
            if (world.isClient && clientFireCallback != null) {
                clientFireCallback.accept(user);
            }

            // Server-side: broadcast fire particles to all tracking players
            if (!world.isClient()) {
                RevolverFireParticlePacket packet = new RevolverFireParticlePacket(user.getId());
                PlayerLookup.tracking(user).forEach(p -> ServerPlayNetworking.send(p, packet));
            }

            // PASS prevents vanilla hand swing animation
            return ActionResult.PASS;
        }

        ItemStack ammo = user.getProjectileType(stack);
        if (!user.getAbilities().creativeMode && ammo.isEmpty()) {
            return ActionResult.FAIL;
        }

        // Trigger reload animation when starting to charge
        if (!world.isClient) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(user, instanceId, "controller", "animation.model.reloademptyright");
        }

        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
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
            // Stop reload animation when player releases before full charge
            if (!world.isClient && user instanceof PlayerEntity player) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
                triggerAnim(player, instanceId, "controller", "idle");
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

    // -------------------------------------------------------------------------
    // Draw animation flag helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Bullet loading and shooting
    // -------------------------------------------------------------------------

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
     * Calculates the barrel tip position server-side,
     * mirroring the third-person fire particle offset in RevolverParticleHandler.
     * This ensures bullets spawn from the visually correct position.
     * Uses constants that match RevolverParticleHandler's TP_FIRE_* offsets.
     */
    private static Vec3d calcBarrelPosition(LivingEntity shooter) {
        float pitch   = shooter.getPitch();
        float yaw     = shooter.getYaw();
        float bodyYaw = shooter.getBodyYaw();

        double baseX = shooter.getX();
        double baseY = shooter.getY() + 1.45; // Approximate shoulder height
        double baseZ = shooter.getZ();

        Vec3d bodyRight = Vec3d.fromPolar(0, bodyYaw + 90).normalize();
        Vec3d lookFwd   = Vec3d.fromPolar(pitch, yaw);
        Vec3d lookUp    = Vec3d.fromPolar(pitch - 90, yaw).normalize();
        Vec3d lookRight = lookFwd.crossProduct(lookUp).normalize();

        Vec3d shoulder = new Vec3d(
                baseX + bodyRight.x * BARREL_SHOULDER,
                baseY,
                baseZ + bodyRight.z * BARREL_SHOULDER
        );

        return shoulder
                .add(lookFwd.multiply(BARREL_FWD))
                .add(lookRight.multiply(BARREL_RIGHT))
                .add(lookUp.multiply(BARREL_UP));
    }

    /**
     * Fires the loaded bullet as a projectile entity.
     * Combines revolver base damage with bullet damage.
     * Spawns projectile from calculated barrel position for visual accuracy.
     * Triggers fire animation and plays explosion sound.
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

        // Set spawn position to barrel tip for visual accuracy
        Vec3d barrelPos = calcBarrelPosition(shooter);
        projectile.setPosition(barrelPos.x, barrelPos.y, barrelPos.z);

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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Checks if the revolver currently has a loaded bullet.
     */
    public static boolean isCharged(ItemStack stack) {
        ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        return component != null && !component.isEmpty();
    }

    /**
     * Sets the particle keyframe handler for GeckoLib animations.
     * Called during client initialization.
     *
     * @param handler Consumer that handles particle keyframe events
     */
    public static void setParticleKeyframeHandler(Consumer<ParticleKeyframeEvent<BaseRevolverItem>> handler) {
        particleKeyframeHandler = handler;
    }

    // -------------------------------------------------------------------------
    // GeckoLib animation setup
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<BaseRevolverItem> controller = new AnimationController<>(this, "controller", 5, state -> {
            // Get render perspective (may be null in some contexts)
            ModelTransformationMode perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            if (perspective == null) {
                return PlayState.CONTINUE;
            }

            // Check if rendering in first-person view
            boolean isFirstPerson = perspective == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
                    || perspective == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND;

            AnimationController<?> ctrl = state.getController();

            // In third-person: suppress draw animation by replacing with idle
            if (!isFirstPerson) {
                RawAnimation triggered = ctrl.getTriggeredAnimation();

                if (triggered != null && DRAW_ANIM.equals(triggered)) {
                    ctrl.setAnimation(IDLE_ANIM);
                    return PlayState.CONTINUE;
                }
            }

            return PlayState.CONTINUE;
        });

        // Set particle handler for animation keyframe events
        controller.setParticleKeyframeHandler(event -> particleKeyframeHandler.accept(event));

        controller
                .receiveTriggeredAnimations() // Required for predicate to be called during triggers
                .triggerableAnim("animation.model.fireright", FIRE_ANIM)
                .triggerableAnim("animation.model.reloademptyright", RELOAD_ANIM)
                .triggerableAnim("animation.model.drawright", DRAW_ANIM)
                .triggerableAnim("idle", IDLE_ANIM);

        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
