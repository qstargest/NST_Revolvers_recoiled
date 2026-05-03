package org.stargest.nst_revrecoiled.Items;

import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.util.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModConfig;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
 * - GeckoLib animations (fire, reload, draw, idle)
 * - Perspective-aware rendering (draw animation suppressed in third-person)
 * - Draw animation when equipping
 * - Prevents vanilla animations (hand swing, item switch)
 * - Ballistic projectiles with gravity (spawned from calculated barrel position)
 * - Delayed firing mechanics allowing synchronization of projectile spawning with fire animations
 * - Per-item recoil callbacks (extensible for addon mods)
 *
 * Addon mods can register custom recoil behavior per item via
 * registerRecoilCallback(), called during client initialization.
 * Core methods (loadBullet, calcBarrelPosition, performShoot, draw animation helpers)
 * are protected to allow subclasses to override shooting and animation behavior.
 * Pending shots and memory management are tracked internally for safe server-side firing delays.
 */
public abstract class BaseRevolverItem extends RangedWeaponItem implements GeoItem, RevolverArmPoseItem {

    // Default constants for fallback
    private static final int DEFAULT_CHARGE_TIME_TICKS = 50; // 2.5 seconds at 20 TPS
    private static final float DEFAULT_PROJECTILE_VELOCITY = 6.0f;
    private static final float DEFAULT_PROJECTILE_DIVERGENCE = 0.2f; // Reduced for better accuracy

    // Barrel position offsets — mirror TP_FIRE_* constants in RevolverParticleHandler
    private static final double BARREL_SHOULDER = 0.35; // Body-right offset to shoulder
    private static final double BARREL_FWD      = 1.0;  // Forward from shoulder
    private static final double BARREL_RIGHT    = -0.20;
    private static final double BARREL_UP       = 0.05;

    public static final int SHOOT_DELAY_TICKS = 3;

    /** Tracks the remaining delay ticks before a scheduled shot is actually fired. */
    private static final Map<UUID, Integer> PENDING_SHOTS = new ConcurrentHashMap<>();

    /** Stores bullets that have been fired but are waiting for their delayed release. */
    private static final Map<UUID, ItemStack> PENDING_BULLETS = new ConcurrentHashMap<>();

    // Animation definitions
    private static final RawAnimation FIRE_ANIM   = RawAnimation.begin().thenPlay("fire");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation DRAW_ANIM   = RawAnimation.begin().thenPlay("draw");
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

    private final float fallbackDamage;
    private final int fallbackDurability;

    // -------------------------------------------------------------------------
    // Config properties for dynamic behavior
    // -------------------------------------------------------------------------

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
     * Allows custom revolvers to synchronize projectile spawning with firing animations.
     *
     * @return the delay in ticks
     */
    public int getShootDelayTicks(){
        return SHOOT_DELAY_TICKS;
    }

    // -------------------------------------------------------------------------
    // Per-item client-side fire callbacks
    // -------------------------------------------------------------------------

    /**
     * Per-item camera recoil callbacks, keyed by item registry ID.
     * Invoked on the client at the exact moment of firing, before the next frame renders.
     * ConcurrentHashMap used defensively — registration happens during client init,
     * but the map may be read from the render thread.
     *
     * Addon mods register their own callbacks via registerRecoilCallback() to apply
     * custom recoil behavior for their revolver items without subclassing the callback system.
     */
    private static final Map<Identifier, Consumer<LivingEntity>> RECOIL_CALLBACKS =
            new ConcurrentHashMap<>();

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
        Identifier id = Registries.ITEM.getId(item);
        if (id == null || id.equals(Registries.ITEM.getId(Items.AIR))) {
            throw new IllegalStateException(
                    "registerRecoilCallback called before item registration for: " + item
            );
        }
        RECOIL_CALLBACKS.put(id, callback);
    }

    public BaseRevolverItem(Settings settings, float baseDamage, int maxDurability) {
        super(settings.maxDamage(maxDurability));
        this.fallbackDamage = baseDamage;
        this.fallbackDurability = maxDurability;
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
    // RangedWeaponItem — required abstract method (unused; logic is in performShoot())
    // -------------------------------------------------------------------------

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index,
                         float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        // Not used — custom shooting logic is in the protected performShoot() method below
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
        return getChargeTimeTicks();
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
        String id = Registries.ITEM.getId(this).getPath();
        ModConfig.RevolverStats stats =
                ModConfig.get().revolvers.stats.get(id);
        return stats != null ? stats.damage : fallbackDamage;
    }

    /**
     * Called when the player right-clicks with the revolver.
     *
     * If the revolver is charged:
     * - Server: triggers the firing animation and schedules a delayed projectile launch
     *   by placing the bullet and a delay timer into the pending maps.
     * - Client: looks up and invokes the recoil callback registered for this item,
     *   bypassing GeckoLib animation delay for immediate feedback.
     *   Callbacks are keyed by item registry ID, allowing for addon extensibility.
     * - Returns PASS to suppress the vanilla hand-swing animation.
     *
     * If the revolver is not charged:
     * - Returns FAIL if the player has no ammo and is not in creative mode.
     * - Server: triggers the reload animation via GeckoLib triggerAnim().
     * - Sets the active hand to begin the charge timer.
     * - Returns CONSUME to prevent other interactions from firing.
     *
     * @param world the world the item was used in
     * @param user  the user of the item
     * @param hand  the hand the item is held in
     * @return the result of using the item
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (hand == Hand.OFF_HAND) {
            return TypedActionResult.fail(stack);
        }

        if (isCharged(stack)) {
            // Shoot only on server
            if (!world.isClient) {
                // Ignore if a shot is already pending
                if (PENDING_BULLETS.containsKey(user.getUuid())) {
                    return TypedActionResult.fail(stack);
                }
                long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
                triggerAnim(user, instanceId, "controller", "fire");

                // Save the loaded bullet and clear the charged state
                ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                ItemStack bullet = Objects.requireNonNull(component).getProjectiles().get(0).copy();
                PENDING_BULLETS.put(user.getUuid(), bullet);
                stack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);

                PENDING_SHOTS.put(user.getUuid(), this.getShootDelayTicks());

            }

            if (world.isClient()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                Consumer<LivingEntity> recoilCallback = RECOIL_CALLBACKS.get(itemId);
                if (recoilCallback != null) recoilCallback.accept(user);
            }

            // PASS prevents vanilla hand swing animation
            return TypedActionResult.pass(stack);
        }

        // --- Reload logic proceeds normally ---
        ItemStack ammo = user.getProjectileType(stack);
        if (!user.getAbilities().creativeMode && ammo.isEmpty()) {
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(user, instanceId, "controller", "reload");
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    /**
     * Called when player releases right-click or gets interrupted.
     * Loads the bullet if charging completed successfully.
     * Stops reload animation if player releases early.
     */
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        int chargeTimerMax = getChargeTimeTicks();
        int chargedTicks = chargeTimerMax - remainingUseTicks;
        float chargeProgress = (float) chargedTicks / chargeTimerMax;

        if (chargeProgress >= 1.0f && !isCharged(stack)) {
            if (loadBullet(user, stack)) {
                world.playSound(
                        null,
                        user.getX(), user.getY(), user.getZ(),
                        ModSounds.RELOAD,
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

        //return false;
    }

    /**
     * Called every tick while the item is in an entity's inventory.
     * Handles state ticks for the revolver, focusing on the actively held instance.
     *
     * Responsibilities:
     * - Triggers the draw animation when the item is first selected.
     * - Unsets the draw animation flag when the item is not selected.
     * - Processes pending delayed shots. If the delay timer reaches zero, invokes
     *   performShoot method with the cached bullet and removes the pending state.
     *
     * @param stack    the item stack
     * @param world    the world
     * @param entity   the entity holding the item
     * @param slot     the inventory slot index
     * @param selected true if the item is in the active hand slot
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!selected) {
            clearDrawAnimationFlag(stack);
            return; // Do not process draw or pending shots for inactive slots
        }

        if (world.isClient || !(entity instanceof PlayerEntity player)) return;

        // Draw animation
        if (!hasDrawAnimationPlayed(stack)) {
            world.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.DRAW,
                    SoundCategory.PLAYERS,
                    1.0f, 1.0f
            );

            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(player, instanceId, "controller", "draw");
            markDrawAnimationPlayed(stack);
        }

        // Delayed shoot — only executed when the item is actively held
        Integer ticksLeft = PENDING_SHOTS.get(player.getUuid());
        if (ticksLeft == null) return;

        if (ticksLeft <= 0) {
            PENDING_SHOTS.remove(player.getUuid());
            ItemStack savedBullet = PENDING_BULLETS.remove(player.getUuid());
            if (savedBullet != null) {
                Hand hand = player.getMainHandStack().getItem() == stack.getItem()
                        ? Hand.MAIN_HAND
                        : Hand.OFF_HAND;
                performShoot(world, player, hand, stack, savedBullet,
                        getProjectileVelocity(), getProjectileDivergence());
            }
        } else {
            PENDING_SHOTS.put(player.getUuid(), ticksLeft - 1);
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
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getBoolean(DRAW_PLAYED_KEY);
    }

    /**
     * Marks that the draw animation has been played for this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void markDrawAnimationPlayed(ItemStack stack) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, nbt -> {
            NbtCompound compound = nbt.copyNbt();
            compound.putBoolean(DRAW_PLAYED_KEY, true);
            return NbtComponent.of(compound);
        });
    }

    /**
     * Clears the draw animation flag from this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void clearDrawAnimationFlag(ItemStack stack) {
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
     * Protected to allow subclasses to override ammo selection or loading logic.
     */
    protected boolean loadBullet(LivingEntity shooter, ItemStack revolver) {
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
     * Calculates the barrel tip position server-side.
     * Uses different offsets for players (optimized for 1st person view)
     * and mobs (optimized for 3rd person outstretched arm view).
     */
    protected Vec3d calcBarrelPosition(LivingEntity shooter) {
        float pitch   = shooter.getPitch();
        float yaw     = shooter.getYaw();

        Vec3d lookFwd   = Vec3d.fromPolar(pitch, yaw);
        Vec3d lookUp    = Vec3d.fromPolar(pitch - 90, yaw).normalize();
        Vec3d lookRight = lookFwd.crossProduct(lookUp).normalize();

        if (shooter instanceof PlayerEntity) {
            // 1st-person perspective offset relative to the camera (eye pos)
            Vec3d eyePos = shooter.getEyePos();
            return eyePos
                    .add(lookFwd.multiply(0.8))   // Forward from camera
                    .add(lookRight.multiply(0.35)) // Right from camera
                    .add(lookUp.multiply(-0.25));  // Down from camera
        } else {
            // 3rd-person shoulder offset for mobs (like RevolverBandit holding gun in outstretched right arm)
            double baseY = shooter.getY() + shooter.getStandingEyeHeight() - 0.2;
            Vec3d center = new Vec3d(shooter.getX(), baseY, shooter.getZ());

            // To find the exact right hand position safely regardless of yaw math flipped signs:
            // lookRight points to the entity's visual right.
            // 0.35 blocks to the right places the anchor on the right shoulder.
            // 0.85 blocks forward matches the fully outstretched arm.
            return center
                    .add(lookFwd.multiply(1.5))    // Length of extended arm + gun
                    .add(lookRight.multiply(0.20))  // Offset to the physical Right shoulder
                    .add(lookUp.multiply(1));   // Slight dip for arm posture
        }
    }

    /**
     * Spawns the loaded bullet as a projectile entity and handles shooting effects.
     * Combines revolver base damage with bullet damage.
     * Spawns the projectile from the perspective-corrected barrel position for visual accuracy.
     * Called by the inventory tick loop once the delayed shooting timer reaches zero.
     * Plays the gunshot explosion sound and damages the item stack.
     * Protected to allow subclasses to override projectile type, damage scaling,
     * or sound behavior.
     *
     * @param world       the server world
     * @param shooter     the entity shooting the revolver
     * @param hand        the hand the revolver is held in
     * @param stack       the revolver item stack
     * @param bulletStack the bullet item stack to fire
     * @param velocity    the base velocity of the projectile
     * @param divergence  the scatter/divergence applying to the projectile
     */
    public void performShoot(World world, LivingEntity shooter, Hand hand, ItemStack stack,
                                ItemStack bulletStack, float velocity, float divergence) {
        if (world.isClient) return;

        float bulletDamage = 0.0f;
        if (bulletStack.getItem() instanceof BaseBulletItem bulletItem) {
            bulletDamage = bulletItem.getDamage();
        }
        float totalDamage = this.getDamage() + bulletDamage;

        BulletProjectileEntity projectile = new BulletProjectileEntity(world, shooter, bulletStack, totalDamage);

        // 1. Calculate physical barrel tip position
        Vec3d barrelPos = calcBarrelPosition(shooter);
        projectile.setPosition(barrelPos.x, barrelPos.y, barrelPos.z);

        // 2. Correction for offset: perform a raycast from the eye to find the exact target point.
        // This ensures the trajectory converges directly on the crosshair target regardless of distance.
        Vec3d lookVec = shooter.getRotationVec(1.0f);
        Vec3d eyePos = shooter.getEyePos();
        double maxRange = 100.0;
        Vec3d endPoint = eyePos.add(lookVec.multiply(maxRange));

        // Raycast against blocks
        HitResult blockHit = world.raycast(new RaycastContext(
                eyePos, endPoint,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                shooter
        ));

        Vec3d actualTarget = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getPos()
                : endPoint;

        // Try to find entities.json intersecting the ray
        Box searchBox = shooter.getBoundingBox().stretch(lookVec.multiply(maxRange)).expand(1.0);
        double closestDist = actualTarget.squaredDistanceTo(eyePos);

        for (Entity entity : world.getOtherEntities(shooter, searchBox, e -> !e.isSpectator() && e.canHit())) {
            Box entityBox = entity.getBoundingBox().expand(0.3f);
            Optional<Vec3d> hitOpt = entityBox.raycast(eyePos, endPoint);
            if (hitOpt.isPresent()) {
                double dist = eyePos.squaredDistanceTo(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    actualTarget = hitOpt.get();
                }
            }
        }

        Vec3d aimDir = actualTarget.subtract(barrelPos).normalize();

        // 3. Launch projectile
        projectile.setVelocity(aimDir.x, aimDir.y, aimDir.z, velocity, divergence);

        world.spawnEntity(projectile);
        stack.damage(1, shooter, LivingEntity.getSlotForHand(hand));

        world.playSound(
                null,
                shooter.getX(), shooter.getY(), shooter.getZ(),
                ModSounds.SHOT,
                SoundCategory.PLAYERS,
                0.35f,
                1.5f / (world.getRandom().nextFloat() * 0.4f + 0.8f)
        );
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

    // -------------------------------------------------------------------------
    // GeckoLib animation setup
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<BaseRevolverItem> controller = new AnimationController<>(this, "controller", 5, state -> {
            // Get render perspective (may be null in some contexts)
            ModelTransformationMode perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            ModConfig config = ModConfig.get();
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
                .receiveTriggeredAnimations() // Required for predicate to be called during triggers
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

    /**
     * Removes any pending shot delays for the given player.
     * Called when a player disconnects or unloads.
     */
    public static void removePendingShot(UUID playerId){
        PENDING_SHOTS.remove(playerId);
    }

    /**
     * Removes any stored pending bullet for the given player.
     * Called when a player disconnects or unloads.
     */
    public static void removePendingBullet(UUID playerId){
        PENDING_BULLETS.remove(playerId);
    }
}
