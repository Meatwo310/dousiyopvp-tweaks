package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.entity.SecretConvoyTruckEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

public final class SecretConvoyTruckModel extends EntityModel<SecretConvoyTruckEntity> {
    private final ModelPart root;

    public SecretConvoyTruckModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(0, 96)
                .addBox(-17, -5, -8, 34, 5, 16, none), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-16, -17, -8, 13, 12, 16, none), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-3, -12, -8, 9, 7, 16, none), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("cargo", CubeListBuilder.create().texOffs(0, 32)
                .addBox(6, -18, -8, 12, 13, 16, none), PartPose.offset(0, 18, 0));
        addWheel(root, "front_left", -10, 18, -9); addWheel(root, "front_right", -10, 18, 9);
        addWheel(root, "rear_left", 12, 18, -9); addWheel(root, "rear_right", 12, 18, 9);
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void addWheel(PartDefinition root, String name, float x, float y, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(96, 96)
                .addBox(-4, -4, -2, 8, 8, 4), PartPose.offset(x, y, z));
    }

    @Override public void setupAnim(SecretConvoyTruckEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(pose, consumer, light, overlay, red, green, blue, alpha);
    }
}
