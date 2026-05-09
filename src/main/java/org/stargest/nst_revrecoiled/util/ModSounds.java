package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Main;

/**
 * Utility class for managing custom sound events for the mod.
 * Uses NeoForge DeferredRegister for safe registration.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Main.MODID);

    /** Sound event instance for revolver shots. */
    public static final RegistryObject<SoundEvent> SHOT = registerSoundEvent("revolver_shot");
    /** Sound event instance for revolver reloads. */
    public static final RegistryObject<SoundEvent> RELOAD = registerSoundEvent("revolver_reload");
    /** Sound event instance for drawing the revolver. */
    public static final RegistryObject<SoundEvent> DRAW = registerSoundEvent("revolver_draw");

    /**
     * Registers a SoundEvent with the given name.
     *
     * @param name the name of the sound event
     * @return a DeferredHolder containing the registered SoundEvent
     */
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Main.MODID, name)));
    }
}
