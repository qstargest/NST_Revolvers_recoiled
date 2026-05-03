package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;

/**
 * Custom biped model for the Revolver Bandit.
 * Uses proportions matching the Pillager (8x10x8 head) while supporting
 * standard biped animations and custom revolver arm poses.
 */
public class RevolverBanditModel extends BipedEntityModel<RevolverBanditEntity> {

    public RevolverBanditModel(ModelPart root) {
        super(root);
    }

    /**
     * Returns custom textured model data matching the Pillager texture layout.
     * Starts with standard BipedEntityModel data to ensure all required parts
     * (head, hat, body, etc.) exist, avoiding NoSuchElementException.
     */
    public static TexturedModelData getTexturedModelData() {
        // 1. Start with standard biped layout (ensures head, hat, body, arms, legs exist)
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();

        // 2. Override Pillager Head & Components
        // We redefine "head" and "hat" with Pillager proportions (8x10x8 instead of 8x8x8)
        ModelPartData head = root.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        root.addChild(EntityModelPartNames.HAT,
                ModelPartBuilder.create().uv(32, 0).cuboid(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f, new Dilation(0.45f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Add Nose (Lowered by 1px to match reference)
        head.addChild(EntityModelPartNames.NOSE,
                ModelPartBuilder.create().uv(24, 0).cuboid(-1.0f, -1.0f, -6.0f, 2.0f, 4.0f, 2.0f),
                ModelTransform.pivot(0.0f, -2.0f, 0.0f));

        // 3. Override Pillager Body & Jacket (Outer Layer)
        ModelPartData body = root.addChild(EntityModelPartNames.BODY,
                ModelPartBuilder.create().uv(16, 20).cuboid(-4.0f, 0.0f, -3.0f, 8.0f, 12.0f, 6.0f),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Jacket/Vest (Outer layer, mapped to Pillager coat at UV 0, 38). Child of body.
        body.addChild("jacket",
                ModelPartBuilder.create().uv(0, 38).cuboid(-4.0f, 0.0f, -3.0f, 8.0f, 18.0f, 6.0f, new Dilation(0.45f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // 4. Override Pillager Arms & Sleeves
        ModelPartData rightArm = root.addChild(EntityModelPartNames.RIGHT_ARM,
                ModelPartBuilder.create().uv(40, 46).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                ModelTransform.pivot(-5.0f, 2.0f, 0.0f));
        rightArm.addChild("right_sleeve",
                ModelPartBuilder.create().uv(40, 46).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        ModelPartData leftArm = root.addChild(EntityModelPartNames.LEFT_ARM,
                ModelPartBuilder.create().uv(40, 46).mirrored().cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                ModelTransform.pivot(5.0f, 2.0f, 0.0f));
        leftArm.addChild("left_sleeve",
                ModelPartBuilder.create().uv(40, 46).mirrored().cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // 5. Override Pillager Legs & Pants
        ModelPartData rightLeg = root.addChild(EntityModelPartNames.RIGHT_LEG,
                ModelPartBuilder.create().uv(0, 22).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                ModelTransform.pivot(-2.0f, 12.0f, 0.0f));
        rightLeg.addChild("right_pants",
                ModelPartBuilder.create().uv(0, 22).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        ModelPartData leftLeg = root.addChild(EntityModelPartNames.LEFT_LEG,
                ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                ModelTransform.pivot(2.0f, 12.0f, 0.0f));
        leftLeg.addChild("left_pants",
                ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(0.25f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        return TexturedModelData.of(modelData, 64, 64);
    }

    /**
     * Applies vanilla biped angles first, then overlays the revolver arm pose.
     * The arm pose is only applied when the entity can be found in the world
     * and is holding a RevolverArmPoseItem.
     */
    @Override
    public void setAngles(RevolverBanditEntity entity,
                          float limbAngle, float limbDistance,
                          float animationProgress,
                          float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        PlayerArmPose.applyRevolverPoseForMob(this, entity, entity.getId());
    }
}
