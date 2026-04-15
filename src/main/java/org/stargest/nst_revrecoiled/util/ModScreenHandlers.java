package org.stargest.nst_revrecoiled.util;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;

/**
 * Centralized registry for all mod screen handler types.
 * The client-side screen binding is performed separately
 * in ClientEvents to avoid loading client-only classes on the server.
 * Adapted for Forge 1.20.1.
 */
public class ModScreenHandlers {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MODID);

    public static final RegistryObject<MenuType<AssemblyTableScreenHandler>> ASSEMBLY_TABLE_HANDLER =
            MENU_TYPES.register("assembly_table",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new AssemblyTableScreenHandler(windowId, inv)));
}
