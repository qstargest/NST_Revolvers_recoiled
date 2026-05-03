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
    public BanditConfig bandit = new BanditConfig();
    public VisualsConfig visuals = new VisualsConfig();
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

    public static class VisualsConfig {
        public boolean enablePlayerAimTracking = true;
        public boolean enableBanditAimTracking = true;
        public RecoilConfig recoil = new RecoilConfig();

        public static class RecoilConfig {
            public boolean enabled = true;
            public float pitch = 4.5f;
            public float yawVariance = 1.5f;
            public float kickDuration = 0.1f;
            public float recoveryDuration = 0.45f;
        }
    }

    public static class RevolverConfig {
        public String _comment_chargeTimeTicks = "Time in ticks (20 ticks = 1 sec) required to fully charge the revolver.";
        public int chargeTimeTicks = 50;

        public String _comment_projectileVelocity = "Initial velocity of the fired projectile.";
        public float projectileVelocity = 6.0f;

        public String _comment_projectileDivergence = "Projectile spread when firing (0.0 is perfect accuracy).";
        public float projectileDivergence = 0.2f;

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
        public float damage;
        public int durability;

        public RevolverStats(float damage, int durability) {
            this.damage = damage;
            this.durability = durability;
        }
    }

    public static class BulletConfig {
        public String _comment_maxAgeTicks = "Maximum lifetime of the bullet in ticks (how long it flies before despawning).";
        public int maxAgeTicks = 20;

        public String _comment_gravity = "Gravity applied to the bullet (higher value makes it drop faster).";
        public float gravity = 0.15f;

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
        public boolean spawnRevolvermakerHouse = true;
        public String _comment_houseWeight = "Spawn chance/weight of the revolvermaker house in villages (higher number = more frequent).";
        public int houseWeight = 5;
    }

    public static class BanditConfig {
        public float maxHealth = 24.0f;

        public String _comment_movementSpeed = "Base movement speed of the bandit.";
        public float movementSpeed = 0.35f;

        public String _comment_followRange = "Sight and tracking range of the bandit in blocks.";
        public float followRange = 24.0f;

        public String _comment_postFireCooldown = "Delay before the next shot in ticks (60 ticks = 3 sec).";
        public int postFireCooldown = 60;

        public String _comment_fleeDistanceGolem = "Distance in blocks at which the bandit will start fleeing from an Iron Golem.";
        public float fleeDistanceGolem = 1.5f;

        public String _comment_fleeDistancePlayer = "Distance in blocks at which the bandit will start fleeing from a Player.";
        public float fleeDistancePlayer = 3.0f;

        public String _comment_shootRangeSq = "Squared maximum shooting distance (e.g. 256.0 is 16 blocks).";
        public double shootRangeSq = 256.0;

        public String _comment_equippedItem = "Weapon the bandit spawns with. Must inherit from BaseRevolverItem. Defaults to cobblestone revolver.";
        public String equippedItem = "nst_revrecoiled:cobblestone_revolver";

        public String _comment_ammoItem = "Ammunition the bandit shoots. Must inherit from BaseBulletItem. Defaults to stone bullet.";
        public String ammoItem = "nst_revrecoiled:stone_bullet";
    }

    public static class IngredientConfig {
        public String item;
        public int count;

        public IngredientConfig(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
