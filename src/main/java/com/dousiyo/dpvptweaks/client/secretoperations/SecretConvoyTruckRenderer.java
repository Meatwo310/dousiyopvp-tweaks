package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.entity.SecretConvoyTruckEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class SecretConvoyTruckRenderer extends EntityRenderer<SecretConvoyTruckEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "secret_convoy_truck"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            DpvpTweaks.MODID, "textures/entity/secret_convoy_truck.png");
    private final SecretConvoyTruckModel model;

    public SecretConvoyTruckRenderer(EntityRendererProvider.Context context) {
        super(context); model = new SecretConvoyTruckModel(context.bakeLayer(LAYER)); shadowRadius = 1.6F;
    }

    @Override public void render(SecretConvoyTruckEntity entity, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F - yaw));
        pose.scale(0.125F, -0.125F, -0.125F);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(SecretConvoyTruckEntity entity) { return TEXTURE; }
}
