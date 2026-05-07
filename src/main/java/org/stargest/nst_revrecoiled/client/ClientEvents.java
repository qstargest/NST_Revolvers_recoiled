package org.stargest.nst_revrecoiled.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.client.gui.AssemblyTableScreen;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.render.entity.mob.RevolverBanditModel;
import org.stargest.nst_revrecoiled.client.render.entity.mob.RevolverBanditRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.CobblestoneRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.DiamondRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.GoldenRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.IronRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.util.ModEntityModelLayers;
import org.stargest.nst_revrecoiled.util.ModEntities;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.client.render.entity.player.RevolverArmConfig;
import org.stargest.nst_revrecoiled.util.ModItems;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.world.entity.EntityType;
import org.stargest.nst_revrecoiled.client.render.entity.villager.ModVillagerRenderer;
import org.stargest.nst_revrecoiled.client.render.entity.zombie_villager.ModZombieVillagerRenderer;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Consumer;

/**
 * Contains all client-side event subscribers and initializers for the mod.
 * Registers model layers, entity renderers, particle providers, and screen handlers.
 * Kept entirely separate from server logic via @EventBusSubscriber(value = Dist.CLIENT).
 */
public class ClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.ASSEMBLY_TABLE_HANDLER.get(), AssemblyTableScreen::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModEntityModelLayers.REVOLVER_BANDIT, RevolverBanditModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.BULLET_PROJECTILE.get(),
                ThrownItemRenderer::new
        );

        event.registerEntityRenderer(ModEntities.REVOLVER_BANDIT.get(), RevolverBanditRenderer::new);
        event.registerEntityRenderer(EntityType.VILLAGER, ModVillagerRenderer::new);
        event.registerEntityRenderer(EntityType.ZOMBIE_VILLAGER, ModZombieVillagerRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Revolver item renderers
            registerItemRenderer(ModItems.COBBLESTONE_REVOLVER.get(), new CobblestoneRevolverItemRenderer());
            registerItemRenderer(ModItems.IRON_REVOLVER.get(),        new IronRevolverItemRenderer());
            registerItemRenderer(ModItems.GOLDEN_REVOLVER.get(),      new GoldenRevolverItemRenderer());
            registerItemRenderer(ModItems.DIAMOND_REVOLVER.get(),     new DiamondRevolverItemRenderer());

            // Revolver arm configs
            PlayerArmPose.registerArmConfig(ModItems.COBBLESTONE_REVOLVER.get(), RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.IRON_REVOLVER.get(),        RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.GOLDEN_REVOLVER.get(),      RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.DIAMOND_REVOLVER.get(),     RevolverArmConfig.DEFAULT);

            // Recoil callbacks
            Consumer<LivingEntity> defaultRecoil =
                    shooter -> CameraRecoilManager.getInstance().scheduleRecoil(
                            BaseRevolverItem.SHOOT_DELAY_TICKS
                    );
            BaseRevolverItem.registerRecoilCallback(ModItems.COBBLESTONE_REVOLVER.get(), defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.IRON_REVOLVER.get(),        defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.GOLDEN_REVOLVER.get(),      defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.DIAMOND_REVOLVER.get(),     defaultRecoil);
        });
    }

    protected static void registerItemRenderer(Item item, GeoItemRenderer<?> renderer) {
        if (item instanceof BaseRevolverItem revolverItem) {
            revolverItem.setRenderer(renderer);
        }
    }
}
