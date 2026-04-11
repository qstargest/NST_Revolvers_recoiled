package org.stargest.nst_revrecoiled.Items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Entities.BulletProjectileEntity;
import org.stargest.nst_revrecoiled.network.RevolverFireParticlePacket;
import org.stargest.nst_revrecoiled.network.RevolverReloadParticlePacket;
import org.stargest.nst_revrecoiled.util.ModConfig;
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
import software.bernie.geckolib.renderer.GeoItemRenderer;

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
public abstract class BaseRevolverItem extends ProjectileWeaponItem implements GeoItem, RevolverArmPoseItem {

    private static final double BARREL_SHOULDER = 0.35;
    private static final double BARREL_FWD      = 1.0;
    private static final double BARREL_RIGHT    = -0.20;
    private static final double BARREL_UP       = 0.05;

    private static final RawAnimation FIRE_ANIM   = RawAnimation.begin().thenPlay("animation.model.fireright");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("animation.model.reloademptyright");
    private static final RawAnimation DRAW_ANIM   = RawAnimation.begin().thenPlay("animation.model.drawright");
    private static final RawAnimation IDLE_ANIM   = RawAnimation.begin().thenLoop("idle");

    private static final int USE_DURATION = 72000;

    private static final String DRAW_PLAYED_KEY = "DrawAnimPlayed";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final float fallbackDamage;
    private final int fallbackDurability;

    public int getChargeTimeTicks() { return ModConfig.get().revolvers.chargeTimeTicks; }
    public float getProjectileVelocity() { return ModConfig.get().revolvers.projectileVelocity; }
    public float getProjectileDivergence() { return ModConfig.get().revolvers.projectileDivergence; }

    /**
     * Per-item camera recoil callbacks, keyed by item registry ID.
     * Invoked on the client at the exact moment of firing, before the next frame renders.
     * ConcurrentHashMap used defensively — registration happens during client init,
     * but the map may be read from the render thread.
     *
     * Addon mods register their own callbacks via registerRecoilCallback() to apply
     * custom recoil behavior for their revolver items without subclassing the callback system.
     */
    private static final Map<ResourceLocation, Consumer<LivingEntity>> RECOIL_CALLBACKS = new ConcurrentHashMap<>();
    /**
     * Per-item muzzle-flash particle callbacks, keyed by item registry ID.
     * Invoked on the client at the exact moment of firing to bypass GeckoLib animation delay.
     * ConcurrentHashMap used defensively — registration happens during client init,
     * but the map may be read from the render thread.
     *
     * Addon mods register their own callbacks via registerParticleCallback() to apply
     * custom particle effects for their revolver items.
     */
    private static final Map<ResourceLocation, Consumer<LivingEntity>> PARTICLE_CALLBACKS = new ConcurrentHashMap<>();

    /**
     * Client-only renderer container. Populated during client initialization.
     * Accessed by ModItemRenderers to bind the GeckoLib renderer for this item.
     */
    private GeoItemRenderer<?> cachedRenderer = null;

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
        RECOIL_CALLBACKS.put(BuiltInRegistries.ITEM.getKey(item), callback);
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
        PARTICLE_CALLBACKS.put(BuiltInRegistries.ITEM.getKey(item), callback);
    }

    private void triggerClientCallbacks(Player user, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Consumer<LivingEntity> recoil = RECOIL_CALLBACKS.get(itemId);
        if (recoil != null) recoil.accept(user);
        Consumer<LivingEntity> particle = PARTICLE_CALLBACKS.get(itemId);
        if (particle != null) particle.accept(user);
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

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                return cachedRenderer;
            }
        });
    }

    @Override
    protected void shoot(@NotNull ServerLevel level, @NotNull LivingEntity shooter, @NotNull InteractionHand hand, @NotNull ItemStack weapon, @NotNull List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit, @Nullable LivingEntity target) {
        // Custom shooting logic is in performShoot()
    }

    @Override
    protected void shootProjectile(@NotNull LivingEntity shooter, @NotNull Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        // Revolvers handle their own projectile shooting in performShoot
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    public boolean isPerspectiveAware() { return true; }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.getItem() instanceof BaseBulletItem;
    }

    @Override
    public int getDefaultProjectileRange() { return 15; }

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
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        return false;
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
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity user, @NotNull ItemStack stack, int remainingUseTicks) {
        int chargeTimerMax = getChargeTimeTicks();
        int elapsed = USE_DURATION - remainingUseTicks;

        int triggerTick = (int) (0.4f * chargeTimerMax);
        if (elapsed == triggerTick && !level.isClientSide) {
            RevolverReloadParticlePacket packet = new RevolverReloadParticlePacket(user.getId());

            if (user instanceof ServerPlayer shooter) {
                PacketDistributor.sendToPlayer(shooter, packet);
            }
            PacketDistributor.sendToPlayersTrackingEntity(user, packet);
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
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (isCharged(stack)) {
            if (!level.isClientSide) {
                performShoot(level, user, hand, stack, getProjectileVelocity(), getProjectileDivergence());
            }

            if (level.isClientSide()) {
                triggerClientCallbacks(user, stack);
            }

            if (!level.isClientSide) {
                PacketDistributor.sendToPlayersTrackingEntity(user, new RevolverFireParticlePacket(user.getId()));
            }

            return InteractionResult.PASS;
        }

        ItemStack ammo = user.getProjectile(stack);
        if (!user.getAbilities().instabuild && ammo.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            triggerAnim(user, instanceId, "controller", "animation.model.reloademptyright");
        }

        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user) {
        return stack;
    }

    /**
     * Called when player releases right-click or gets interrupted.
     * Loads the bullet if charging completed successfully.
     * Stops reload animation if player releases early.
     */
    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user, int remainingUseTicks) {
        int chargeTimerMax = getChargeTimeTicks();
        int chargedTicks = USE_DURATION - remainingUseTicks;

        if (chargedTicks >= chargeTimerMax && !isCharged(stack)) {
            if (loadBullet(user, stack)) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        } else {
            if (!level.isClientSide && user instanceof Player player) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!level.isClientSide && selected && entity instanceof Player player) {
            if (!hasDrawAnimationPlayed(stack)) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
                triggerAnim(player, instanceId, "controller", "animation.model.drawright");
                markDrawAnimationPlayed(stack);
            }
        }

        if (!selected) {
            clearDrawAnimationFlag(stack);
        }
    }

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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(DRAW_PLAYED_KEY, true));
    }

    /**
     * Clears the draw animation flag from this item stack.
     * Protected to allow subclasses to override draw animation tracking behavior.
     */
    protected void clearDrawAnimationFlag(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(DRAW_PLAYED_KEY));
    }

    /**
     * Loads a bullet from the user's inventory into the revolver.
     * Uses vanilla ChargedProjectilesComponent for compatibility.
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
            projectiles.add(new ItemStack(ModItems.STONE_BULLET.get()));
        }

        revolver.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
        return true;
    }

    /**
     * Calculates the barrel tip position server-side,
     * mirroring the third-person fire particle offset in RevolverParticleHandler.
     * This ensures bullets spawn from the visually correct position.
     * Uses constants that match RevolverParticleHandler's TP_FIRE_* offsets.
     * Protected to allow subclasses to override barrel positioning for custom models.
     */
    protected Vec3 calcBarrelPosition(LivingEntity shooter) {
        float pitch   = shooter.getXRot();
        float yaw     = shooter.getYRot();
        float bodyYaw = shooter.yBodyRot;

        double baseX = shooter.getX();
        double baseY = shooter.getY() + 1.45;
        double baseZ = shooter.getZ();

        Vec3 bodyRight = Vec3.directionFromRotation(0, bodyYaw + 90).normalize();
        Vec3 lookFwd   = Vec3.directionFromRotation(pitch, yaw);
        Vec3 lookUp    = Vec3.directionFromRotation(pitch - 90, yaw).normalize();
        Vec3 lookRight = lookFwd.cross(lookUp).normalize();

        Vec3 shoulder = new Vec3(
                baseX + bodyRight.x * BARREL_SHOULDER,
                baseY,
                baseZ + bodyRight.z * BARREL_SHOULDER
        );

        return shoulder
                .add(lookFwd.scale(BARREL_FWD))
                .add(lookRight.scale(BARREL_RIGHT))
                .add(lookUp.scale(BARREL_UP));
    }

    /**
     * Fires the loaded bullet as a projectile entity.
     * Combines revolver base damage with bullet damage.
     * Spawns projectile from calculated barrel position for visual accuracy.
     * Triggers fire animation and plays explosion sound.
     * Protected to allow subclasses to override projectile type, damage scaling,
     * sound, or animation behavior.
     */
    protected void performShoot(Level level, LivingEntity shooter, InteractionHand hand, ItemStack stack,
                                 float velocity, float divergence) {
        if (level.isClientSide) return;

        ChargedProjectiles component = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (component == null || component.isEmpty()) return;

        ItemStack bulletStack = component.getItems().get(0);

        float bulletDamage = 0.0f;
        if (bulletStack.getItem() instanceof BaseBulletItem bulletItem) {
            bulletDamage = bulletItem.getDamage();
        }
        float totalDamage = this.getDamage() + bulletDamage;

        BulletProjectileEntity projectile = new BulletProjectileEntity(
                level,
                shooter,
                bulletStack,
                totalDamage
        );

        Vec3 barrelPos = calcBarrelPosition(shooter);
        projectile.setPos(barrelPos.x, barrelPos.y, barrelPos.z);

        projectile.shootFromRotation(
                shooter,
                shooter.getXRot(),
                shooter.getYRot(),
                0.0f,
                velocity,
                divergence
        );

        level.addFreshEntity(projectile);

        stack.hurtAndBreak(1, shooter, LivingEntity.getSlotForHand(hand));

        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                0.35f, 1.5f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (shooter instanceof Player player) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            triggerAnim(player, instanceId, "controller", "animation.model.fireright");
        }

        stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
    }

    /**
     * Checks if the revolver currently has a loaded bullet.
     */
    public static boolean isCharged(ItemStack stack) {
        ChargedProjectiles component = stack.get(DataComponents.CHARGED_PROJECTILES);
        return component != null && !component.isEmpty();
    }

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

            if (!isFirstPerson) {
                RawAnimation triggered = ctrl.getTriggeredAnimation();
                if (triggered != null && DRAW_ANIM.equals(triggered)) {
                    ctrl.setAnimation(IDLE_ANIM);
                    return PlayState.CONTINUE;
                }
            }

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
