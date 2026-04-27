package org.stargest.nst_revrecoiled.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;

/**
 * Utility class for managing custom sound events for the mod.
 * Centralizes registry identifiers and sound event instances for revolvers.
 */
public class ModSounds {
    /** Identifier for the revolver shot sound effect. */
    public static final Identifier SHOT_SOUND_ID = Identifier.of(Main.MOD_ID, "revolver_shot");
    /** Identifier for the revolver reload sound effect. */
    public static final Identifier RELOAD_SOUND_ID = Identifier.of(Main.MOD_ID, "revolver_reload");
    /** Identifier for the revolver draw/equip sound effect. */
    public static final Identifier DRAW_SOUND_ID = Identifier.of(Main.MOD_ID, "revolver_draw");

    /** Sound event instance for revolver shots. */
    public static final SoundEvent SHOT = SoundEvent.of(SHOT_SOUND_ID);
    /** Sound event instance for revolver reloads. */
    public static final SoundEvent RELOAD = SoundEvent.of(RELOAD_SOUND_ID);
    /** Sound event instance for drawing the revolver. */
    public static final SoundEvent DRAW = SoundEvent.of(DRAW_SOUND_ID);

    /**
     * Registers all sound events defined in this class into the Minecraft sound event registry.
     * This ensures the sounds are correctly linked to their identifiers and available for use.
     */
    public static void init() {
        Registry.register(Registries.SOUND_EVENT, SHOT_SOUND_ID, SHOT);
        Registry.register(Registries.SOUND_EVENT, RELOAD_SOUND_ID, RELOAD);
        Registry.register(Registries.SOUND_EVENT, DRAW_SOUND_ID, DRAW);
    }
}
