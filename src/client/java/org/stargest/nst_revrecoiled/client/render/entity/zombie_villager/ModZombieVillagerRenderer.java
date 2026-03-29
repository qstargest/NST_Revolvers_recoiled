package org.stargest.nst_revrecoiled.client.render.entity.zombie_villager;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieVillagerEntityRenderer;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.Villager.ModVillagers;

/**
 * Custom renderer for the zombie revolvermaker villager.
 * Extends the vanilla ZombieVillagerEntityRenderer to override texture selection 
 * for the revolvermaker profession. 
 */
public class ModZombieVillagerRenderer extends ZombieVillagerEntityRenderer {

    private static final Identifier REVOLVERMAKER_TEXTURE =
            Identifier.of(Main.MOD_ID, "textures/entity/zombie_villager/profession/revolvermaker.png");

    public ModZombieVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ZombieVillagerEntity entity) {
        if (entity.getVillagerData().getProfession() == ModVillagers.REVOLVERMAKER) {
            return REVOLVERMAKER_TEXTURE;
        }
        return super.getTexture(entity);
    }
}
