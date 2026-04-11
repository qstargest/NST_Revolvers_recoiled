package org.stargest.nst_revrecoiled.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;


/**
 * Centralized registry for all mod screen handler types.
 * The client-side screen binding is performed separately
 * in ClientEvents to avoid loading client-only classes on the server.
 */
public class ModScreenHandlers {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, NstRevRecoiled.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblyTableScreenHandler>> ASSEMBLY_TABLE_HANDLER =
            MENU_TYPES.register("assembly_table",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new AssemblyTableScreenHandler(windowId, inv)));
}
