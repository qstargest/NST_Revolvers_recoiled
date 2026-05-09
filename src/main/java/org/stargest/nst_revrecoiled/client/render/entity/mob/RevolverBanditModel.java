package org.stargest.nst_revrecoiled.client.render.entity.mob;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.Entities.RevolverBanditEntity;
import org.stargest.nst_revrecoiled.client.render.entity.player.PlayerArmPose;

/**
 * Custom biped model for the Revolver Bandit.
 * Uses proportions matching the Pillager (8x10x8 head) while supporting
 * standard biped animations and custom revolver arm poses.
 */
public class RevolverBanditModel extends HumanoidModel<RevolverBanditEntity> {

    public RevolverBanditModel(ModelPart root) {
        super(root);
    }

    /**
     * Returns a LayerDefinition with Pillager-proportioned parts.
     * Starts with standard HumanoidModel mesh to ensure all required parts exist.
     */
    public static LayerDefinition createBodyLayer() {
        // 1. Start with standard biped layout
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // 2. Pillager Head & Hat (8x10x8 instead of 8x8x8)
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -10.0f, -4.0f, 8, 10, 8),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -10.0f, -4.0f, 8, 10, 8,
                        new CubeDeformation(0.45f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // Nose (lowered 1px to match reference)
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, -1.0f, -6.0f, 2, 4, 2),
                PartPose.offset(0.0f, -2.0f, 0.0f));

        // 3. Pillager Body & Jacket
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20).addBox(-4.0f, 0.0f, -3.0f, 8, 12, 6),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        body.addOrReplaceChild("jacket",
                CubeListBuilder.create().texOffs(0, 38).addBox(-4.0f, 0.0f, -3.0f, 8, 18, 6,
                        new CubeDeformation(0.45f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // 4. Arms & Sleeves
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4),
                PartPose.offset(-5.0f, 2.0f, 0.0f));
        rightArm.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4,
                        new CubeDeformation(0.25f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4),
                PartPose.offset(5.0f, 2.0f, 0.0f));
        leftArm.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4,
                        new CubeDeformation(0.25f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // 5. Legs & Pants
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
                PartPose.offset(-2.0f, 12.0f, 0.0f));
        rightLeg.addOrReplaceChild("right_pants",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4,
                        new CubeDeformation(0.25f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
                PartPose.offset(2.0f, 12.0f, 0.0f));
        leftLeg.addOrReplaceChild("left_pants",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4,
                        new CubeDeformation(0.25f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Applies vanilla biped angles first, then overlays the revolver arm pose.
     * The arm pose is only applied when the entity can be found in the world
     * and is holding a RevolverArmPoseItem.
     */
    @Override
    public void setupAnim(@NotNull RevolverBanditEntity state, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(state, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Entity entity = client.level.getEntity(state.getId());
        if (!(entity instanceof RevolverBanditEntity bandit)) return;

        PlayerArmPose.applyRevolverPoseForMob(this, bandit, state.getId());
    }
}
