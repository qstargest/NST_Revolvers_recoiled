package org.stargest.nst_revrecoiled.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.stargest.nst_revrecoiled.Main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Handles loading and saving the JSON configuration file using GSON.
 * Files are stored in the standard Minecraft 'config' directory.
 */
public class ConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR_NAME = "nst_revrecoiled";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String RECIPES_FILE = "recipes.json";
    private static final String VISUALS_FILE = "visuals.json";

    /**
     * Loads the configuration from disk, splitting it into three files.
     * If any file does not exist, defaults are used and the file is created.
     */
    public static void load() {
        ModConfig config = new ModConfig(); // Start with defaults

        // 1. Load Settings (revolvers, bullets, worldgen)
        File sFile = getConfigFile(SETTINGS_FILE);
        if (sFile.exists()) {
            try (FileReader reader = new FileReader(sFile)) {
                SettingsContainer sc = GSON.fromJson(reader, SettingsContainer.class);
                if (sc != null) {
                    if (sc.revolvers != null) config.revolvers = sc.revolvers;
                    if (sc.bullets != null) config.bullets = sc.bullets;
                    if (sc.worldGen != null) config.worldGen = sc.worldGen;
                    if (sc.bandit != null) config.bandit = sc.bandit;
                }
            } catch (IOException e) {
                Main.LOGGER.error("Failed to load settings.json", e);
            }
        }

        // 2. Load Recipes
        File rFile = getConfigFile(RECIPES_FILE);
        if (rFile.exists()) {
            try (FileReader reader = new FileReader(rFile)) {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, java.util.List<ModConfig.IngredientConfig>>>(){}.getType();
                java.util.Map<String, java.util.List<ModConfig.IngredientConfig>> recipes = GSON.fromJson(reader, type);
                if (recipes != null) config.recipes = recipes;
            } catch (IOException e) {
                Main.LOGGER.error("Failed to load recipes.json", e);
            }
        }

        // 3. Load Visuals
        File vFile = getConfigFile(VISUALS_FILE);
        if (vFile.exists()) {
            try (FileReader reader = new FileReader(vFile)) {
                ModConfig.VisualsConfig visuals = GSON.fromJson(reader, ModConfig.VisualsConfig.class);
                if (visuals != null) config.visuals = visuals;
            } catch (IOException e) {
                Main.LOGGER.error("Failed to load visuals.json", e);
            }
        }

        ModConfig.set(config);

        // Save back to ensure all files exist and are up to date with defaults
        save();
    }

    /**
     * Saves the current configuration to three separate files.
     */
    public static void save() {
        ModConfig config = ModConfig.get();

        // Ensure directory exists
        File dir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Main.LOGGER.error("Failed to create config directory: {}", dir.getAbsolutePath());
            return;
        }

        // 1. Save Settings
        try (FileWriter writer = new FileWriter(getConfigFile(SETTINGS_FILE))) {
            SettingsContainer sc = new SettingsContainer();
            sc.revolvers = config.revolvers;
            sc.bullets = config.bullets;
            sc.worldGen = config.worldGen;
            sc.bandit = config.bandit;
            GSON.toJson(sc, writer);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save settings.json", e);
        }

        // 2. Save Recipes
        try (FileWriter writer = new FileWriter(getConfigFile(RECIPES_FILE))) {
            GSON.toJson(config.recipes, writer);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save recipes.json", e);
        }

        // 3. Save Visuals
        try (FileWriter writer = new FileWriter(getConfigFile(VISUALS_FILE))) {
            GSON.toJson(config.visuals, writer);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save visuals.json", e);
        }
    }

    /**
     * Helper to bundle core settings for JSON serialization.
     */
    private static class SettingsContainer {
        public ModConfig.RevolverConfig revolvers;
        public ModConfig.BulletConfig bullets;
        public ModConfig.WorldGenConfig worldGen;
        public ModConfig.BanditConfig bandit;
    }

    /**
     * @param fileName Name of the file in the config/nst_revrecoiled directory.
     * @return The File object.
     */
    private static File getConfigFile(String fileName) {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
        return configDir.resolve(fileName).toFile();
    }

    /**
     * @return Current configuration serialized to JSON string for network sync.
     */
    public static String toJson() {
        return GSON.toJson(ModConfig.get());
    }
}
