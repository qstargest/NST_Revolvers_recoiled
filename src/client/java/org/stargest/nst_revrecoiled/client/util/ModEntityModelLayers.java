package org.stargest.nst_revrecoiled.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.client.render.entity.mob.RevolverBanditModel;

/**
 * Utility class for registering custom entity model layers.
 * Defines unique identifiers for model parts and binds them to their textured data generators.
 */
public final class ModEntityModelLayers {


    public static final EntityModelLayer REVOLVER_BANDIT =
            new EntityModelLayer(Identifier.of(Main.MOD_ID, "revolver_bandit"), "main");

    private ModEntityModelLayers() {}


    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(
                REVOLVER_BANDIT,
                RevolverBanditModel::getTexturedModelData
        );
    }
}
