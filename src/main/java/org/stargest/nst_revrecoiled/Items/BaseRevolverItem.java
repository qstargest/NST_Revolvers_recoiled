package org.stargest.nst_revrecoiled.Items;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.UseAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModConfig;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;

import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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

    // Default constants for fallback
    private static final int DEFAULT_CHARGE_TIME_TICKS = 50; // 2.5 seconds at 20 TPS
    private static final float DEFAULT_PROJECTILE_VELOCITY = 6.0f;
    private static final float DEFAULT_PROJECTILE_DIVERGENCE = 0.2f; // Reduced for better accuracy

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

    private Supplier<GeoItemRenderer<?>> rendererFactory = null;

    /**
     * Client-only renderer container. Populated during client initialization.
     * Accessed by ModItemRenderers to bind the GeckoLib renderer for this item.
     */
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    private final float fallbackDamage;
    private final int fallbackDurability;

    private static final String CHARGED_KEY = "ChargedBullet";

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

    public BaseRevolverItem(Settings settings, float baseDamage, int maxDurability) {
        super(settings.maxDamage(maxDurability));
        this.fallbackDamage = baseDamage;
        this.fallbackDurability = maxDurability;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // -------------------------------------------------------------------------
    // GeoItem — renderer binding
    // -------------------------------------------------------------------------

    public void setRendererFactory(Supplier<GeoItemRenderer<?>> factory) {
        this.rendererFactory = factory;
    }

    /**
     * GeckoLib calls this on both client and server.
     * Only the client-side renderer is set, so server-side this is a no-op.
     */
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        if (rendererFactory == null) return;

        Supplier<GeoItemRenderer<?>> factory = rendererFactory;
        consumer.accept(new RenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getCustomRenderer() {
                if (renderer == null) {
                    renderer = factory.get();
                }
                return renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
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
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
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
    public int getMaxUseTime(ItemStack stack) {
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
        int chargeTimerMax = getChargeTimeTicks();
        int elapsed = chargeTimerMax - remainingUseTicks;

        // Trigger reload particles at 40% of the charge duration (matches default tick 20/50)
        int triggerTick = (int) (0.4f * chargeTimerMax);
        if (elapsed == triggerTick && !world.isClient) {
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isCharged(stack)) {
            // Shoot only on server
            if (!world.isClient) {
                performShoot(world, user, hand, stack, getProjectileVelocity(), getProjectileDivergence());
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
            return TypedActionResult.pass(stack);
        }

        ItemStack ammo = user.getProjectileType(stack);
        if (!user.getAbilities().creativeMode && ammo.isEmpty()) {
            return TypedActionResult.fail(stack);
        }

        // Trigger reload animation when starting to charge
        if (!world.isClient) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(user, instanceId, "controller", "animation.model.reloademptyright");
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

        //return false;
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
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(DRAW_PLAYED_KEY);
    }

    /**
     * Marks that the draw animation has been played for this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void markDrawAnimationPlayed(ItemStack stack) {
        stack.getOrCreateNbt().putBoolean(DRAW_PLAYED_KEY, true);
    }

    /**
     * Clears the draw animation flag from this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void clearDrawAnimationFlag(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) nbt.remove(DRAW_PLAYED_KEY);
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

        ItemStack bullet = ammo.isEmpty()
                ? new ItemStack(ModItems.STONE_BULLET)
                : ammo.copy();
        bullet.setCount(1);

        if (!ammo.isEmpty() && shooter instanceof PlayerEntity player
                && !player.getAbilities().creativeMode) {
            ammo.decrement(1);
        }

        revolver.getOrCreateNbt().put(CHARGED_KEY, bullet.writeNbt(new NbtCompound()));
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

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(CHARGED_KEY)) return;

        ItemStack bulletStack = ItemStack.fromNbt(nbt.getCompound(CHARGED_KEY));

        float bulletDamage = 0.0f;
        if (bulletStack.getItem() instanceof BaseBulletItem bulletItem) {
            bulletDamage = bulletItem.getDamage();
        }
        float totalDamage = this.getDamage() + bulletDamage;

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

        stack.damage(1, shooter, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));

        world.playSound(
                null,
                shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS,
                0.35f,
                1.5f / (world.getRandom().nextFloat() * 0.4f + 0.8f)
        );

        // Trigger fire animation
        if (shooter instanceof PlayerEntity player) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            triggerAnim(player, instanceId, "controller", "animation.model.fireright");
        }

        stack.getOrCreateNbt().remove(CHARGED_KEY);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Checks if the revolver currently has a loaded bullet.
     */
    public static boolean isCharged(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(CHARGED_KEY);
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

            // In third-person: suppress draw animation by stopping it

            if (!isFirstPerson) {
                RawAnimation current = ctrl.getCurrentRawAnimation();
                if (DRAW_ANIM.equals(current)) {
                    ctrl.stop();
                    return PlayState.STOP;
                }
            }

            // Update animation speed ONLY when a new animation is triggered.
            // This ensures the speed remains stable for the duration of the animation
            // and doesn't reset to 1.0f prematurely once the trigger is processed.
            if (ctrl.getCurrentRawAnimation() != null) {
                if (RELOAD_ANIM.equals(ctrl.getCurrentRawAnimation())) {
                    ctrl.setAnimationSpeed(50.0 / config.revolvers.chargeTimeTicks);
                } else {
                    ctrl.setAnimationSpeed(1.0);
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
