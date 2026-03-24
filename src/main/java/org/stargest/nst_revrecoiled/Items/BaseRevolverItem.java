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
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.util.ModItems;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * - Per-item recoil and particle callbacks (extensible for addon mods)
 * - Server-timed reload particles (sent via packet at animation keyframe tick, no GeckoLib dependency)
 * - Networked particle synchronization (all nearby players see fire and reload particles)
 *
 * Addon mods can register custom recoil and particle behavior per item via
 * registerRecoilCallback() and registerParticleCallback(), called during client initialization.
 * Core methods (loadBullet, calcBarrelPosition, performShoot, draw animation helpers)
 * are protected to allow subclasses to override shooting and animation behavior.
 */
public abstract class BaseRevolverItem extends RangedWeaponItem implements GeoItem, RevolverArmPoseItem {

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
     * Per-item muzzle-flash particle callbacks, keyed by item registry ID.
     * Invoked on the client at the exact moment of firing to bypass GeckoLib animation delay.
     * ConcurrentHashMap used defensively — registration happens during client init,
     * but the map may be read from the render thread.
     *
     * Addon mods register their own callbacks via registerParticleCallback() to apply
     * custom particle effects for their revolver items.
     */
    private static final Map<Identifier, Consumer<LivingEntity>> PARTICLE_CALLBACKS =
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

    /**
     * Registers a muzzle-flash particle callback for a specific revolver item.
     * Called during client initialization from Nst_revolvers_recoiledClient
     * or from an addon mod's ClientModInitializer.
     *
     * The callback is invoked on the client at the exact moment of firing,
     * bypassing GeckoLib animation delay for instant visual feedback.
     *
     * @param item     the revolver item to bind the callback to
     * @param callback consumer invoked on the client when the revolver is fired
     * @throws IllegalStateException if the item has not yet been registered
     *                               in the item registry at call time
     */
    public static void registerParticleCallback(Item item, Consumer<LivingEntity> callback) {
        Identifier id = Registries.ITEM.getId(item);
        if (id == null || id.equals(Registries.ITEM.getId(Items.AIR))) {
            throw new IllegalStateException(
                    "registerParticleCallback called before item registration for: " + item
            );
        }
        PARTICLE_CALLBACKS.put(id, callback);
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
     * Called every tick while the revolver is being actively used (charging).
     * Sends RevolverReloadParticlePacket to the shooter and all tracking players
     * at tick 20 (1.0 second into the charge) — the moment the bullet-insertion
     * keyframe occurs in the reload animation.
     *
     * Sending the packet server-side at a fixed tick is simpler and more reliable
     * than relying on GeckoLib animation keyframe callbacks, and ensures reload
     * particles are visible to all nearby players regardless of their GeckoLib state.
     */
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        int elapsed = CHARGE_TIME_TICKS - remainingUseTicks;

        // Tick 20 = 1.0 second = bullet-insertion keyframe in the reload animation
        if (elapsed == 20 && !world.isClient) {
            RevolverReloadParticlePacket packet = new RevolverReloadParticlePacket(user.getId());

            // Send to the shooter themselves
            if (user instanceof ServerPlayerEntity shooter) {
                ServerPlayNetworking.send(shooter, packet);
            }
            // Send to all other players tracking this entity
            PlayerLookup.tracking(user).forEach(p -> ServerPlayNetworking.send(p, packet));
        }
    }

    /**
     * Called when the player right-clicks with the revolver.
     *
     * If the revolver is charged:
     * - Server: fires the projectile via performShoot().
     * - Client: looks up and invokes the recoil callback registered for this item,
     *   then the particle callback — both bypassing GeckoLib animation delay.
     *   Callbacks are keyed by item registry ID, allowing addon mods to register
     *   custom behavior per item without modifying this class.
     * - Server: broadcasts RevolverFireParticlePacket to all tracking players so
     *   that nearby players also see the muzzle-flash (local player is skipped
     *   on the receiving end to avoid duplication).
     * - Returns PASS to suppress the vanilla hand-swing animation.
     *
     * If the revolver is not charged:
     * - Returns FAIL if the player has no ammo and is not in creative mode.
     * - Server: triggers the reload animation via GeckoLib triggerAnim().
     * - Sets the active hand to begin the charge timer.
     * - Returns CONSUME to prevent other interactions from firing.
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isCharged(stack)) {
            // Shoot only on server
            if (!world.isClient) {
                performShoot(world, user, hand, stack, PROJECTILE_VELOCITY, PROJECTILE_DIVERGENCE);
            }

            // Invoke per-item recoil and particle callbacks on the client.
            // Keyed by item ID so addon mods can register different behavior per revolver.
            if (world.isClient()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());

                Consumer<LivingEntity> recoilCallback = RECOIL_CALLBACKS.get(itemId);
                if (recoilCallback != null) {
                    recoilCallback.accept(user);
                }

                Consumer<LivingEntity> particleCallback = PARTICLE_CALLBACKS.get(itemId);
                if (particleCallback != null) {
                    particleCallback.accept(user);
                }
            }

            // Server-side: broadcast fire particles to all players tracking this entity
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
     * Calculates the barrel tip position server-side,
     * mirroring the third-person fire particle offset in RevolverParticleHandler.
     * This ensures bullets spawn from the visually correct position.
     * Uses constants that match RevolverParticleHandler's TP_FIRE_* offsets.
     * Protected to allow subclasses to override barrel positioning for custom models.
     */
    protected Vec3d calcBarrelPosition(LivingEntity shooter) {
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
     * Protected to allow subclasses to override projectile type, damage scaling,
     * sound, or animation behavior.
     */
    protected void performShoot(World world, LivingEntity shooter, Hand hand, ItemStack stack,
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
