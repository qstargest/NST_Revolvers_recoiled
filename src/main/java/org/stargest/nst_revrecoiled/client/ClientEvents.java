package org.stargest.nst_revrecoiled.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.client.gui.AssemblyTableScreen;
import org.stargest.nst_revrecoiled.client.managers.CameraRecoilManager;
import org.stargest.nst_revrecoiled.client.render.entity.mob.RevolverBanditModel;
import org.stargest.nst_revrecoiled.client.render.entity.mob.RevolverBanditRenderer;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;
import org.stargest.nst_revrecoiled.client.render.entity.player.RevolverArmConfig;
import org.stargest.nst_revrecoiled.client.render.entity.villager.ModVillagerRenderer;
import org.stargest.nst_revrecoiled.client.render.entity.zombie_villager.ModZombieVillagerRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.CobblestoneRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.DiamondRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.GoldenRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.render.revolvers.IronRevolverItemRenderer;
import org.stargest.nst_revrecoiled.client.util.ModEntityModelLayers;
import org.stargest.nst_revrecoiled.util.ModEntities;
import org.stargest.nst_revrecoiled.util.ModItems;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Consumer;

/**
 * Contains all client-side event subscribers and initializers for the mod.
 * Registers model layers, entity renderers, particle providers, and screen handlers.
 * Kept entirely separate from server logic via @EventBusSubscriber(value = Dist.CLIENT).
 *
 * Adapted for Forge 1.20.1.
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Screens
            /**
             * Registers the Assembly Table screen factory.
             * MenuScreens.register binds the AssemblyTableScreen class to the
             * ASSEMBLY_TABLE_HANDLER type, allowing the client to instantiate
             * the GUI when the server sends an open-screen packet.
             */
            MenuScreens.register(ModScreenHandlers.ASSEMBLY_TABLE_HANDLER.get(), AssemblyTableScreen::new);

            /**
             * Registers GeckoLib renderers for all revolver items.
             * The renderers are cached within the item instances to allow
             * ModItemRenderers to retrieve them during the bake phase.
             */
            registerItemRenderer(ModItems.COBBLESTONE_REVOLVER.get(), new CobblestoneRevolverItemRenderer());
            registerItemRenderer(ModItems.IRON_REVOLVER.get(),        new IronRevolverItemRenderer());
            registerItemRenderer(ModItems.GOLDEN_REVOLVER.get(),      new GoldenRevolverItemRenderer());
            registerItemRenderer(ModItems.DIAMOND_REVOLVER.get(),     new DiamondRevolverItemRenderer());

            /**
             * Configures custom third-person arm poses for revolvers.
             * Each item is mapped to a RevolverArmConfig which dictates how
             * the player's arms are transformed during the AIMING pose.
             */
            PlayerArmPose.registerArmConfig(ModItems.COBBLESTONE_REVOLVER.get(), RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.IRON_REVOLVER.get(),        RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.GOLDEN_REVOLVER.get(),      RevolverArmConfig.DEFAULT);
            PlayerArmPose.registerArmConfig(ModItems.DIAMOND_REVOLVER.get(),     RevolverArmConfig.DEFAULT);

            /**
             * Binds the camera recoil manager to revolver firing events.
             * The manager applies a procedurally generated kick-up to the
             * player's pitch when a revolver is fired on the client.
             */
            Consumer<LivingEntity> defaultRecoil =
                    shooter -> CameraRecoilManager.getInstance().scheduleRecoil(
                            BaseRevolverItem.SHOOT_DELAY_TICKS
                    );            BaseRevolverItem.registerRecoilCallback(ModItems.COBBLESTONE_REVOLVER.get(), defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.IRON_REVOLVER.get(),        defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.GOLDEN_REVOLVER.get(),      defaultRecoil);
            BaseRevolverItem.registerRecoilCallback(ModItems.DIAMOND_REVOLVER.get(),     defaultRecoil);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModEntityModelLayers.REVOLVER_BANDIT, RevolverBanditModel::createBodyLayer);
    }

    /**
     * Registers entity renderers for bullets and custom villager skins.
     */
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.REVOLVER_BANDIT.get(), RevolverBanditRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(EntityType.VILLAGER, ModVillagerRenderer::new);
        event.registerEntityRenderer(EntityType.ZOMBIE_VILLAGER, ModZombieVillagerRenderer::new);
    }

    protected static void registerItemRenderer(Item item, GeoItemRenderer<?> renderer) {
        if (item instanceof BaseRevolverItem revolverItem) {
            revolverItem.setRenderer(renderer);
        }
    }
}
