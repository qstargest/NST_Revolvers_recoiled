package org.stargest.nst_revrecoiled.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Handles loading and saving the JSON configuration file using GSON.
 * Files are stored in config/nst_revrecoiled/ inside the game directory.
 *
 * Adapted for Forge 1.20.1.
 */
public class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.MODID);
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
        ModConfig config = new ModConfig();

        File sFile = getConfigFile(SETTINGS_FILE);
        if (sFile.exists()) {
            try (FileReader reader = new FileReader(sFile)) {
                SettingsContainer sc = GSON.fromJson(reader, SettingsContainer.class);
                if (sc != null) {
                    if (sc.revolvers != null) config.revolvers = sc.revolvers;
                    if (sc.bullets   != null) config.bullets   = sc.bullets;
                    if (sc.worldGen  != null) config.worldGen  = sc.worldGen;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load settings.json", e);
            }
        }

        File rFile = getConfigFile(RECIPES_FILE);
        if (rFile.exists()) {
            try (FileReader reader = new FileReader(rFile)) {
               Type type = new com.google.gson.reflect.TypeToken<
                       Map<String, List<ModConfig.IngredientConfig>>>() {}.getType();
                Map<String, List<ModConfig.IngredientConfig>> recipes =
                        GSON.fromJson(reader, type);
                if (recipes != null) config.recipes = recipes;
            } catch (IOException e) {
                LOGGER.error("Failed to load recipes.json", e);
            }
        }

        File vFile = getConfigFile(VISUALS_FILE);
        if (vFile.exists()) {
            try (FileReader reader = new FileReader(vFile)) {
                ModConfig.VisualsConfig visuals = GSON.fromJson(reader, ModConfig.VisualsConfig.class);
                if (visuals != null) config.visuals = visuals;
            } catch (IOException e) {
                LOGGER.error("Failed to load visuals.json", e);
            }
        }

        ModConfig.set(config);
        save();
    }

    /**
     * Saves the current configuration to three separate files.
     */
    public static void save() {
        ModConfig config = ModConfig.get();

        File dir = getConfigDir().toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.error("Failed to create config directory: {}", dir.getAbsolutePath());
            return;
        }

        try (FileWriter w = new FileWriter(getConfigFile(SETTINGS_FILE))) {
            SettingsContainer sc = new SettingsContainer();
            sc.revolvers = config.revolvers;
            sc.bullets   = config.bullets;
            sc.worldGen  = config.worldGen;
            GSON.toJson(sc, w);
        } catch (IOException e) { LOGGER.error("Failed to save settings.json", e); }

        try (FileWriter w = new FileWriter(getConfigFile(RECIPES_FILE))) {
            GSON.toJson(config.recipes, w);
        } catch (IOException e) { LOGGER.error("Failed to save recipes.json", e); }

        try (FileWriter w = new FileWriter(getConfigFile(VISUALS_FILE))) {
            GSON.toJson(config.visuals, w);
        } catch (IOException e) { LOGGER.error("Failed to save visuals.json", e); }
    }

    private static class SettingsContainer {
        public ModConfig.RevolverConfig  revolvers;
        public ModConfig.BulletConfig    bullets;
        public ModConfig.WorldGenConfig  worldGen;
    }

    private static Path getConfigDir() {
        // Forge config folder lives in <gamedir>/config
        return FMLPaths.GAMEDIR.get().resolve("config").resolve(CONFIG_DIR_NAME);
    }

    /**
     * @param fileName Name of the file in the config/nst_revrecoiled directory.
     * @return The File object.
     */
    private static File getConfigFile(String fileName) {
        return getConfigDir().resolve(fileName).toFile();
    }

    /**
     * @return Current configuration serialized to JSON string for network sync.
     */
    public static String toJson() {
        return GSON.toJson(ModConfig.get());
    }
}
