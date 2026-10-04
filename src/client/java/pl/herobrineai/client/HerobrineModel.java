package pl.herobrineai.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class HerobrineModel extends EntityModel<HerobrineRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath("herobrine-ai", "herobrine"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public HerobrineModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 0, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, 0, -2, 4, 12, 4), PartPose.offset(4, 1, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-3, 0, -2, 4, 12, 4), PartPose.offset(-4, 1, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HerobrineRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float swing = state.walkAnimationPos;
        float amp = state.walkAnimationSpeed;
        leftLeg.xRot = Mth.cos(swing * 0.6662f) * 1.2f * amp;
        rightLeg.xRot = Mth.cos(swing * 0.6662f + Mth.PI) * 1.2f * amp;
        leftArm.xRot = Mth.cos(swing * 0.6662f + Mth.PI) * 0.45f * amp;
        rightArm.xRot = Mth.cos(swing * 0.6662f) * 0.45f * amp;
    }
}
