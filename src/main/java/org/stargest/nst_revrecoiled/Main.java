package org.stargest.nst_revrecoiled;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.stargest.nst_revrecoiled.Structures.ModStructures;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.network.PacketHandler;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.util.*;

/**
 * Main Forge mod initialization class.
 * Sets up mod event buses, registers packets via PacketHandler, configuration, and all registries
 * including items, entities, generic blocks, and custom structures.
 *
 * Adapted for Forge 1.20.1.
 */
@Mod(Main.MODID)
public class Main {

    public static final String MODID = "nst_revrecoiled";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Load configuration
        ConfigLoader.load();

        // Register DeferredRegisters
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModScreenHandlers.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(ModItems::buildCreativeTab);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GameEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    /**
     * Explicitly registers POIs and Professions using RegisterEvent to ensure correct binding order.
     */
    private void onRegister(RegisterEvent event) {
        event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> {
            var states = ImmutableSet.copyOf(ModBlocks.ASSEMBLY_TABLE.get().getStateDefinition().getPossibleStates());
            helper.register(ModVillagers.ASSEMBLY_TABLE_POI_KEY.location(), new PoiType(states, 1, 1));
        });

        event.register(Registries.VILLAGER_PROFESSION, helper -> helper.register(ModVillagers.REVOLVERMAKER_KEY.location(), new VillagerProfession(
            "revolvermaker",
            (Holder<PoiType> h) -> h.is(ModVillagers.ASSEMBLY_TABLE_POI_KEY),
            (Holder<PoiType> h) -> h.is(ModVillagers.ASSEMBLY_TABLE_POI_KEY),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_ARMORER
        )));
    }

    /**
     * Container for Forge game-bus events including server lifecycle, commands, and player login.
     */
    public static class GameEvents {
        @SubscribeEvent
        public void onServerStarting(ServerStartingEvent event) {
            if (!AssemblyRecipes.isFrozen()) {
                AssemblyRecipes.freeze();
            }
            ConfigLoader.load();
            ModStructures.reinit(event.getServer());
        }

        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {
            ModCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                String json = ConfigLoader.toJson();
                PacketHandler.sendToPlayer(new SyncConfigS2CPacket(json), serverPlayer);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    /**
     * Container for Forge game-bus events that only trigger on the client (e.g., logout, entity leave level).
     */
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            CameraRecoilManager.getInstance().reset();
            PlayerArmPose.clearAllStates();
        }

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
                PlayerArmPose.clearState(event.getEntity().getId());
            }
        }
    }
}
