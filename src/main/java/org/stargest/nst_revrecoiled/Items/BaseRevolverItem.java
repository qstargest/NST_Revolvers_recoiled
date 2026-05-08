package org.stargest.nst_revrecoiled.Items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Base class for all revolver weapons.
 * Uses crossbow-style mechanics: charge before shooting.
 * Integrates with GeckoLib for custom animations (fire, reload, draw).
 * Includes perspective-aware animation handling to suppress certain animations
 * in third-person view.
 *
 * Key features:
 * - Charge-based shooting system (hold to reload, release to fire)
 * - GeckoLib animations (fire, reload, draw, idle)
 * - Perspective-aware rendering (draw animation suppressed in third-person)
 * - Draw animation + sound when equipping
 * - Prevents vanilla animations (hand swing, item switch)
 * - Ballistic projectiles with gravity (spawned from calculated barrel
 * position)
 * - Delayed firing mechanics for synchronization with animations
 * - Raycast-corrected aim: projectile trajectory converges on the crosshair
 * target
 * - Per-item recoil callbacks (extensible for addon mods)
 *
 * Addon mods can register custom recoil behavior per item via
 * registerRecoilCallback(),
 * called during client initialization.
 * Core methods (loadBullet, calcBarrelPosition, performShoot, draw animation
 * helpers)
 * are protected to allow subclasses to override shooting and animation
 * behavior.
 */
public abstract class BaseRevolverItem extends ProjectileWeaponItem implements GeoItem, RevolverArmPoseItem {

    // Default barrel offsets for mob (3rd-person) shooting
    private static final double BARREL_SHOULDER = 0.35;
    private static final double BARREL_FWD = 1.0;
    private static final double BARREL_RIGHT = -0.20;
    private static final double BARREL_UP = 0.05;

    /**
     * Ticks to wait between triggering the fire animation and spawning the
     * projectile.
     */
    public static final int SHOOT_DELAY_TICKS = 3;

    /**
     * Tracks the remaining delay ticks before a scheduled shot is actually fired.
     */
    private static final Map<UUID, Integer> PENDING_SHOTS = new ConcurrentHashMap<>();

    /**
     * Stores bullets that have been fired but are waiting for their delayed
     * release.
     */
    private static final Map<UUID, ItemStack> PENDING_BULLETS = new ConcurrentHashMap<>();

    private static final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlay("fire");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation DRAW_ANIM = RawAnimation.begin().thenPlay("draw");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private static final int USE_DURATION = 72000;

    private static final String DRAW_PLAYED_KEY = "DrawAnimPlayed";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final float fallbackDamage;
    private final int fallbackDurability;

    public int getChargeTimeTicks() {
        return ModConfig.get().revolvers.chargeTimeTicks;
    }

    public float getProjectileVelocity() {
        return ModConfig.get().revolvers.projectileVelocity;
    }

    public float getProjectileDivergence() {
        return ModConfig.get().revolvers.projectileDivergence;
    }

    /**
     * Gets the number of ticks to delay the projectile spawn after firing.
     * Allows custom revolvers to synchronize projectile spawning with firing
     * animations.
     *
     * @return the delay in ticks
     */
    public int getShootDelayTicks() {
        return SHOOT_DELAY_TICKS;
    }

    // -------------------------------------------------------------------------
    // Per-item client-side recoil callbacks
    // -------------------------------------------------------------------------

    /**
     * Per-item camera recoil callbacks, keyed by item registry ID.
     * Invoked on the client at the exact moment of firing, before the next frame
     * renders.
     * ConcurrentHashMap used defensively — registration happens during client init,
     * but the map may be read from the render thread.
     *
     * Addon mods register their own callbacks via registerRecoilCallback() to apply
     * custom recoil behavior for their revolver items without subclassing the
     * callback system.
     */
    private static final Map<ResourceLocation, Consumer<LivingEntity>> RECOIL_CALLBACKS = new ConcurrentHashMap<>();

    /**
     * Registers a camera recoil callback for a specific revolver item.
     * Called during client initialization from Nst_revolvers_recoiledClient
     * or from an addon mod's ClientModInitializer.
     *
     * The callback is invoked on the client at the exact moment of firing,
     * before the next frame renders, so recoil offsets are applied immediately.
     *
     * @param item     the revolver item to bind the callback to
     * @param callback consumer invoked on the client when the revolver is fired
     * @throws IllegalStateException if the item has not yet been registered
     *                               in the item registry at call time
     */
    public static void registerRecoilCallback(Item item, Consumer<LivingEntity> callback) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            throw new IllegalStateException(
                    "registerRecoilCallback called before item registration for: " + item);
        }
        RECOIL_CALLBACKS.put(id, callback);
    }

    /**
     * Client-only renderer container. Populated during client initialization.
     * Accessed by ModItemRenderers to bind the GeckoLib renderer for this item.
     */
    private GeoItemRenderer<?> cachedRenderer = null;

    private void triggerClientCallbacks(Player user, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Consumer<LivingEntity> recoil = RECOIL_CALLBACKS.get(itemId);
        if (recoil != null)
            recoil.accept(user);
    }

    public BaseRevolverItem(Item.Properties properties, float baseDamage, int maxDurability) {
        super(properties.durability(maxDurability));
        this.fallbackDamage = baseDamage;
        this.fallbackDurability = maxDurability;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public void setRenderer(GeoItemRenderer<?> renderer) {
        this.cachedRenderer = renderer;
    }

    // -------------------------------------------------------------------------
    // GeoItem — renderer binding
    // -------------------------------------------------------------------------

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                return cachedRenderer;
            }
        });
    }

    // -------------------------------------------------------------------------
    // ProjectileWeaponItem — required abstract methods (unused; logic is in
    // performShoot())
    // -------------------------------------------------------------------------

    @Override
    protected void shoot(@NotNull ServerLevel level, @NotNull LivingEntity shooter,
            @NotNull InteractionHand hand, @NotNull ItemStack weapon,
            @NotNull List<ItemStack> projectileItems, float velocity,
            float inaccuracy, boolean isCrit, @Nullable LivingEntity target) {
        // Custom shooting logic is in performShoot()
    }

    @Override
    protected void shootProjectile(@NotNull LivingEntity shooter, @NotNull Projectile projectile,
            int index, float velocity, float inaccuracy, float angle,
            @Nullable LivingEntity target) {
        // Revolvers handle their own projectile shooting in performShoot()
    }

    // -------------------------------------------------------------------------
    // Item overrides
    // -------------------------------------------------------------------------

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        // NONE prevents vanilla eating/drinking animation during charging
        return ItemUseAnimation.NONE;
    }

    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.getItem() instanceof BaseBulletItem;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    public float getDamage() {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        ModConfig.RevolverStats stats = ModConfig.get().revolvers.stats.get(id);
        return stats != null ? stats.damage : fallbackDamage;
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        ModConfig.RevolverStats stats = ModConfig.get().revolvers.stats.get(id);
        return stats != null ? stats.durability : fallbackDurability;
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack,
            @NotNull ItemStack newStack,
            boolean slotChanged) {
        return slotChanged;
    }

    // -------------------------------------------------------------------------
    // Core use / reload / fire flow
    // -------------------------------------------------------------------------

    /**
     * Called when the player right-clicks with the revolver.
     *
     * If the revolver is charged:
     * - Server: triggers the fire animation immediately and schedules a delayed
     * projectile
     * launch by placing the bullet and a delay timer into the pending maps.
     * The charged state is cleared right away so the UI responds instantly.
     * - Client: looks up and invokes the recoil callback registered for this item,
     * bypassing GeckoLib animation delay for immediate feedback.
     * Callbacks are keyed by item registry ID, allowing addon mods to register
     * custom behavior per item without modifying this class.
     * - Returns PASS to suppress the vanilla hand-swing animation.
     *
     * If the revolver is not charged:
     * - Returns FAIL if the player has no ammo and is not in creative mode.
     * - Server: triggers the reload animation via GeckoLib triggerAnim().
     * - Sets the active hand to begin the charge timer.
     * - Returns CONSUME to prevent other interactions from firing.
     */
    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player user,
            @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResult.FAIL;
        }

        if (isCharged(stack)) {
            if (!level.isClientSide) {
                // Ignore if a shot is already pending for this player
                if (PENDING_BULLETS.containsKey(user.getUUID())) {
                    return InteractionResult.PASS;
                }

                long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
                triggerAnim(user, instanceId, "controller", "fire");

                // Save the loaded bullet and clear the charged state immediately
                ChargedProjectiles component = stack.get(DataComponents.CHARGED_PROJECTILES);
                ItemStack bullet = Objects.requireNonNull(component).getItems().get(0).copy();
                PENDING_BULLETS.put(user.getUUID(), bullet);
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

                PENDING_SHOTS.put(user.getUUID(), this.getShootDelayTicks());
            }

            if (level.isClientSide()) {
                triggerClientCallbacks(user, stack);
            }

            return InteractionResult.PASS;
        }

        // --- Reload logic ---
        ItemStack ammo = user.getProjectile(stack);
        if (!user.getAbilities().instabuild && ammo.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            triggerAnim(user, instanceId, "controller", "reload");
        }

        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
            @NotNull LivingEntity user) {
        return stack;
    }

    /**
     * Called when player releases right-click or gets interrupted.
     * Loads the bullet if charging completed successfully.
     * Stops reload animation if player releases early.
     */
    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
            @NotNull LivingEntity user, int remainingUseTicks) {
        int chargeTimerMax = getChargeTimeTicks();
        int chargedTicks = USE_DURATION - remainingUseTicks;

        if (chargedTicks >= chargeTimerMax && !isCharged(stack)) {
            if (loadBullet(user, stack)) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(),
                        ModSounds.RELOAD.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        } else {
            // Stop reload animation when player releases before full charge
            if (!level.isClientSide && user instanceof Player player) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
                triggerAnim(player, instanceId, "controller", "idle");
            }
        }
        return false;
    }

    /**
     * Called every tick while the item is in an entity's inventory.
     * Handles state ticks for the revolver, focusing on the actively held instance.
     *
     * Responsibilities:
     * - Triggers the draw animation and sound when the item is first selected.
     * - Unsets the draw animation flag when the item is not selected.
     * - Processes pending delayed shots. If the delay timer reaches zero, invokes
     * performShoot() with the cached bullet and removes the pending state.
     */
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
            @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!selected) {
            clearDrawAnimationFlag(stack);
            return; // Do not process draw or pending shots for inactive slots
        }

        if (level.isClientSide || !(entity instanceof Player player))
            return;

        // Draw animation + sound on first equip
        if (!hasDrawAnimationPlayed(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.DRAW.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            triggerAnim(player, instanceId, "controller", "draw");
            markDrawAnimationPlayed(stack);
        }

        // Delayed shoot — only executed when the item is actively held
        Integer ticksLeft = PENDING_SHOTS.get(player.getUUID());
        if (ticksLeft == null)
            return;

        if (ticksLeft <= 0) {
            PENDING_SHOTS.remove(player.getUUID());
            ItemStack savedBullet = PENDING_BULLETS.remove(player.getUUID());
            if (savedBullet != null) {
                InteractionHand hand = player.getMainHandItem().getItem() == stack.getItem()
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;
                performShoot(level, player, hand, stack, savedBullet,
                        getProjectileVelocity(), getProjectileDivergence());
            }
        } else {
            PENDING_SHOTS.put(player.getUUID(), ticksLeft - 1);
        }
    }

    // -------------------------------------------------------------------------
    // Draw animation flag helpers
    // -------------------------------------------------------------------------

    /**
     * Checks if the draw animation has already been played for this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected boolean hasDrawAnimationPlayed(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag().getBoolean(DRAW_PLAYED_KEY);
    }

    /**
     * Marks that the draw animation has been played for this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void markDrawAnimationPlayed(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putBoolean(DRAW_PLAYED_KEY, true));
    }

    /**
     * Clears the draw animation flag from this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void clearDrawAnimationFlag(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.remove(DRAW_PLAYED_KEY));
    }

    // -------------------------------------------------------------------------
    // Bullet loading and shooting
    // -------------------------------------------------------------------------

    /**
     * Loads a bullet from the user's inventory into the revolver.
     * Uses vanilla ChargedProjectiles component for compatibility.
     * Protected to allow subclasses to override ammo selection or loading logic.
     */
    protected boolean loadBullet(LivingEntity shooter, ItemStack revolver) {
        ItemStack ammo = shooter.getProjectile(revolver);

        if (ammo.isEmpty() && !(shooter instanceof Player p && p.getAbilities().instabuild)) {
            return false;
        }

        List<ItemStack> projectiles = new ArrayList<>();

        if (!ammo.isEmpty()) {
            ItemStack bulletCopy = ammo.copy();
            bulletCopy.setCount(1);
            projectiles.add(bulletCopy);

            if (shooter instanceof Player player && !player.getAbilities().instabuild) {
                ammo.shrink(1);
            }
        } else {
            // Creative mode fallback
            projectiles.add(new ItemStack(ModItems.STONE_BULLET.get()));
        }

        revolver.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
        return true;
    }

    /**
     * Calculates the barrel tip position server-side.
     * Uses different offsets for players (optimized for 1st-person / eye view)
     * and mobs (optimized for 3rd-person outstretched-arm view).
     * Protected to allow subclasses to override barrel positioning for custom
     * models.
     */
    protected Vec3 calcBarrelPosition(LivingEntity shooter) {
        float pitch = shooter.getXRot();
        float yaw = (shooter instanceof Mob mob) ? mob.yHeadRot : shooter.getYRot();

        Vec3 lookFwd = Vec3.directionFromRotation(pitch, yaw);
        Vec3 lookUp = Vec3.directionFromRotation(pitch - 90, yaw).normalize();
        Vec3 lookRight = lookFwd.cross(lookUp).normalize();

        if (shooter instanceof Player) {
            // 1st-person perspective offset relative to the camera (eye pos)
            Vec3 eyePos = shooter.getEyePosition();
            return eyePos
                    .add(lookFwd.scale(0.8)) // Forward from camera
                    .add(lookRight.scale(0.35)) // Right from camera
                    .add(lookUp.scale(-0.25)); // Down from camera
        } else {
            // 3rd-person shoulder offset for mobs holding gun in outstretched right arm
            float bodyYaw = shooter.yBodyRot;
            Vec3 bodyRight = Vec3.directionFromRotation(0, bodyYaw + 90).normalize();

            double baseY = shooter.getY() + shooter.getEyeHeight() - 0.2;
            Vec3 center = new Vec3(shooter.getX(), baseY, shooter.getZ());

            Vec3 shoulder = center.add(bodyRight.scale(BARREL_SHOULDER));
            return shoulder
                    .add(lookFwd.scale(BARREL_FWD))
                    .add(lookRight.scale(BARREL_RIGHT))
                    .add(lookUp.scale(BARREL_UP));
        }
    }

    /**
     * Spawns the loaded bullet as a projectile entity and handles shooting effects.
     * Combines revolver base damage with bullet damage and plays gunshot sounds.
     * Spawns the projectile from the perspective-corrected barrel position.
     * Employs raycasting to ensure the projectile trajectory converges on the
     * crosshair.
     *
     * This overload accepts a pre-extracted bullet stack, used by the delayed-shot
     * system
     * (the bullet is pulled from the pending map rather than from the item's
     * component).
     *
     * @param bulletStack the bullet item that was loaded before the delay timer
     *                    started
     */
    public void performShoot(Level level, LivingEntity shooter, InteractionHand hand,
            ItemStack stack, ItemStack bulletStack,
            float velocity, float divergence) {
        if (level.isClientSide)
            return;

        float bulletDamage = 0.0f;
        if (bulletStack.getItem() instanceof BaseBulletItem bulletItem) {
            bulletDamage = bulletItem.getDamage();
        }
        float totalDamage = this.getDamage() + bulletDamage;

        BulletProjectileEntity projectile = new BulletProjectileEntity(
                level, shooter, bulletStack, totalDamage);

        // 1. Calculate physical barrel tip position
        Vec3 barrelPos = calcBarrelPosition(shooter);
        projectile.setPos(barrelPos.x, barrelPos.y, barrelPos.z);

        // 2. Raycast from the eye to find the exact crosshair target.
        // This corrects the trajectory so the bullet always converges on what
        // the player is aiming at, regardless of the barrel-offset distance.
        float yaw = (shooter instanceof Mob mob) ? mob.yHeadRot : shooter.getYRot();
        Vec3 lookVec = Vec3.directionFromRotation(shooter.getXRot(), yaw);
        Vec3 eyePos = shooter.getEyePosition();
        double maxRange = 100.0;
        Vec3 endPoint = eyePos.add(lookVec.scale(maxRange));

        // Raycast against blocks
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, endPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter));

        Vec3 actualTarget = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation()
                : endPoint;

        // Check entities along the ray and take the closest intersection
        AABB searchBox = shooter.getBoundingBox().expandTowards(lookVec.scale(maxRange)).inflate(1.0);
        double closestDistSq = actualTarget.distanceToSqr(eyePos);

        for (Entity entity : level.getEntities(shooter, searchBox,
                e -> !e.isSpectator() && e.isPickable())) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3f);
            Optional<Vec3> hitOpt = entityBox.clip(eyePos, endPoint);
            if (hitOpt.isPresent()) {
                double distSq = eyePos.distanceToSqr(hitOpt.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    actualTarget = hitOpt.get();
                }
            }
        }

        Vec3 aimDir = actualTarget.subtract(barrelPos).normalize();

        // 3. Launch projectile in the corrected direction
        projectile.shoot(aimDir.x, aimDir.y, aimDir.z, velocity, divergence);

        level.addFreshEntity(projectile);
        stack.hurtAndBreak(1, shooter, LivingEntity.getSlotForHand(hand));

        // Gunshot sound
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                ModSounds.SHOT.get(), SoundSource.PLAYERS,
                0.8f, 1.5f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Checks if the revolver currently has a loaded bullet.
     */
    public static boolean isCharged(ItemStack stack) {
        ChargedProjectiles component = stack.get(DataComponents.CHARGED_PROJECTILES);
        return component != null && !component.isEmpty();
    }

    /**
     * Removes any pending shot delays for the given player.
     * Should be called when a player disconnects or unloads.
     */
    public static void removePendingShot(UUID playerId) {
        PENDING_SHOTS.remove(playerId);
    }

    /**
     * Removes any stored pending bullet for the given player.
     * Should be called when a player disconnects or unloads.
     */
    public static void removePendingBullet(UUID playerId) {
        PENDING_BULLETS.remove(playerId);
    }

    // -------------------------------------------------------------------------
    // GeckoLib animation setup
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<BaseRevolverItem> controller = new AnimationController<>(this, "controller", 5, state -> {
            ItemDisplayContext perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            ModConfig config = ModConfig.get();
            if (perspective == null) {
                return PlayState.CONTINUE;
            }

            boolean isFirstPerson = perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

            AnimationController<?> ctrl = state.getController();

            // In third-person: suppress draw animation by replacing with idle
            if (!isFirstPerson) {
                RawAnimation triggered = ctrl.getTriggeredAnimation();
                if (triggered != null && DRAW_ANIM.equals(triggered)) {
                    ctrl.setAnimation(IDLE_ANIM);
                    return PlayState.CONTINUE;
                }
            }

            // Update animation speed ONLY when a new animation is triggered.
            // This ensures the speed remains stable for the duration of the animation
            // and doesn't reset to 1.0f prematurely once the trigger is processed.
            RawAnimation triggered = ctrl.getTriggeredAnimation();
            if (triggered != null) {
                if (RELOAD_ANIM.equals(triggered)) {
                    ctrl.setAnimationSpeed(50.0f / config.revolvers.chargeTimeTicks);
                } else {
                    ctrl.setAnimationSpeed(1.0f);
                }
            }

            return PlayState.CONTINUE;
        });

        controller
                .receiveTriggeredAnimations()
                .triggerableAnim("fire", FIRE_ANIM)
                .triggerableAnim("reload", RELOAD_ANIM)
                .triggerableAnim("draw", DRAW_ANIM)
                .triggerableAnim("idle", IDLE_ANIM);

        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
