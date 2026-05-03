package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModSounds;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.EnumSet;
import java.util.List;


/**
 * Custom hostile entity that uses a revolver as its primary weapon.
 * Imitates Pillager-like behavior but with specialized revolver mechanics.
 * Inherits from RaiderEntity to support hostile AI patterns.
 *
 * Key features:
 * - Server-side weapon state machine (idle, charging, pending fire, cooldown)
 * - Synced charge progress for client-side arm animations
 * - Configurable attributes (health, speed, movement) via ModConfig
 */
public class RevolverBanditEntity extends RaiderEntity {

    // -------------------------------------------------------------------------
    // TrackedData — synced to clients for arm animation
    // -------------------------------------------------------------------------

    /** Charging progress 0.0 (start) → 1.0 (fully loaded). */
    private static final TrackedData<Float> CHARGE_PROGRESS =
            DataTracker.registerData(RevolverBanditEntity.class, TrackedDataHandlerRegistry.FLOAT);

    /** True while the mob is actively charging the revolver. */
    private static final TrackedData<Boolean> IS_CHARGING =
            DataTracker.registerData(RevolverBanditEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // -------------------------------------------------------------------------
    // Server-side weapon state (not synced)
    // -------------------------------------------------------------------------

    /** Ticks spent charging on the current reload cycle. */
    private int chargingTicks = 0;

    /**
     * Counts down after triggering the fire animation.
     * While >= 0 a shot is pending; -1 means no pending shot.
     */
    private int pendingFireTicks = -1;

    private int postFireCooldown = 0;

    // -------------------------------------------------------------------------
    // Constructor / baseline attributes
    // -------------------------------------------------------------------------

    public RevolverBanditEntity(EntityType<? extends RevolverBanditEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Baseline attributes bridging configurable settings.
     */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, ModConfig.get().bandit.maxHealth)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, ModConfig.get().bandit.movementSpeed)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, ModConfig.get().bandit.followRange);
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        // Minimal distance keeping to prevent hitbox overlapping at point-blank range
        goalSelector.add(1, new FleeEntityGoal<>(this, IronGolemEntity.class, ModConfig.get().bandit.fleeDistanceGolem, 1.0, 1.2));
        goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, ModConfig.get().bandit.fleeDistancePlayer, 1.0, 1.2));

        goalSelector.add(2, new RevolverBanditShootGoal(this));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(7, new LookAroundGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, VillagerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CHARGE_PROGRESS, 0.0f);
        builder.add(IS_CHARGING, false);
    }

    /**
     * Called on first spawn via the world's entity initializer.
     * Equips the revolver so the mob is always armed.
     */
    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                           SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);

        ItemStack helmet = getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == Items.WHITE_BANNER) {
            equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }

        equipRevolver();
        return data;
    }

    /** Overrides MobEntity equipment init to ensure the revolver is always present. */
    @Override
    protected void initEquipment(net.minecraft.util.math.random.Random random,
                                 LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        equipRevolver();
    }

    protected void equipRevolver() {
        ItemStack held = getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof BaseRevolverItem)) {
            Item configItem = Registries.ITEM.get(Identifier.of(ModConfig.get().bandit.equippedItem));
            if (configItem instanceof BaseRevolverItem) {
                equipStack(EquipmentSlot.MAINHAND, new ItemStack(configItem));
            } else {
                equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COBBLESTONE_REVOLVER));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tick — weapon handling
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            tickWeapon();
        }
    }


    protected void tickWeapon() {
        World world = getWorld();
        ItemStack stack = getMainHandStack();
        if (!(stack.getItem() instanceof BaseRevolverItem revolver)) return;

        // --- State 1: pending fire ---
        if (pendingFireTicks >= 0) {
            if (pendingFireTicks == 0) {
                Item ammoItem = Registries.ITEM.get(Identifier.of(ModConfig.get().bandit.ammoItem));
                if (!(ammoItem instanceof BaseBulletItem)) ammoItem = ModItems.STONE_BULLET;

                ItemStack bulletStack = new ItemStack(ammoItem);
                revolver.performShoot(
                        world, this, Hand.MAIN_HAND, stack, bulletStack,
                        revolver.getProjectileVelocity(), revolver.getProjectileDivergence()
                );
            }
            pendingFireTicks--;
            return;
        }

        // --- State 2: post-fire cooldown ---
        if (postFireCooldown > 0) {
            postFireCooldown--;
            return;
        }

        LivingEntity target = getTarget();
        boolean canAttack = target != null
                && target.isAlive()
                && canSee(target)
                && squaredDistanceTo(target) <= 256.0; // 16 blocks

        boolean charged = BaseRevolverItem.isCharged(stack);

        // --- State 3: charged + valid target → fire ---
        if (charged && canAttack) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            revolver.triggerAnim(this, instanceId, "controller", "fire");

            // Clear the loaded bullet; a fresh bullet will be used for the shot
            stack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);

            pendingFireTicks = revolver.getShootDelayTicks();
            postFireCooldown = ModConfig.get().bandit.postFireCooldown;
            chargingTicks = 0;
            updateChargeTracking(0.0f, false);
            return;
        }

        // --- State 4: not charged + valid target → charge ---
        if (!charged && canAttack) {
            if (chargingTicks == 0) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
                revolver.triggerAnim(this, instanceId, "controller", "reload");
            }

            chargingTicks++;
            float progress = (float) chargingTicks / revolver.getChargeTimeTicks();
            updateChargeTracking(progress, true);

            if (chargingTicks >= revolver.getChargeTimeTicks()) {
                Item ammoItem = Registries.ITEM.get(Identifier.of(ModConfig.get().bandit.ammoItem));
                if (!(ammoItem instanceof BaseBulletItem)) ammoItem = ModItems.STONE_BULLET;

                List<ItemStack> projectiles = List.of(new ItemStack(ammoItem));
                stack.set(DataComponentTypes.CHARGED_PROJECTILES,
                        ChargedProjectilesComponent.of(projectiles));
                world.playSound(null, getX(), getY(), getZ(),
                        ModSounds.RELOAD, SoundCategory.HOSTILE, 1.0f, 1.0f);
                chargingTicks = 0;
                updateChargeTracking(0.0f, false);
            }
            return;
        }

        // --- State 5: no valid target → reset ---
        if (chargingTicks > 0) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) world);
            revolver.triggerAnim(this, instanceId, "controller", "idle");
            chargingTicks = 0;
            updateChargeTracking(0.0f, false);
        }
    }

    private void updateChargeTracking(float progress, boolean charging) {
        dataTracker.set(CHARGE_PROGRESS, progress);
        dataTracker.set(IS_CHARGING, charging);
    }

    // -------------------------------------------------------------------------
    // RaiderEntity Requirements
    // -------------------------------------------------------------------------

    @Override
    public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
        // No bonuses needed for now
    }

    @Override
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_PILLAGER_CELEBRATE;
    }

    @Override
    public boolean canJoinRaid() {
        return false; // User requested no raid participation
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PILLAGER_DEATH;
    }

    // -------------------------------------------------------------------------
    // Public client-readable state
    // -------------------------------------------------------------------------


    public float getChargeProgress() {
        return dataTracker.get(CHARGE_PROGRESS);
    }

    /**
     * True while the mob is reloading.
     * Read by the client-side arm poser to show the reload animation pose.
     */
    public boolean isChargingRevolver() {
        return dataTracker.get(IS_CHARGING);
    }

    /**
     * Resets all weapon state.
     * Called from the shoot goal's {@code stop()} so state is clean when
     * the goal is interrupted (e.g. target dies or walks out of range).
     */
    public void stopCharging() {
        if (!getWorld().isClient) {
            chargingTicks = 0;
            pendingFireTicks = -1;
            postFireCooldown = 0;
            updateChargeTracking(0.0f, false);
        }
    }

    // -------------------------------------------------------------------------
    // Shoot goal
    // -------------------------------------------------------------------------


    protected static class RevolverBanditShootGoal extends Goal {

        private final RevolverBanditEntity bandit;

        RevolverBanditShootGoal(RevolverBanditEntity bandit) {
            this.bandit = bandit;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = bandit.getTarget();
            return target != null && target.isAlive() && !target.isSpectator();
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void stop() {
            bandit.stopCharging();
        }

        @Override
        public void tick() {
            LivingEntity target = bandit.getTarget();
            if (target == null) return;

            double shootRangeSq = ModConfig.get().bandit.shootRangeSq;
            double distSq = bandit.squaredDistanceTo(target);
            boolean inRange = distSq <= shootRangeSq;
            boolean hasLos = bandit.canSee(target);

            if (inRange && hasLos) {
                bandit.getNavigation().stop();
            } else {
                bandit.getNavigation().startMovingTo(target, 0.8);
            }

            bandit.getLookControl().lookAt(target, 30.0f, 30.0f);
        }
    }
}
