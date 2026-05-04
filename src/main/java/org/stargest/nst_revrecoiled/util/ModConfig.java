package org.stargest.nst_revrecoiled.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration data holder for the mod.
 * Uses a singleton pattern with a volatile reference for thread-safe access
 * across the Minecraft Server Thread and other threads.
 */
public class ModConfig {

    private static volatile ModConfig INSTANCE = new ModConfig();

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void set(ModConfig config) {
        INSTANCE = config;
    }

    public RevolverConfig revolvers = new RevolverConfig();
    public BulletConfig bullets = new BulletConfig();
    public WorldGenConfig worldGen = new WorldGenConfig();
    public VisualsConfig visuals = new VisualsConfig();
    public BanditConfig bandit = new BanditConfig();
    public Map<String, List<IngredientConfig>> recipes = new HashMap<>();

    public ModConfig() {
        // Initialize default recipes

        // Cobblestone Revolver
        recipes.put("nst_revrecoiled:cobblestone_revolver", List.of(
                new IngredientConfig("minecraft:cobblestone", 16),
                new IngredientConfig("minecraft:spruce_planks", 2),
                new IngredientConfig("minecraft:gunpowder", 3)
        ));

        // Iron Revolver
        recipes.put("nst_revrecoiled:iron_revolver", List.of(
                new IngredientConfig("minecraft:iron_ingot", 10),
                new IngredientConfig("minecraft:spruce_planks", 2),
                new IngredientConfig("minecraft:gunpowder", 6)
        ));

        // Golden Revolver
        recipes.put("nst_revrecoiled:golden_revolver", List.of(
                new IngredientConfig("minecraft:gold_ingot", 14),
                new IngredientConfig("minecraft:spruce_planks", 2),
                new IngredientConfig("minecraft:gunpowder", 8)
        ));

        // Diamond Revolver
        recipes.put("nst_revrecoiled:diamond_revolver", List.of(
                new IngredientConfig("minecraft:diamond", 5),
                new IngredientConfig("minecraft:spruce_planks", 2),
                new IngredientConfig("minecraft:gunpowder", 12)
        ));

        // Stone Bullet
        recipes.put("nst_revrecoiled:stone_bullet", List.of(
                new IngredientConfig("minecraft:cobblestone", 1),
                new IngredientConfig("minecraft:gunpowder", 1)
        ));

        // Iron Bullet
        recipes.put("nst_revrecoiled:iron_bullet", List.of(
                new IngredientConfig("minecraft:iron_ingot", 1),
                new IngredientConfig("minecraft:gunpowder", 1)
        ));

        // Golden Bullet
        recipes.put("nst_revrecoiled:golden_bullet", List.of(
                new IngredientConfig("minecraft:gold_ingot", 1),
                new IngredientConfig("minecraft:gunpowder", 1)
        ));

        // Diamond Bullet
        recipes.put("nst_revrecoiled:diamond_bullet", List.of(
                new IngredientConfig("minecraft:diamond", 1),
                new IngredientConfig("minecraft:gunpowder", 1)
        ));
    }

    /**
     * Ensures all loaded configuration values are within sane ranges.
     * Prevents division by zero, negative health, and other logic errors.
     */
    public void validate() {
        if (revolvers == null) revolvers = new RevolverConfig();
        revolvers.validate();

        if (bullets == null) bullets = new BulletConfig();
        bullets.validate();

        if (worldGen == null) worldGen = new WorldGenConfig();
        worldGen.validate();

        if (visuals == null) visuals = new VisualsConfig();
        visuals.validate();

        if (bandit == null) bandit = new BanditConfig();
        bandit.validate();

        if (recipes == null) {
            recipes = new HashMap<>();
        } else {
            // Ensure recipe counts are positive
            recipes.values().forEach(list -> {
                if (list != null) list.forEach(IngredientConfig::validate);
            });
        }
    }

    public static class VisualsConfig {
        public String _comment_enablePlayerAimTracking = "Whether to track where the player is aiming with the revolver (arm movement). Default: true";
        public boolean enablePlayerAimTracking = true;

        public String _comment_enableBanditAimTracking = "Whether to track where the bandit is aiming (arm movement). Default: true";
        public boolean enableBanditAimTracking = true;

        public RecoilConfig recoil = new RecoilConfig();

        public void validate() {
            if (recoil == null) recoil = new RecoilConfig();
            recoil.validate();
        }

        public static class RecoilConfig {
            public String _comment_enabled = "Enable camera recoil when firing. Default: true";
            public boolean enabled = true;

            public String _comment_pitch = "Upward camera kick in degrees. Recommended: 0 to 10. Default: 4.5";
            public float pitch = 4.5f;

            public String _comment_yawVariance = "Maximum horizontal camera shake in degrees. Recommended: 0 to 5. Default: 1.5";
            public float yawVariance = 1.5f;

            public String _comment_kickDuration = "Time in seconds to reach peak recoil. MUST be greater than 0. Default: 0.1";
            public float kickDuration = 0.1f;

            public String _comment_recoveryDuration = "Time in seconds to return to normal view. MUST be greater than 0. Default: 0.45";
            public float recoveryDuration = 0.45f;

            public void validate() {
                if (pitch < 0) pitch = 4.5f;
                if (yawVariance < 0) yawVariance = 1.5f;
                if (kickDuration <= 0) kickDuration = 0.1f;
                if (recoveryDuration <= 0) recoveryDuration = 0.45f;
            }
        }
    }

    public static class RevolverConfig {
        public String _comment_chargeTimeTicks = "Time in ticks (20 ticks = 1 sec) required to fully charge the revolver. Recommended: 10 to 100. Default: 50";
        public int chargeTimeTicks = 50;

        public String _comment_projectileVelocity = "Initial velocity of the fired projectile. Recommended: 1 to 10. Default: 6.0";
        public float projectileVelocity = 6.0f;

        public String _comment_projectileDivergence = "Projectile spread (0.0 is perfect accuracy). Recommended: 0 to 1. Default: 0.2";
        public float projectileDivergence = 0.2f;

        public void validate() {
            if (chargeTimeTicks <= 0) chargeTimeTicks = 50;
            if (projectileVelocity <= 0) projectileVelocity = 6.0f;
            if (projectileDivergence < 0) projectileDivergence = 0.2f;

            if (stats == null) {
                stats = new HashMap<>();
            } else {
                stats.values().forEach(RevolverStats::validate);
            }
        }

        public Map<String, RevolverStats> stats = new HashMap<>();

        public RevolverConfig() {
            // Default stats for vanilla tiered revolvers
            stats.put("cobblestone_revolver", new RevolverStats(4.0f, 131));
            stats.put("iron_revolver", new RevolverStats(5.0f, 250));
            stats.put("golden_revolver", new RevolverStats(6.0f, 32));
            stats.put("diamond_revolver", new RevolverStats(7.0f, 1561));
        }
    }

    public static class RevolverStats {
        public String _comment_damage = "Base damage of the revolver. Default: varies by tier (4.0 to 7.0)";
        public float damage;

        public String _comment_durability = "Maximum durability. Default: varies by tier (32 to 1561)";
        public int durability;

        public RevolverStats(float damage, int durability) {
            this.damage = damage;
            this.durability = durability;
        }

        public void validate() {
            if (damage < 0) damage = 4.0f;
            if (durability <= 0) durability = 100;
        }
    }

    public static class BulletConfig {
        public String _comment_maxAgeTicks = "Maximum lifetime of the bullet in ticks (how long it flies before despawning). Recommended: 10 to 100. Default: 20";
        public int maxAgeTicks = 20;

        public String _comment_gravity = "Gravity applied to the bullet (0.0 to 1.0). Higher value makes it drop faster. Default: 0.15";
        public float gravity = 0.15f;

        public void validate() {
            if (maxAgeTicks <= 0) maxAgeTicks = 20;
            if (gravity < 0) gravity = 0.15f;

            if (damage == null) {
                damage = new HashMap<>();
            } else {
                damage.entrySet().forEach(entry -> {
                    if (entry.getValue() < 0) entry.setValue(2.0f);
                });
            }
        }

        public Map<String, Float> damage = new HashMap<>();

        public BulletConfig() {
            // Default damage for vanilla tiered bullets
            damage.put("stone_bullet", 2.0f);
            damage.put("iron_bullet", 4.0f);
            damage.put("golden_bullet", 3.0f);
            damage.put("diamond_bullet", 6.0f);
        }
    }

    public static class WorldGenConfig {
        public String _comment_spawnRevolvermakerHouse = "Whether to generate the revolvermaker house in villages. Default: true";
        public boolean spawnRevolvermakerHouse = true;

        public String _comment_houseWeight = "Relative weight of the house in village generation. 0 to 100. Default: 5";
        public int houseWeight = 5;

        public void validate() {
            if (houseWeight < 0) houseWeight = 5;
        }
    }

    public static class BanditConfig {
        public String _comment_maxHealth = "Maximum health of the bandit. Default: 24.0";
        public float maxHealth = 24.0f;

        public String _comment_movementSpeed = "Base movement speed. Default: 0.35";
        public float movementSpeed = 0.35f;

        public String _comment_followRange = "Sight and tracking range in blocks. Default: 24.0";
        public float followRange = 24.0f;

        public String _comment_postFireCooldown = "Delay between shots in ticks. Default: 60";
        public int postFireCooldown = 60;

        public String _comment_fleeDistanceGolem = "Distance at which the bandit flees from Iron Golems. Default: 1.5";
        public float fleeDistanceGolem = 1.5f;

        public String _comment_fleeDistancePlayer = "Distance at which the bandit flees from Players. Default: 3.0";
        public float fleeDistancePlayer = 3.0f;

        public String _comment_shootRangeSq = "Squared max shooting distance (e.g. 256.0 is 16 blocks). Default: 256.0";
        public double shootRangeSq = 256.0;

        public String _comment_equippedItem = "Registry ID of the bandit's weapon. Default: nst_revrecoiled:cobblestone_revolver. Can only accept revolvers from mod or it's addons";
        public String equippedItem = "nst_revrecoiled:cobblestone_revolver";

        public String _comment_ammoItem = "Registry ID of the bandit's ammo. Default: nst_revrecoiled:stone_bullet. Can only accept bullets from mod or it's addons";
        public String ammoItem = "nst_revrecoiled:stone_bullet";

        public void validate() {
            if (maxHealth <= 0) maxHealth = 24.0f;
            if (movementSpeed <= 0) movementSpeed = 0.35f;
            if (followRange <= 0) followRange = 24.0f;
            if (postFireCooldown < 0) postFireCooldown = 60;
            if (fleeDistanceGolem < 0) fleeDistanceGolem = 1.5f;
            if (fleeDistancePlayer < 0) fleeDistancePlayer = 3.0f;
            if (shootRangeSq <= 0) shootRangeSq = 256.0;

            if (spawn == null) spawn = new SpawnConfig();
            spawn.validate();
        }

        public SpawnConfig spawn = new SpawnConfig();

        public static class SpawnConfig {
            public String _comment_spawnInPatrols = "Allow bandits to spawn in Pillager Patrols. Default: true";
            public boolean spawnInPatrols = true;

            public String _comment_patrolSpawnChance = "Chance (0.0 to 1.0) for a patrol member to be a bandit. Default: 0.2";
            public float patrolSpawnChance = 0.2f;

            public String _comment_spawnInOutposts = "Allow bandits to spawn in Pillager Outposts. Default: true";
            public boolean spawnInOutposts = true;

            public String _comment_outpostSpawnWeight = "Relative weight in Outposts. 1 = rare, 10 = 50/50 with pillagers. Default: 4";
            public int outpostSpawnWeight = 4;

            public void validate() {
                if (patrolSpawnChance < 0 || patrolSpawnChance > 1) patrolSpawnChance = 0.2f;
                if (outpostSpawnWeight < 0) outpostSpawnWeight = 4;
            }
        }
    }

    public static class IngredientConfig {
        public String _comment_item = "Registry ID of the item (e.g. 'minecraft:iron_ingot').";
        public String item;

        public String _comment_count = "Amount required. Default: 1";
        public int count;

        public IngredientConfig(String item, int count) {
            this.item = item;
            this.count = count;
        }

        public void validate() {
            if (item == null || item.isEmpty()) item = "minecraft:air";
            if (count <= 0) count = 1;
        }
    }
}
