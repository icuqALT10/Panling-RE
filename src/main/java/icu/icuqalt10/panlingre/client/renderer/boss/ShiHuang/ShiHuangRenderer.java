package icu.icuqalt10.panlingre.client.renderer.boss.ShiHuang;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.client.models.boss.ShiHuang.ShiHuangModel;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class ShiHuangRenderer extends GeoEntityRenderer<ShiHuangEntity> {
    private float auraInflate;
    private int auraBand;
    private boolean auraSword;
    private boolean auraCardsOnly;
    private int auraCardTint;
    private final org.joml.Vector3f auraCenter=new org.joml.Vector3f();
    public ShiHuangRenderer(EntityRendererProvider.Context context){super(context,new ShiHuangModel());withScale(icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE);shadowRadius=.8F*icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE;addRenderLayer(new EntranceLight(this));addRenderLayer(new Eyes(this));addRenderLayer(new BlinkGhost(this));addRenderLayer(new CommandLight(this));}
    @Override public void renderCubesOfBone(PoseStack pose,software.bernie.geckolib.cache.object.GeoBone bone,VertexConsumer buffer,int light,int overlay,int color){
        auraSword=bone.getName().equals("sword");auraCardTint=cardTint(bone.getName());
        if(auraInflate>0&&auraCardsOnly&&auraCardTint<0)return;
        super.renderCubesOfBone(pose,bone,buffer,auraCardTint>=0?LightTexture.FULL_BRIGHT:light,overlay,color);
    }
    static int cardTint(String name){
        if(name.startsWith("card_metal"))return 0xF4F7FF;
        if(name.startsWith("card_wood"))return 0x42D99B;
        if(name.startsWith("card_water"))return 0x577BBA;
        if(name.startsWith("card_fire"))return 0xF53125;
        if(name.startsWith("card_earth"))return 0xDCA832;
        return -1;
    }
    @Override public void renderCube(PoseStack pose,software.bernie.geckolib.cache.object.GeoCube cube,VertexConsumer buffer,int light,int overlay,int color){
        if(auraInflate>0){
            auraCenter.zero();int count=0;
            for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()){auraCenter.add(vertex.position());count++;}
            auraCenter.div(count);
        }
        super.renderCube(pose,cube,buffer,light,overlay,color);
    }
    @Override public void createVerticesOfQuad(software.bernie.geckolib.cache.object.GeoQuad quad,org.joml.Matrix4f matrix,org.joml.Vector3f normal,VertexConsumer buffer,int light,int overlay,int color){
        if(auraInflate<=0){super.createVerticesOfQuad(quad,matrix,normal,buffer,light,overlay,color);return;}
        int tint=auraCardTint>=0?auraCardTint:(auraBand==0)^auraSword?0xFFCB55:0x9C41FF;
        color=(color&0xFF000000)|tint;
        float inflateX=auraInflate,inflateY=auraInflate,inflateZ=auraInflate;
        if(auraCardTint>=0){
            // Unfolding clasps scale up to 40x; keep their glow thickness in world space.
            var scale=matrix.getScale(new org.joml.Vector3f());
            float width=auraInflate*icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE;
            inflateX=scale.x>1e-6?width/scale.x:0;
            inflateY=scale.y>1e-6?width/scale.y:0;
            inflateZ=scale.z>1e-6?width/scale.z:0;
        }
        for(int i=0;i<4;i++){
            var vertex=quad.vertices()[auraBand==1?3-i:i];
            var p=new org.joml.Vector3f(vertex.position());
            p.add(Math.signum(p.x-auraCenter.x)*inflateX,Math.signum(p.y-auraCenter.y)*inflateY,Math.signum(p.z-auraCenter.z)*inflateZ);
            matrix.transformPosition(p);
            buffer.addVertex(p.x,p.y,p.z,color,vertex.texU(),vertex.texV(),overlay,LightTexture.FULL_BRIGHT,normal.x,normal.y,normal.z);
        }
    }
    private static final class CommandLight extends GeoRenderLayer<ShiHuangEntity> {
        private static final RenderType BORDER=RenderType.create("panlingre_emperor_command_outline",com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,1536,false,true,RenderType.CompositeState.builder()
                        .setShaderState(net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(ResourceLocation.fromNamespaceAndPath("panlingre","textures/effect/white.png"),false,false))
                        .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setCullState(net.minecraft.client.renderer.RenderStateShard.CULL).setWriteMaskState(net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE).createCompositeState(false));
        private final ShiHuangRenderer renderer;
        CommandLight(ShiHuangRenderer renderer){super(renderer);this.renderer=renderer;}
        @Override public void render(PoseStack pose,ShiHuangEntity entity,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
            double end=entity.commandGlowEnd();
            double t=entity.seconds(partial);boolean body=entity.hasCombatGlow(partial);
            if(!body&&entity.cardElements().isEmpty())return;
            renderer.auraCardsOnly=!body;
            float strength=!body?1:entity.enraged()?(float)(.85+.15*Math.sin(entity.actionClock(partial)*.25)):(float)(Math.min(1,t/.35)*Math.min(1,(end+.15-t)/.15)*(.85+.15*Math.sin(t*5)));
            for(int band=1;band>=0;band--){
                renderer.auraBand=band;renderer.auraInflate=band==0?.012F:.065F;
                int alpha=(int)(strength*(band==0?48:28));
                var pass=band==0?EntranceLight.SILHOUETTE:BORDER;
                renderer.reRender(model,pose,buffers,entity,pass,buffers.getBuffer(pass),partial,LightTexture.FULL_BRIGHT,overlay,(alpha<<24)|0xFFFFFF);
            }
            renderer.auraInflate=0;renderer.auraCardsOnly=false;
        }
    }
    @Override public net.minecraft.world.phys.Vec3 getRenderOffset(ShiHuangEntity entity,float partial){
        var offset=entity.visualPosition(partial).subtract(entity.getPosition(partial));
        double t=entity.entranceTicks(partial);
        if(t>=ShiHuangEntity.ENTRANCE_GHOST_TICK&&t<ShiHuangEntity.ENTRANCE_REVEAL_TICK)
            offset=offset.add(0,icu.icuqalt10.panlingre.client.TombLightEffects.entranceDescentHeight(t),0);
        return offset;
    }
    @Override public boolean shouldRender(ShiHuangEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){
        return entity.blinkTicks(0)>=0&&entity.blinkTicks(0)<ShiHuangEntity.BLINK_GHOST_TICKS||super.shouldRender(entity,frustum,x,y,z);
    }
    @Override protected void applyRotations(ShiHuangEntity entity,PoseStack pose,float age,float yaw,float partial,float scale){
        // 始皇的倒下由动画完成，禁用原版死亡侧翻。
        pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
    }
    @Override public RenderType getRenderType(ShiHuangEntity entity,ResourceLocation texture,MultiBufferSource buffers,float partial){
        return entity.isInvisible()?null:RenderType.entityTranslucent(texture);
    }
    private static final class BlinkGhost extends GeoRenderLayer<ShiHuangEntity> {
        BlinkGhost(ShiHuangRenderer renderer){super(renderer);}
        @Override public void render(PoseStack pose,ShiHuangEntity entity,BakedGeoModel model,RenderType type,
                                     MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
            double age=entity.blinkTicks(partial);if(age<0||age>=ShiHuangEntity.BLINK_GHOST_TICKS||entity.dying())return;
            float fade=(float)Math.min(1,(ShiHuangEntity.BLINK_GHOST_TICKS-age)/(ShiHuangEntity.BLINK_GHOST_TICKS-ShiHuangEntity.BLINK_LIGHT_TICKS));
            var geo=(ShiHuangModel)getRenderer().getGeoModel();
            var offset=entity.blinkFrom().subtract(entity.getPosition(partial)).scale(1/icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE);
            float yaw=net.minecraft.util.Mth.rotLerp(partial,entity.yBodyRotO,entity.yBodyRot);
            pose.pushPose();
            // GeoEntityRenderer restores body yaw before applying layers. Translate in world axes first.
            pose.translate(offset.x,offset.y,offset.z);
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(yaw-entity.blinkYaw()));
            geo.applyPose(entity.blinkPose());
            var core=EntranceLight.SILHOUETTE;
            getRenderer().reRender(model,pose,buffers,entity,core,buffers.getBuffer(core),partial,LightTexture.FULL_BRIGHT,overlay,((int)(fade*64)<<24)|0xFFCB55);
            pose.popPose();
            if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch(core);
            icu.icuqalt10.panlingre.client.TombLightEffects.renderGhostVolume(pose,buffers,age,fade);
            geo.applyPose(entity.pose(partial));pose.popPose();
        }
    }
    private static final class EntranceLight extends GeoRenderLayer<ShiHuangEntity> {
        private static final RenderType SILHOUETTE=RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"));
        EntranceLight(ShiHuangRenderer renderer){super(renderer);}
        @Override public void render(PoseStack pose,ShiHuangEntity entity,BakedGeoModel model,RenderType type,
                                     MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
            double t=entity.entranceTicks(partial);
            if(t<ShiHuangEntity.ENTRANCE_GHOST_TICK||t>=ShiHuangEntity.ENTRANCE_REVEAL_TICK||entity.dying())return;
            int alpha=(int)(96*Math.min(1,(t-ShiHuangEntity.ENTRANCE_GHOST_TICK)/ShiHuangEntity.ENTRANCE_GHOST_FADE_TICKS));
            // Keep the translucent anatomy readable through the descending column's bright core.
            getRenderer().reRender(model,pose,buffers,entity,SILHOUETTE,buffers.getBuffer(SILHOUETTE),partial,LightTexture.FULL_BRIGHT,overlay,(alpha<<24)|0xFFCB55);
            if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch(SILHOUETTE);
            icu.icuqalt10.panlingre.client.TombLightEffects.renderEntranceVolume(pose,buffers,entity,partial);
        }
    }
    /** Full bright and independent of body invisibility; retain eye color against the entrance light. */
    private static final class Eyes extends GeoRenderLayer<ShiHuangEntity> {
        private static final RenderType TYPE=RenderType.eyes(ResourceLocation.fromNamespaceAndPath("panlingre","textures/entity/boss/shihuang/shihuang_spirit_eyes.png"));
        private static final RenderType ENTRANCE_TYPE=RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("panlingre","textures/entity/boss/shihuang/shihuang_spirit_eyes.png"));
        Eyes(ShiHuangRenderer renderer){super(renderer);}
        @Override public void render(PoseStack pose,ShiHuangEntity entity,BakedGeoModel model,RenderType type,
                                     MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
            double t=entity.entranceTicks(partial);
            if(t>=0&&t<ShiHuangEntity.ENTRANCE_GHOST_TICK)return;
            RenderType eyes=t>=0&&t<ShiHuangEntity.ENTRANCE_REVEAL_TICK?ENTRANCE_TYPE:TYPE;
            int alpha=t<0?255:(int)(255*Math.min(1,(t-ShiHuangEntity.ENTRANCE_GHOST_TICK)/ShiHuangEntity.ENTRANCE_GHOST_FADE_TICKS));
            getRenderer().reRender(model,pose,buffers,entity,eyes,buffers.getBuffer(eyes),partial,LightTexture.FULL_BRIGHT,overlay,(alpha<<24)|0xFFFFFF);
            icu.icuqalt10.panlingre.client.TombLightEffects.renderRageEyes(pose,buffers,entity,partial);
        }
    }
}
