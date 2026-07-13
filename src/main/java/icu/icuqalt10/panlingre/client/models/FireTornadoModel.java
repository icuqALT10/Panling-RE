package icu.icuqalt10.panlingre.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class FireTornadoModel<T extends Entity> extends EntityModel<T> {
    // 对应你截图里的层级
    private final ModelPart tornadoBody;
    private final ModelPart tornado1;
    private final ModelPart tornado2;
    private final ModelPart tornado3;
    private final ModelPart tornado4;

    public FireTornadoModel(ModelPart root) {
        // 根据你截图中的嵌套关系，一层层获取子节点
        this.tornadoBody = root.getChild("tornado_body");
        this.tornado1 = this.tornadoBody.getChild("tornado_1");
        this.tornado2 = this.tornado1.getChild("tornado_2");
        this.tornado3 = this.tornado2.getChild("tornado_3");
        this.tornado4 = this.tornado3.getChild("tornado_4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition tornado_body = partdefinition.addOrReplaceChild("tornado_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0F, 0.0F));

        PartDefinition tornado_1 = tornado_body.addOrReplaceChild("tornado_1", CubeListBuilder.create().texOffs(1, 83).addBox(-2.5F, -7.0F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 25.0F, 0.0F));

        PartDefinition tornado_2 = tornado_1.addOrReplaceChild("tornado_2", CubeListBuilder.create().texOffs(49, 72).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(78, 32).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(74, 28).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, 0.0F));

        PartDefinition tornado_3 = tornado_2.addOrReplaceChild("tornado_3", CubeListBuilder.create().texOffs(105, 57).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(6, 6).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-9.0F, -8.0F, -9.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition tornado_4 = tornado_3.addOrReplaceChild("tornado_4", CubeListBuilder.create().texOffs(18, 12).addBox(-2.5F, -19.0F, -2.5F, 5.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 100).addBox(-6.0F, -19.0F, -6.0F, 12.0F, 10.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(8, 94).addBox(-9.0F, -19.0F, -9.0F, 18.0F, 10.0F, 18.0F, new CubeDeformation(0.0F))
                .texOffs(26, 87).addBox(-12.0F, -19.0F, -12.0F, 24.0F, 10.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        this.tornado1.yRot = ageInTicks * -0.4F;

        this.tornadoBody.y = (float) Math.sin(ageInTicks * 0.15F) * 1.25F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        // 因为它们是嵌套的，只要渲染最顶级的父节点，所有的子节点都会被一并渲染出来
        this.tornadoBody.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}