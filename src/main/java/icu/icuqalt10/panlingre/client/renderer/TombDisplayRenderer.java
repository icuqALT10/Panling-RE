package icu.icuqalt10.panlingre.client.renderer;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.TombDisplayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Block models/atlas UVs are baked once per shape and uploaded to GPU buffers. */
public final class TombDisplayRenderer extends EntityRenderer<TombDisplayEntity> {
    private static final Map<Integer,VertexBuffer> MESHES=new HashMap<>();
    private static final ResourceLocation GEOMETRY=ResourceLocation.fromNamespaceAndPath("panlingre","shihuang_scene/geometry.json");
    private static final ResourceLocation SEAL=ResourceLocation.fromNamespaceAndPath("panlingre","textures/entity/tomb_seal.png");
    public static final ResourceManagerReloadListener RELOAD = resources -> clearCache();

    public TombDisplayRenderer(EntityRendererProvider.Context context) { super(context); }
    private static void clearCache() { MESHES.values().forEach(VertexBuffer::close); MESHES.clear(); }
    private static VertexBuffer bake(int kind, ResourceManager resources) {
        Minecraft mc=Minecraft.getInstance();
        try (var reader=resources.openAsReader(GEOMETRY)) {
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            var palette=json.getAsJsonArray("palette");
            var states=new net.minecraft.world.level.block.state.BlockState[palette.size()];
            for (int i=0;i<states.length;i++) {
                var item=palette.get(i).getAsJsonObject(); var tag=new CompoundTag();
                tag.putString("Name",item.get("Name").getAsString()); var properties=new CompoundTag();
                item.getAsJsonObject("Properties").entrySet().forEach(e -> properties.putString(e.getKey(),e.getValue().getAsString()));
                tag.put("Properties",properties);
                states[i]=NbtUtils.readBlockState(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK),tag);
            }
            var builder=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.BLOCK);
            PoseStack local=new PoseStack(); var dispatcher=mc.getBlockRenderer();
            for (var cell : json.getAsJsonArray("shapes").get(kind).getAsJsonObject().getAsJsonArray("blocks")) {
                var b=cell.getAsJsonArray(); var state=states[b.get(0).getAsInt()];
                local.pushPose(); local.translate(b.get(1).getAsInt(),b.get(2).getAsInt(),b.get(3).getAsInt());
                int light=state.getLightEmission()>0 ? LightTexture.FULL_BRIGHT : LightTexture.pack(10,0);
                dispatcher.getModelRenderer().renderModel(local.last(),builder,state,dispatcher.getBlockModel(state),1,1,1,
                        light,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,null);
                local.popPose();
            }
            var mesh=builder.buildOrThrow(); var buffer=new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind(); buffer.upload(mesh); VertexBuffer.unbind();
            return buffer;
        } catch (IOException e) { throw new IllegalStateException("Missing composite tomb geometry",e); }
    }
    @Override public void render(TombDisplayEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource out,int light) {
        float progress=entity.progress(partial); int kind=entity.kind();
        if (kind==TombDisplayEntity.SEAL) {
            if (progress>0) renderSeal(pose,out,progress);
            return;
        }
        pose.pushPose();
        switch (kind) {
            case 0 -> pose.translate(0,5*progress,0);
            case 1 -> pose.translate(0,7*progress,0);
            case 2 -> pose.translate(0,2*progress,0);
            case TombDisplayEntity.BRONZE_LEFT -> pose.mulPose(Axis.YP.rotationDegrees(-95*progress));
            case TombDisplayEntity.BRONZE_RIGHT -> pose.mulPose(Axis.YP.rotationDegrees(95*progress));
            case TombDisplayEntity.COFFIN -> pose.mulPose(Axis.XP.rotationDegrees(-105*progress));
        }
        VertexBuffer buffer=MESHES.computeIfAbsent(kind,k -> bake(k,Minecraft.getInstance().getResourceManager()));
        RenderType type=RenderType.cutout(); type.setupRenderState(); buffer.bind();
        buffer.drawWithShader(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()),
                RenderSystem.getProjectionMatrix(),RenderSystem.getShader());
        VertexBuffer.unbind(); type.clearRenderState(); pose.popPose();
    }
    private void renderSeal(PoseStack pose,MultiBufferSource out,float alpha) {
        var consumer=out.getBuffer(RenderType.entityTranslucent(SEAL)); var p=pose.last();
        // This render type disables culling: one quad is visible from both sides.
        float[][] vertices={{0,0,0,1},{41,0,1,1},{41,29,1,0},{0,29,0,0}};
        for (int i=0;i<4;i++) {
            float[] v=vertices[i];
            consumer.addVertex(p,v[0],v[1],0).setColor(1F,1F,1F,alpha).setUv(v[2],v[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p,0,0,1);
        }
    }
    @Override public ResourceLocation getTextureLocation(TombDisplayEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
