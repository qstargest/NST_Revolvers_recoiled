package org.stargest.nst_revrecoiled.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;

/**
 * Centralized registry for all mod screen handler types.
 * Must be called from the main mod initializer before any screen is opened.
 *
 * The client-side screen binding (HandledScreens.register) is performed separately
 * in Nst_revolvers_recoiledClient to avoid loading client-only classes on the server.
 */
public class ModScreenHandlers {

    /**
     * Screen handler type for the Assembly Table GUI.
     * Null before register() is called; safe to reference afterwards.
     */
    public static ScreenHandlerType<AssemblyTableScreenHandler> ASSEMBLY_TABLE_HANDLER;

    /**
     * Registers all mod screen handler types with the Minecraft registry.
     * Called once during mod initialisation from the main ModInitializer.
     */
    public static void register() {
        ASSEMBLY_TABLE_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Main.MOD_ID, "assembly_table"),
                new ScreenHandlerType<>(AssemblyTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );
    }
}
