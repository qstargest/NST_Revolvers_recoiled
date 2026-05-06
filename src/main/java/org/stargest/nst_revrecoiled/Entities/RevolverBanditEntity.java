package org.stargest.nst_revrecoiled.Entities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.util.ModConfig;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModSounds;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.EnumSet;
import java.util.List;

/**
 * Custom hostile entity that uses a revolver as its primary weapon.
 * Imitates Pillager-like behavior but with specialized revolver mechanics.
 * Inherits from Raider to support hostile AI patterns.
 *
 * <p>Key features:
 * <ul>
 *   <li>Server-side weapon state machine (idle, charging, pending fire, cooldown)</li>
 *   <li>Synced charge progress for client-side arm animations</li>
 *   <li>Configurable attributes (health, speed, movement) via ModConfig</li>
 * </ul>
 */
public class RevolverBanditEntity extends Raider {

    // -------------------------------------------------------------------------
    // SynchedEntityData — synced to clients for arm animation
    // -------------------------------------------------------------------------

    /** Charging progress 0.0 (start) → 1.0 (fully loaded). */
    private static final EntityDataAccessor<Float> CHARGE_PROGRESS =
            SynchedEntityData.defineId(RevolverBanditEntity.class, EntityDataSerializers.FLOAT);

    /** True while the mob is actively charging the revolver. */
    private static final EntityDataAccessor<Boolean> IS_CHARGING =
            SynchedEntityData.defineId(RevolverBanditEntity.class, EntityDataSerializers.BOOLEAN);

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

    public RevolverBanditEntity(EntityType<? extends RevolverBanditEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Baseline attributes bridging configurable settings.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConfig.get().bandit.maxHealth)
                .add(Attributes.MOVEMENT_SPEED, ModConfig.get().bandit.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, ModConfig.get().bandit.followRange);
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, IronGolem.class, ModConfig.get().bandit.fleeDistanceGolem, 1.0, 1.2));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, ModConfig.get().bandit.fleeDistancePlayer, 1.0, 1.2));
        goalSelector.addGoal(2, new RevolverBanditShootGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE_PROGRESS, 0.0f);
        builder.define(IS_CHARGING, false);
    }

    /**
     * Called on first spawn via the world's entity initializer.
     * Equips the revolver so the mob is always armed.
     */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                  @NotNull DifficultyInstance difficulty,
                                                  @NotNull EntitySpawnReason spawnReason,
                                                  @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);

        ItemStack helmet = getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() == Items.WHITE_BANNER) {
            setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }

        equipRevolver();
        return data;
    }

    /** Overrides MobEntity equipment init to ensure the revolver is always present. */
    @Override
    protected void populateDefaultEquipmentSlots(@NotNull RandomSource random,
                                                 @NotNull DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        equipRevolver();
    }

    protected void equipRevolver() {
        ItemStack held = getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof BaseRevolverItem)) {
            @Nullable Item configItem = BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse(ModConfig.get().bandit.equippedItem)).map(Holder.Reference::value).orElse(null);
            if (configItem instanceof BaseRevolverItem) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(configItem));
            } else {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COBBLESTONE_REVOLVER.get()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tick — weapon handling
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickWeapon();
        }
    }

    protected void tickWeapon() {
        Level level = level();
        ItemStack stack = getMainHandItem();
        if (!(stack.getItem() instanceof BaseRevolverItem revolver)) return;

        // --- State 1: pending fire ---
        if (pendingFireTicks >= 0) {
            if (pendingFireTicks == 0) {
                @Nullable Item ammoItem = BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse(ModConfig.get().bandit.ammoItem)).map(Holder.Reference::value).orElse(null);
                if (!(ammoItem instanceof BaseBulletItem)) ammoItem = ModItems.STONE_BULLET.get();

                ItemStack bulletStack = new ItemStack(ammoItem);
                revolver.performShoot(level, this, InteractionHand.MAIN_HAND, stack, bulletStack,
                        revolver.getProjectileVelocity(), revolver.getProjectileDivergence());
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
                && hasLineOfSight(target)
                && distanceToSqr(target) <= ModConfig.get().bandit.shootRangeSq;

        boolean charged = BaseRevolverItem.isCharged(stack);

        // --- State 3: charged + valid target → fire ---
        if (charged && canAttack) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            revolver.triggerAnim(this, instanceId, "controller", "fire");

            stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

            pendingFireTicks = revolver.getShootDelayTicks();
            postFireCooldown = ModConfig.get().bandit.postFireCooldown;
            chargingTicks = 0;
            updateChargeTracking(0.0f, false);
            return;
        }

        // --- State 4: not charged + valid target → charge ---
        if (!charged && canAttack) {
            if (chargingTicks == 0) {
                long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
                revolver.triggerAnim(this, instanceId, "controller", "reload");
            }

            chargingTicks++;
            float progress = (float) chargingTicks / revolver.getChargeTimeTicks();
            updateChargeTracking(progress, true);

            if (chargingTicks >= revolver.getChargeTimeTicks()) {
                @Nullable Item ammoItem = BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse(ModConfig.get().bandit.ammoItem)).map(Holder.Reference::value).orElse(null);
                if (!(ammoItem instanceof BaseBulletItem)) ammoItem = ModItems.STONE_BULLET.get();

                List<ItemStack> projectiles = List.of(new ItemStack(ammoItem));
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
                level.playSound(null, getX(), getY(), getZ(),
                        ModSounds.RELOAD.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                chargingTicks = 0;
                updateChargeTracking(0.0f, false);
            }
            return;
        }

        // --- State 5: no valid target → reset ---
        if (chargingTicks > 0) {
            long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) level);
            revolver.triggerAnim(this, instanceId, "controller", "idle");
            chargingTicks = 0;
            updateChargeTracking(0.0f, false);
        }
    }

    private void updateChargeTracking(float progress, boolean charging) {
        entityData.set(CHARGE_PROGRESS, progress);
        entityData.set(IS_CHARGING, charging);
    }

    // -------------------------------------------------------------------------
    // Raider requirements
    // -------------------------------------------------------------------------

    @Override
    public void applyRaidBuffs(@NotNull ServerLevel level, int wave, boolean unused) {
        // No bonuses needed
    }

    @Override
    public @NotNull SoundEvent getCelebrateSound() {
        return SoundEvents.PILLAGER_CELEBRATE;
    }

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    // -------------------------------------------------------------------------
    // Public client-readable state
    // -------------------------------------------------------------------------

    /** Returns the current reload progress [0.0, 1.0] for arm animation. */
    public float getChargeProgress() {
        return entityData.get(CHARGE_PROGRESS);
    }

    /**
     * True while the mob is reloading.
     * Read by the client-side arm poser to show the reload animation pose.
     */
    public boolean isChargingRevolver() {
        return entityData.get(IS_CHARGING);
    }

    /**
     * Resets all weapon state.
     * Called from the shoot goal's {@code stop()} so state is clean when
     * the goal is interrupted (e.g. target dies or walks out of range).
     */
    public void stopCharging() {
        if (!level().isClientSide) {
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
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = bandit.getTarget();
            return target != null && target.isAlive() && !target.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            bandit.stopCharging();
        }

        @Override
        public void tick() {
            LivingEntity target = bandit.getTarget();
            if (target == null) return;

            double distSq = bandit.distanceToSqr(target);
            boolean inRange = distSq <= ModConfig.get().bandit.shootRangeSq;
            boolean hasLos = bandit.hasLineOfSight(target);

            if (inRange && hasLos) {
                bandit.getNavigation().stop();
            } else {
                bandit.getNavigation().moveTo(target, 0.8);
            }

            bandit.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
    }
}
