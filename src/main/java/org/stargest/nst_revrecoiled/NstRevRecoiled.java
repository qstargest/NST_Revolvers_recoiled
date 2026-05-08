package org.stargest.nst_revrecoiled;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Structures.ModStructures;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;
import org.stargest.nst_revrecoiled.client.ClientEvents;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.network.SyncConfigS2CPacket;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.util.*;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Main NeoForge mod initialization class.
 * Sets up mod event buses, registers packets, configuration, and all registries
 * including items, entities, generic blocks, and custom structures.
 */
@Mod(NstRevRecoiled.MOD_ID)
public class NstRevRecoiled {

    public static final String MOD_ID = "nst_revrecoiled";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public NstRevRecoiled(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {}", MOD_ID);

        // Load configuration from disk
        ConfigLoader.load();

        // Register DeferredRegisters on the mod bus
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModScreenHandlers.MENU_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        // Explicitly register POIs and Professions using RegisterEvent to ensure correct binding order
        modEventBus.addListener(this::onRegister);

        // Subscribe mod-bus events (RegisterPayloadHandlersEvent, BuildCreativeModeTabContentsEvent, etc.)
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(ModItems::buildCreativeTab);
        modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientEvents::registerEntityRenderers);
            modEventBus.addListener(ClientEvents::registerLayerDefinitions);
            modEventBus.addListener(ClientEvents::registerScreens);
            modEventBus.addListener(ClientEvents::clientSetup);

            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onLogout);
            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onEntityLeaveLevel);
            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onEndClientTick);

            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onLogout);
            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onEntityLeaveLevel);
            NeoForge.EVENT_BUS.addListener(ClientGameEvents::onEndClientTick);
        }

        modEventBus.addListener(NstRevRecoiled::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.REVOLVER_BANDIT.get(), RevolverBanditEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event){
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS){
            event.accept(ModItems.REVOLVER_BANDIT_SPAWN_EGG);
        }
    }

    protected void onRegister(RegisterEvent event) {
        event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> {
            var states = ImmutableSet.copyOf(ModBlocks.ASSEMBLY_TABLE.get().getStateDefinition().getPossibleStates());
            helper.register(ModVillagers.ASSEMBLY_TABLE_POI_KEY.location(), new PoiType(states, 1, 1));
        });

        event.register(Registries.VILLAGER_PROFESSION, helper -> helper.register(ModVillagers.REVOLVERMAKER_KEY.location(), new VillagerProfession(
                "revolvermaker",
                (Holder<PoiType> holder) -> holder.is(ModVillagers.ASSEMBLY_TABLE_POI_KEY),
                (Holder<PoiType> holder) -> holder.is(ModVillagers.ASSEMBLY_TABLE_POI_KEY),
                ImmutableSet.of(),
                ImmutableSet.of(),
                SoundEvents.VILLAGER_WORK_ARMORER
        )));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // S2C packets
        registrar.playToClient(
                SyncConfigS2CPacket.TYPE,
                SyncConfigS2CPacket.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { 
                    Gson gson = new Gson();
                    ModConfig config = gson.fromJson(payload.configJson(),ModConfig.class);
                    if (config != null) ModConfig.set(config);
                })
        );

        // C2S packets
        registrar.playToServer(
                AssemblyCraftC2SPacket.Payload.TYPE,
                AssemblyCraftC2SPacket.Payload.STREAM_CODEC,
                AssemblyCraftC2SPacket::handleRevolver
        );
        registrar.playToServer(
                AssemblyCraftC2SPacket.BulletPayload.TYPE,
                AssemblyCraftC2SPacket.BulletPayload.STREAM_CODEC,
                AssemblyCraftC2SPacket::handleBullet
        );
    }

    // -----------------------------------------------------------------------
    // NeoForge game-bus events (server lifecycle, commands, player login)
    // -----------------------------------------------------------------------

    @EventBusSubscriber(modid = MOD_ID)
    public static class GameEvents {

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            MinecraftServer server = event.getServer();
            if (!AssemblyRecipes.isFrozen()) {
                AssemblyRecipes.freeze();
            }
            ConfigLoader.load();
            ModStructures.reinit(server);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            ModCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                String json = ConfigLoader.toJson();
                PacketDistributor.sendToPlayer(serverPlayer, new SyncConfigS2CPacket(json));
            }
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event){
            Player player = event.getEntity();
            if (player != null) {
                BaseRevolverItem.removePendingShot(player.getUUID());
                BaseRevolverItem.removePendingBullet(player.getUUID());
            }
        }
    }

    public static class ClientGameEvents {
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            CameraRecoilManager.getInstance().reset();
            PlayerArmPose.clearAllStates();
        }

        @SubscribeEvent
        public static void onEntityLeaveLevel (EntityLeaveLevelEvent event){
            if (event.getLevel().isClientSide()){
                var entity = event.getEntity();

                if (entity instanceof Player || entity instanceof RevolverBanditEntity){
                    PlayerArmPose.clearState(entity.getId());
                }
            }
        }

        @SubscribeEvent
        public static void onEndClientTick(ClientTickEvent.Post event){
            CameraRecoilManager.getInstance().tickPending();
        }
    }
}
