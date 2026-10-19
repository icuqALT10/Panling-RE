package icu.icuqalt10.panlingre.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.network.TombLightPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/** Additive ribbons have a bright core and a wider, fading halo. No shader pack required. */
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class TombLightEffects {
    private static final net.minecraft.resources.ResourceLocation LIGHT_TEXTURE=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID,"textures/effect/white.png");
    // Standard emissive geometry remains luminous when shader packs replace vanilla shaders.
    // Eyes use a zero alpha cutoff, preserving the soft halo and its complete fade-out.
    private static final RenderType GLOW = RenderType.create("panlingre_tomb_glow",DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,1536,false,true,RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(LIGHT_TEXTURE,false,false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
    private static final RenderType EYE_CORE=RenderType.entityTranslucentEmissive(LIGHT_TEXTURE);
    private static final int GOLD = 0xFFCB55, PURPLE = 0xAC57FF;
    // Positive X/Z angles turn clockwise viewed from above: one revolution in four seconds.
    private static final double ENTRANCE_ROTATION_PER_TICK=Math.PI/40;
    private static final RenderType SWORD_TINT = RenderType.create("panlingre_sword_tint",DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,1536,false,true,RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(LIGHT_TEXTURE,false,false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
    private record Light(TombLightPayload effect, long start) {}
    private static final List<Light> lights = new ArrayList<>();
    public static RenderType emissiveType(){return GLOW;}
    /** Head-local light follows the same interpolated pose as the face, including rage-speed attacks. */
    public static void renderRageEyes(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,ShiHuangEntity boss,float partial){
        if((!boss.enraged()&&!boss.animation().equals("ultimate_full"))||boss.dying())return;
        double time=boss.tickCount+partial;
        pose.pushPose();pose.mulPose(boss.pose(partial).matrices().get("head"));
        // Centers measured from the existing head UV eye mask; stay just in front of the face.
        for(int pass=0;pass<2;pass++){
        var v=buffers.getBuffer(pass==0?GLOW:EYE_CORE);
        for(int side:new int[]{-1,1}){
            float pulse=(float)(.94+.06*Math.sin(time*.24+side));
            // Swept tongues follow the eye slit, flowing from the nose toward the temple.
            for(int tongue=0;tongue<3;tongue++){
                if(pass==0)eyeFlame(v,pose,side,time,tongue,1.9,0xFFB719,.20F*pulse);
                else {
                    eyeFlame(v,pose,side,time,tongue,1,0xFFD43A,.98F*pulse);
                    eyeFlame(v,pose,side,time,tongue,.5,0xFFE77A,.96F*pulse);
                }
            }
            for(int i=0;pass==0&&i<4;i++){
                double age=((time+i*3+side*1.5)%12+12)%12/12;
                Vec3 p=new Vec3(side*(.086+age*.34),3.541+.11*age*age+Math.sin(i*2.4+age*5)*.008,-.311-age*.025);
                float alpha=(float)(Math.sin(Math.PI*age)*.65);
                eyeGlow(v,pose,p,.0322*(1-age*.5),.0168*(1-age*.5),0xFFD14A,alpha);
            }
        }
        }
        pose.popPose();
    }
    private static void eyeFlame(VertexConsumer v,PoseStack pose,int side,double time,int tongue,double spread,int color,float alpha){
        double phase=time*.65+tongue*2.1;
        double length=.29+.025*Math.sin(phase*.45+tongue);
        for(int i=0;i<20;i++){
            double u=i/20.0,w=(i+1)/20.0;
            Vec3 a=eyeFlamePoint(side,u,length,phase,tongue),b=eyeFlamePoint(side,w,length,phase,tongue);
            double wa=spread*.029*Math.sin(Math.PI*(.16+.84*u))*Math.pow(1-u,.45);
            double wb=spread*.029*Math.sin(Math.PI*(.16+.84*w))*Math.pow(1-w,.45);
            float aa=alpha*(float)Math.min(1,(1-u)*4),ab=alpha*(float)Math.min(1,(1-w)*4);
            double inner=.42;
            vertex(v,pose,a.add(0,-wa*inner,-.001),color,aa);vertex(v,pose,b.add(0,-wb*inner,-.001),color,ab);
            vertex(v,pose,b.add(0,wb*inner,-.001),color,ab);vertex(v,pose,a.add(0,wa*inner,-.001),color,aa);
            for(int edge:new int[]{-1,1}){
                vertex(v,pose,a.add(0,edge*wa*inner,0),color,aa);vertex(v,pose,b.add(0,edge*wb*inner,0),color,ab);
                vertex(v,pose,b.add(0,edge*wb,0),color,0);vertex(v,pose,a.add(0,edge*wa,0),color,0);
            }
        }
    }
    private static Vec3 eyeFlamePoint(int side,double u,double length,double phase,int tongue){
        double rise=.062*u+(.035+tongue*.018)*u*u;
        double flutter=Math.sin(u*17-phase)*.0168*u*u+Math.sin(u*31-phase*1.4)*.0084*u;
        // North-face UV: inner eye pixel (210.5,145.5), face origin/size from the current GEO.
        return new Vec3(side*(.08613+length*u),3.5408+rise+flutter,-.3091-.018*u);
    }
    private static void eyeGlow(VertexConsumer v,PoseStack pose,Vec3 p,double width,double height,int color,float alpha){
        // Face-aligned soft discs keep the glow on the eyes rather than rotating across the whole face.
        for(int band=0;band<4;band++){
            double r0=band/4.0,r1=(band+1)/4.0;
            float a0=alpha*(float)Math.pow(1-r0*r0,2),a1=alpha*(float)Math.pow(1-r1*r1,2);
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
                Vec3 u=new Vec3(Math.cos(a)*width,Math.sin(a)*height,0),w=new Vec3(Math.cos(b)*width,Math.sin(b)*height,0);
                vertex(v,pose,p.add(u.scale(r0)),color,a0);vertex(v,pose,p.add(w.scale(r0)),color,a0);
                vertex(v,pose,p.add(w.scale(r1)),color,a1);vertex(v,pose,p.add(u.scale(r1)),color,a1);
            }
        }
    }
    private static ClientLevel world;

    private static void checkWorld(ClientLevel level) {
        if (world != level) { lights.clear(); ShiHuangSwordTrails.clear(); world = level; }
    }
    public static void add(TombLightPayload effect) {
        var level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level != null) lights.add(new Light(effect, effect.startTick()==Long.MIN_VALUE?level.getGameTime():effect.startTick()));
    }
    private static double age(Light light,float partial){
        if(light.effect.clockOwner()>=0){
            if(world.getEntity(light.effect.clockOwner()) instanceof ShiHuangEntity boss&&!boss.dying())
                return boss.actionClock(partial)-light.effect.clockStart();
            return light.effect.ticks();
        }
        return world.getGameTime()-light.start+(double)partial;
    }
    public static void beam(Vec3 from, Vec3 to, int color, float width, int ticks) {
        add(new TombLightPayload(TombLightPayload.BEAM, from, to, color, width, ticks));
    }
    public static void lightning(Vec3 from, Vec3 to, float width, int ticks) {
        add(new TombLightPayload(TombLightPayload.LIGHTNING, from, to, 0xBCA2FF, width, ticks));
    }
    public static void ring(Vec3 center, double radius, int color, int ticks) {
        add(new TombLightPayload(TombLightPayload.RING, center, new Vec3(radius,0,0), color, .035F, ticks));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance(); checkWorld(mc.level);
        if (world == null || mc.isPaused()) return;
        lights.removeIf(l -> age(l,0) >= l.effect.ticks());
    }
    public static void orb(Vec3 center,int color,float radius,int ticks){add(TombLightPayload.orb(center,Vec3.ZERO,color,radius,ticks));}
    public static void burst(Vec3 center,int color,float radius,int ticks){add(TombLightPayload.burst(center,color,radius,ticks));}
    @SubscribeEvent public static void shake(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event){
        var mc=Minecraft.getInstance();checkWorld(mc.level);if(world==null||mc.player==null)return;
        double time=world.getGameTime()+event.getPartialTick();
        float strength=0;
        // Derive entrance shake from the same synchronized clock as the column and reveal.
        for(var entity:world.entitiesForRendering())if(entity instanceof ShiHuangEntity boss&&!boss.dying()
                &&boss.distanceToSqr(mc.player)<160*160){
            double t=boss.entranceTicks((float)event.getPartialTick());
            if(t>=ShiHuangEntity.COFFIN_OPEN_TICKS&&t<ShiHuangEntity.ENTRANCE_REVEAL_TICK)
                strength=Math.max(strength,.65F);
            double impact=t-ShiHuangEntity.ENTRANCE_REVEAL_TICK;
            if(impact>=0&&impact<12)strength=Math.max(strength,7.5F*(float)Math.pow(1-impact/12,2));
            if(boss.animation().equals("ultimate_full")){
                double release=boss.seconds((float)event.getPartialTick())-4.5;
                if(release>=0&&release<1.4)strength=Math.max(strength,12F*(float)Math.pow(1-release/1.4,1.5));
            }
        }
        for(var l:lights){var e=l.effect;if(e.shape()!=TombLightPayload.ARENA_SHAKE)continue;
            // Recheck the actual player's instance bounds each frame, including spectator cameras.
            if(!new net.minecraft.world.phys.AABB(e.from(),e.to()).contains(mc.player.position()))continue;
            double age=time-l.start;if(age<0||age>=e.ticks())continue;
            float envelope=(float)((.45+.55*Math.exp(-age/8))*Math.min(1,(e.ticks()-age)/25));
            strength=Math.max(strength,e.width()*envelope);
        }
        event.setPitch(event.getPitch()+strength*(float)(.65*Math.sin(time*2.1)+.35*Math.sin(time*3.7)));
        event.setYaw(event.getYaw()+strength*.65F*(float)Math.sin(time*2.7+.8));
        event.setRoll(event.getRoll()+strength*.22F*(float)Math.sin(time*1.9));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance(); checkWorld(mc.level); if(world==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose=event.getPoseStack(); Vec3 camera=event.getCamera().getPosition();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();
        List<Integer> emperors=new ArrayList<>();
        for(var entity:world.entitiesForRendering())if(entity instanceof ShiHuangEntity boss){
            emperors.add(boss.getId());ShiHuangSwordTrails.update(boss,partial);
        }
        ShiHuangSwordTrails.retain(emperors);
        var rockType=RenderType.entityCutoutNoCull(LIGHT_TEXTURE);
        var rock= buffers.getBuffer(rockType);
        for(var l:lights){
            double age=age(l,partial);
            ShiHuangSkillEffects.lavaCrust(rock,pose,l.effect,age);
            ShiHuangSkillEffects.woodStems(rock,pose,l.effect,age);
        }
        buffers.endBatch(rockType);
        var colored=buffers.getBuffer(SWORD_TINT);
        ShiHuangSwordTrails.render(colored,pose,true);
        for(var l:lights)ShiHuangSkillEffects.draw(colored,pose,l.effect,age(l,partial),false);
        for(var entity:world.entitiesForRendering())if(entity instanceof ShiHuangEntity boss){ShiHuangSkillEffects.fan(colored,pose,boss,partial,false);ShiHuangSkillEffects.cards(colored,pose,boss,partial,false);}
        buffers.endBatch(SWORD_TINT);
        var vertices=buffers.getBuffer(GLOW);
        GraveDragonEffects.renderLights(vertices,pose,partial);
        ShiHuangSwordTrails.render(vertices,pose,false);
        for(var l:lights){var e=l.effect;double age=age(l,partial);float opacity=(float)Math.max(0,1-age/e.ticks());
            switch(e.shape()){
                case TombLightPayload.RING -> ring(vertices,pose,e.from(),e.to().x,e.color(),e.color(),e.width(),opacity);
                case TombLightPayload.BURST -> {double r=e.width()*(.15+.85*age/e.ticks());ring(vertices,pose,e.from(),r,e.color(),e.color(),.12F,opacity);orb(vertices,pose,e.from(),r,r,e.color(),opacity*.65F);}
                case TombLightPayload.ORB -> orb(vertices,pose,e.from().add(e.to().scale(age)),e.width(),e.width(),e.color(),opacity*.5F);
                case TombLightPayload.ARENA_SHAKE -> {}
                case TombLightPayload.LIGHTNING -> lightning(vertices,pose,e,age,l.start);
                case TombLightPayload.FLOW_EDGE -> flowingEdge(vertices,pose,e,age);
                case TombLightPayload.CRESCENT, TombLightPayload.FALLING_SWORD, TombLightPayload.WATER_COLUMN,
                        TombLightPayload.ELEMENT_CRESCENT,TombLightPayload.LAVA_BOULDER,TombLightPayload.HIT_SPARKS,TombLightPayload.WOOD_SURGE -> ShiHuangSkillEffects.draw(vertices,pose,e,age,true);
                default -> beam(vertices,pose,e.from(),e.to(),e.color(),e.color(),e.width(),opacity);
            }
        }
        for(var entity:world.entitiesForRendering())if(entity instanceof ShiHuangEntity boss&&!boss.dying()){
            ShiHuangSkillEffects.fan(vertices,pose,boss,partial,true);
            ShiHuangSkillEffects.cards(vertices,pose,boss,partial,true);
            ultimate(vertices,pose,boss,partial);
            if(boss.entranceTicks(partial)>=0)entrance(vertices,pose,boss,partial);
            double age=boss.blinkTicks(partial);
            if(age>=0&&age<ShiHuangEntity.BLINK_LIGHT_TICKS){
                blinkBridge(vertices,pose,boss,age,partial);
            }
        }
        buffers.endBatch(GLOW);pose.popPose();
    }
    private static void ultimate(VertexConsumer v,PoseStack pose,ShiHuangEntity boss,float partial){
        if(boss.animation().equals("counter_guard_sweep")){
            double age=boss.seconds(partial)-.1;
            if(age>=0&&age<.5)ring(v,pose,boss.visualPosition(partial).add(0,.14,0),
                    1+(icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangSkillShape.GUARD_COUNTER_RADIUS-1)*age/.5,
                    GOLD,GOLD,.20F,(float)(1-age/.5));
            return;
        }
        if(!boss.animation().equals("ultimate_full"))return;
        double t=boss.seconds(partial);Vec3 base=boss.visualPosition(partial),center=base.add(0,1.8,0);
        if(t>=.65&&t<4.5){
            double charge=Math.min(1,(t-.65)/3.85),clock=boss.tickCount+partial;
            Vec3 focus=boss.swordTrailPoint(boss.pose(partial),true,partial);
            ring(v,pose,base.add(0,.08,0),2.4+charge*2,GOLD,PURPLE,.06F,(float)(.35+.45*charge));
            // Continuous ribbons stay connected; only their brightness flows toward the sword.
            for(int i=0;i<28;i++){
                double phase=(clock*.022+i*.61803398875)%1,theta=i*2.399963+clock*.009;
                double y=.15+.7*((i*13%43)/43.0),r=9+charge*10;
                Vec3 direction=new Vec3(Math.cos(theta),y,Math.sin(theta)).normalize();
                Vec3 side=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(focus.add(direction.scale(r*.5))).cross(direction).normalize();
                Vec3 curl=new Vec3(-direction.z,0,direction.x).scale(Math.sin(i*2.1)*1.2);
                for(int step=0;step<32;step++)for(int edge:new int[]{-1,1})for(int band=0;band<2;band++)for(int corner=0;corner<4;corner++){
                    double u=(step+(corner==1||corner==2?1:0))/32.0;
                    double width=(.025+.045*charge)*(.4+.6*Math.sin(Math.PI*u));
                    double cross=edge*(corner>=2?(band==0?1:4):(band==0?0:1));
                    Vec3 point=focus.add(direction.scale(r*u)).add(curl.scale(Math.sin(Math.PI*u))).add(side.scale(width*cross));
                    double flow=.3+.7*Math.exp(-Math.pow((u-(1-phase))/.22,2));
                    float alpha=(float)(Math.pow(Math.sin(Math.PI*u),.6)*flow*(.35+.4*charge));
                    alpha*=corner>=2?(band==0?.28F:0):(band==0?1:.28F);
                    vertex(v,pose,point,band==0&&corner<2?GOLD:PURPLE,alpha);
                }
            }
            orb(v,pose,focus,.15+charge*.6,.15+charge*.6,GOLD,(float)(.25+.35*charge));
        }
        double release=t-4.5;if(release<0||release>=1.5)return;
        float fade=(float)Math.pow(1-release/1.5,1.2);
        for(int ringIndex=0;ringIndex<3;ringIndex++){
            double age=release-ringIndex*.12;if(age<0)continue;
            double radius=1+age*43;
            // Two distinct radial bands: golden eye fire inside, purple body light outside.
            ring(v,pose,base.add(0,.14+ringIndex*.22,0),radius,GOLD,GOLD,.20F,fade);
            ring(v,pose,base.add(0,.16+ringIndex*.22,0),radius+1.4,PURPLE,PURPLE,.25F,fade*.85F);
        }
        for(int i=0;i<80;i++){
            double theta=i*2.399963,y=(i%13)/12.0*.85-.12;
            Vec3 direction=new Vec3(Math.cos(theta),y,Math.sin(theta)).normalize();
            double reach=(9+release*45)*(1+.15*Math.sin(i*7.1));
            beam(v,pose,center.add(direction.scale(reach*.12)),center.add(direction.scale(reach)),GOLD,PURPLE,.12F,fade);
        }
        orb(v,pose,center,1.8+release*4,2.2+release*3,GOLD,fade*.65F);
    }
    private static void blinkBridge(VertexConsumer v,PoseStack pose,ShiHuangEntity boss,double age,float partial){
        Vec3 from=boss.blinkFrom(),to=boss.visualPosition(partial),direction=to.subtract(from).multiply(1,0,1).normalize();
        Vec3 side=new Vec3(-direction.z,0,direction.x);
        float fade=(float)Math.min(1,(ShiHuangEntity.BLINK_LIGHT_TICKS-age)/3);
        // Overlapping soft sections fill the complete route, including both silhouettes.
        // Flow changes the brightness, never the coverage, so it cannot open moving gaps.
        for(int sheet=-4;sheet<=4;sheet++)for(int row=0;row<24;row++)for(int step=0;step<32;step++)for(int corner=0;corner<4;corner++){
            double y=(row+(corner>=2?1:0))/24.0,u=(step+(corner==1||corner==2?1:0))/32.0;
            double lateral=sheet/4.0,profile=.55+.45*Math.sin(Math.PI*y);
            Vec3 point=from.lerp(to,u).add(side.scale(lateral*profile)).add(0,y*4.8,0);
            float flow=(float)(.92+.08*Math.cos(u*Math.PI*4-age*.85));
            float alpha=fade*flow*(float)(.065*Math.pow(Math.sin(Math.PI*y),.5)*Math.pow(1-lateral*lateral,1.5));
            int color=net.minecraft.util.FastColor.ARGB32.lerp((float)Math.min(1,.2+Math.abs(lateral)*1.4),GOLD,PURPLE);
            vertex(v,pose,point,color,alpha);
        }
    }
    private static void flowingEdge(VertexConsumer v,PoseStack pose,TombLightPayload e,double age){
        float fade=(float)Math.min(1,(e.ticks()-age)/4);
        beam(v,pose,e.from(),e.to(),0x42EAA0,0x137D45,.15F,fade*.65F);
        for(int i=0;i<3;i++){
            double u=(age/12+i/3.0)%1;
            for(int j=0;j<8;j++){
                double a=(u-j*.018+1)%1,b=(u-(j+1)*.018+1)%1;
                if(a<b)continue;
                beam(v,pose,e.from().lerp(e.to(),b),e.from().lerp(e.to(),a),0xDCFFE3,0x3BFF86,.09F,fade*(1-j/8F));
            }
        }
    }
    private static void entrance(VertexConsumer v,PoseStack pose,ShiHuangEntity boss,float partial) {
        double t=boss.entranceTicks(partial);Vec3 p=boss.getPosition(partial);
        double descend=t-ShiHuangEntity.ENTRANCE_DESCEND_TICK;
        float coffinFade=(float)Math.min(1,Math.max(0,(ShiHuangEntity.ENTRANCE_LAND_TICK-t)/24));
        if(t>=0&&coffinFade>0){
            Vec3 mouth=boss.entranceOrigin().add(0,-.28,1);
            coffinGlow(v,pose,mouth,t,coffinFade);
            if(t>=ShiHuangEntity.COFFIN_OPEN_TICKS)
                skyColumn(v,pose,mouth,t-ShiHuangEntity.COFFIN_OPEN_TICKS,t,false,4.5,coffinFade);
        }
        if(descend>=0&&t<ShiHuangEntity.ENTRANCE_LAND_TICK){
            float fade=(float)Math.min(1,(ShiHuangEntity.ENTRANCE_LAND_TICK-t)/12);
            skyColumn(v,pose,p,descend,t,true,3.5,fade);
            if(descend>=12)orb(v,pose,p.add(0,.2,0),6,2,GOLD,fade*.5F);
        }
        double gather=t-ShiHuangEntity.ENTRANCE_LAND_TICK;
        if(gather>=0&&t<ShiHuangEntity.ENTRANCE_REVEAL_TICK){
            for(int i=0;i<96;i++){
                double phase=gather*.025+scatter(i,17),u=phase%1,tail=Math.max(0,u-.16);
                int seed=i+96*(int)Math.floor(phase);
                double a=scatter(seed,43)*Math.PI*2;
                double edge=45/Math.max(Math.abs(Math.cos(a)),Math.abs(Math.sin(a)));
                boolean overhead=i%3==0;
                double radius=overhead?3+18*scatter(seed,89):edge*(.35+.65*scatter(seed,89));
                double endRadius=3+3.6*scatter(seed,127);
                double height=overhead?16+22*scatter(seed,191):.3+10.8*scatter(seed,191);
                Vec3 start=p.add(Math.cos(a)*radius,height,Math.sin(a)*radius);
                Vec3 end=p.add(Math.cos(a)*endRadius,(overhead?4:.6)+4.8*scatter(seed,239),Math.sin(a)*endRadius);
                Vec3 head=start.lerp(end,u),back=start.lerp(end,tail);
                float alpha=(float)Math.sin(Math.PI*u);
                gatherBeam(v,pose,back,head,alpha);
            }
            ring(v,pose,p.add(0,.09,0),1.3+.2*Math.sin(t*.13),PURPLE,GOLD,.025F,.65F);
        }
        if(gather>=0&&gather<24){double radius=gather*1.8;float fade=(float)(1-gather/24);
            ring(v,pose,p.add(0,.08,0),radius,GOLD,GOLD,.16F,fade);
            for(int i=0;i<48;i++){double a=i*Math.PI/24;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a));
                beam(v,pose,p.add(d.scale(radius*.7)).add(0,.08,0),p.add(d.scale(radius)).add(0,.08,0),0xFFE6A0,GOLD,.08F,fade);}
        }
        burst(v,pose,p.add(0,2*icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE,0),t-ShiHuangEntity.ENTRANCE_REVEAL_TICK,
                ShiHuangEntity.ENTRANCE_DRAW_TICK-ShiHuangEntity.ENTRANCE_REVEAL_TICK);
        // Trigger on the rendered clip's pointing frame, even if its start arrived after the entrance clock.
        double pointAge=boss.animation().equals("emerge")?boss.seconds(partial)*20
                -(ShiHuangEntity.ENTRANCE_POINT_TICK-ShiHuangEntity.ENTRANCE_REVEAL_TICK):-1;
        if(pointAge>=0&&pointAge<24){
            var frame=boss.pose(partial);
            Vec3 tip=boss.swordTrailPoint(frame,true,partial),base=boss.swordTrailPoint(frame,false,partial);
            Vec3 forward=tip.subtract(base).normalize();
            burst(v,pose,tip,pointAge,24);
            beam(v,pose,tip,tip.add(forward.scale(32)),PURPLE,GOLD,.16F,(float)(1-pointAge/24));
        }
    }
    private static double scatter(int index,int salt){
        int h=index*0x9E3779B9+salt;h=(h^(h>>>16))*0x85EBCA6B;h=(h^(h>>>13))*0xC2B2AE35;
        return ((h^(h>>>16))&0xFFFFFFFFL)/4294967296.0;
    }
    private static void coffinGlow(VertexConsumer v,PoseStack pose,Vec3 p,double time,float fade){
        // The lid spans 9 x 19 blocks. Fill its entire opening, then fade outward continuously.
        float alpha=fade*(float)(.91+.04*Math.sin(time*.12));
        double[] xs={-1,1,1,-1},zs={-1,-1,1,1};
        for(int i=0;i<4;i++)vertex(v,pose,p.add(xs[i]*4.5,0,zs[i]*9.5),0xE5C7FF,alpha);
        for(int band=0;band<8;band++){
            double a=band/8.0,b=(band+1)/8.0;
            float ai=alpha*(float)((1-a)*(1-a)),ao=alpha*(float)((1-b)*(1-b));
            for(int i=0;i<4;i++){
                int j=(i+1)%4;
                vertex(v,pose,p.add(xs[i]*(4.5+a*4),0,zs[i]*(9.5+a*4)),PURPLE,ai);
                vertex(v,pose,p.add(xs[j]*(4.5+a*4),0,zs[j]*(9.5+a*4)),PURPLE,ai);
                vertex(v,pose,p.add(xs[j]*(4.5+b*4),0,zs[j]*(9.5+b*4)),PURPLE,ao);
                vertex(v,pose,p.add(xs[i]*(4.5+b*4),0,zs[i]*(9.5+b*4)),PURPLE,ao);
            }
        }
        // Rotate spiral highlights over the canopy while keeping its footprint over the opening.
        for(int band=0;band<12;band++)for(int i=0;i<64;i++){
            double r0=band/12.0,r1=(band+1)/12.0;
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
            for(int corner=0;corner<4;corner++){
                double r=corner<2?r0:r1,angle=corner==0||corner==3?a:b;
                double swirl=Math.pow(.5+.5*Math.cos(3*(angle-time*ENTRANCE_ROTATION_PER_TICK)+r*5),3);
                double x=Math.cos(angle),z=Math.sin(angle);
                x=Math.copySign(Math.sqrt(Math.abs(x)),x);z=Math.copySign(Math.sqrt(Math.abs(z)),z);
                vertex(v,pose,p.add(x*8.5*r,4.8*(1-r*r),z*13.5*r),
                        net.minecraft.util.FastColor.ARGB32.lerp((float)swirl,PURPLE,0xE5C7FF),
                        alpha*(float)(.5*(1-r*r)*(.25+.75*swirl)));
            }
        }
    }
    public static double entranceDescentHeight(double time){
        double progress=net.minecraft.util.Mth.clamp((time-ShiHuangEntity.ENTRANCE_DESCEND_TICK)/ShiHuangEntity.ENTRANCE_DESCENT_TICKS,0,1);
        return 96*(1-progress*progress*(3-2*progress));
    }
    private static void skyColumn(VertexConsumer v,PoseStack pose,Vec3 base,double age,double time,boolean descending,double radius,float fade){
        double progress=Math.min(1,age/ShiHuangEntity.ENTRANCE_DESCENT_TICKS);progress=progress*progress*(3-2*progress);
        double bottom=descending?entranceDescentHeight(time):0,top=descending?96:96*progress;
        if(top<=bottom)return;
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 right=new Vec3(new org.joml.Vector3f(1,0,0).rotate(camera.rotation()));
        for(int row=0;row<24;row++)for(int band=0;band<24;band++)for(int corner=0;corner<4;corner++){
            double y=(row+(corner>=2?1:0))/24.0,height=bottom+(top-bottom)*y;
            double side=(band+(corner==1||corner==2?1:0))/12.0-1;
            double edge=Math.abs(side),flow=1+.035*Math.sin(height*.3-age*.4);
            int color=net.minecraft.util.FastColor.ARGB32.lerp((float)Math.min(1,edge*2),0xFFF3C9,PURPLE);
            float alpha=fade*(float)(.85*Math.pow(1-side*side,2)*Math.min(1,(1-y)*12));
            vertex(v,pose,base.add(right.scale(side*radius*2.8*flow)).add(0,height,0),color,alpha);
        }
        // World-space helical ribbons reveal rotation without turning the soft core edge-on.
        for(int ribbon=0;ribbon<3;ribbon++)for(int row=0;row<96;row++)for(int band=0;band<4;band++)for(int corner=0;corner<4;corner++){
            double y=(row+(corner>=2?1:0))/96.0,height=bottom+(top-bottom)*y;
            double across=(band+(corner==1||corner==2?1:0))/2.0-1;
            double angle=time*ENTRANCE_ROTATION_PER_TICK+height*.22+ribbon*Math.PI*2/3+across*.22;
            float alpha=fade*(float)(.65*(1-across*across)*Math.min(1,y*24)*Math.min(1,(1-y)*12));
            vertex(v,pose,base.add(Math.cos(angle)*radius*1.7,height,Math.sin(angle)*radius*1.7),
                    net.minecraft.util.FastColor.ARGB32.lerp((float)Math.abs(across),0xE5C7FF,PURPLE),alpha);
        }
    }
    private static void burst(VertexConsumer v,PoseStack pose,Vec3 p,double age,int life){
        if(age<0||age>=life)return;float fade=(float)(1-age/life);double r=.5+age*1.9;
        orb(v,pose,p,Math.min(4,r*.35),Math.min(4,r*.35),GOLD,fade*.45F);
        ring(v,pose,p.add(0,-1.85,0),r,PURPLE,GOLD,.08F,fade);
        for(int i=0;i<64;i++){double a=i*2.39996,y=(i+.5)/64,s=Math.sqrt(1-y*y);
            Vec3 d=new Vec3(Math.cos(a)*s,y*.45,Math.sin(a)*s);
            beam(v,pose,p.add(d.scale(r*.3)),p.add(d.scale(r)),PURPLE,GOLD,.07F,fade);
        }
    }
    public static void ring(VertexConsumer v,PoseStack pose,Vec3 p,double r,int core,int halo,float width,float alpha){
        if(r<=0)return;
        int segments=Math.max(64,Math.min(512,(int)Math.ceil(r*8)));
        for(int i=0;i<segments;i++){double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            for(int sign:new int[]{-1,1})for(int band=0;band<2;band++){
                double inner=Math.max(0,r+sign*width*(band==0?0:2)),outer=Math.max(0,r+sign*width*(band==0?2:7));
                int ci=band==0?core:halo;float ai=alpha*(band==0?.85F:.2F),ao=band==0?alpha*.2F:0;
                vertex(v,pose,p.add(Math.cos(a)*inner,0,Math.sin(a)*inner),ci,ai);
                vertex(v,pose,p.add(Math.cos(b)*inner,0,Math.sin(b)*inner),ci,ai);
                vertex(v,pose,p.add(Math.cos(b)*outer,0,Math.sin(b)*outer),halo,ao);
                vertex(v,pose,p.add(Math.cos(a)*outer,0,Math.sin(a)*outer),halo,ao);
            }
        }
    }
    static void warningRing(VertexConsumer v,PoseStack pose,Vec3 p,double radius,int color,float alpha){
        // A camera-facing cross section keeps the 0.25-block core readable at grazing angles.
        // A flat annulus becomes subpixel on its far side and flickers as the radius changes.
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int segments=Math.max(64,Math.min(512,(int)Math.ceil(radius*8)));
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            Vec3 from=p.add(Math.cos(a)*radius,0,Math.sin(a)*radius),to=p.add(Math.cos(b)*radius,0,Math.sin(b)*radius);
            Vec3 sa=camera.subtract(from).cross(new Vec3(-Math.sin(a),0,Math.cos(a))).normalize();
            Vec3 sb=camera.subtract(to).cross(new Vec3(-Math.sin(b),0,Math.cos(b))).normalize();
            for(int sign:new int[]{-1,1})for(int band=0;band<2;band++){
                double inner=sign*(band==0?0:.125),outer=sign*(band==0?.125:.4375);
                float ai=alpha*(band==0?.85F:.2F),ao=band==0?alpha*.2F:0;
                vertex(v,pose,from.add(sa.scale(inner)),color,ai);vertex(v,pose,to.add(sb.scale(inner)),color,ai);
                vertex(v,pose,to.add(sb.scale(outer)),color,ao);vertex(v,pose,from.add(sa.scale(outer)),color,ao);
            }
        }
    }
    static void warningPath(VertexConsumer v,PoseStack pose,Vec3 from,Vec3 to,int color){
        beam(v,pose,from,to,color,color,.03F,1);
    }
    public static void beam(VertexConsumer v,PoseStack pose,Vec3 a,Vec3 b,int core,int halo,float width,float alpha){
        Vec3 d=b.subtract(a);if(d.lengthSqr()<1e-8)return;
        Vec3 facing=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(a.lerp(b,.5));
        Vec3 side=facing.cross(d).normalize();if(side.lengthSqr()<.5)side=new Vec3(0,1,0).cross(d).normalize();
        // Interpolated vertex alpha gives a continuous halo instead of hard rectangular strips.
        for(int sign:new int[]{-1,1}){
            Vec3 inner=side.scale(width*.35*sign),middle=side.scale(width*2*sign),outer=side.scale(width*7*sign);
            strip(v,pose,a,b,Vec3.ZERO,inner,core,core,.85F*alpha,.7F*alpha);
            strip(v,pose,a,b,inner,middle,core,halo,.7F*alpha,.22F*alpha);
            strip(v,pose,a,b,middle,outer,halo,halo,.22F*alpha,0);
        }
    }
    static void lightning(VertexConsumer v,PoseStack pose,TombLightPayload e,double age,long seed){
        if(age<0||age>=e.ticks())return;
        // Hold each discharge shape between flashes; no smooth sinusoidal glow or frame-rate jitter.
        double phase=age/e.ticks();
        int flash=phase<.3?0:phase<.62?1:2;
        float alpha=(float)(phase<.14?1:phase<.3?.035:phase<.44?.85:phase<.62?.025:.65*(1-phase)/.38);
        if(e.ticks()<=4){flash=0;alpha=(float)(1-phase);}
        Vec3 direction=e.to().subtract(e.from());double length=direction.length();if(length<.001)return;
        Vec3 axis=direction.normalize(),side=axis.cross(Math.abs(axis.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        Vec3 up=axis.cross(side);
        var random=net.minecraft.util.RandomSource.create(seed^Double.doubleToLongBits(e.from().x)^Long.rotateLeft(Double.doubleToLongBits(e.from().z),17)^flash*341873128712L);
        Vec3[] points=new Vec3[17];double spread=Math.min(1.35,length*.045);
        for(int i=0;i<points.length;i++){
            double t=i/16.0,envelope=Math.sin(Math.PI*t);
            points[i]=e.from().lerp(e.to(),t).add(side.scale((random.nextDouble()*2-1)*spread*envelope))
                    .add(up.scale((random.nextDouble()*2-1)*spread*envelope));
        }
        lightningPath(v,pose,points,e.color(),e.width(),alpha,false);
        for(int branch=0;branch<3;branch++){
            int index=4+branch*4;Vec3[] fork=new Vec3[5];fork[0]=points[index];
            Vec3 end=fork[0].add(direction.scale(.18)).add(side.scale((branch%2==0?1:-1)*spread*2.8)).add(up.scale((random.nextDouble()-.5)*spread*3));
            for(int i=1;i<fork.length;i++)fork[i]=fork[0].lerp(end,i/4.0).add(side.scale((random.nextDouble()-.5)*spread*.7));
            lightningPath(v,pose,fork,e.color(),e.width()*.48F,alpha*.65F,true);
        }
    }
    private static void lightningPath(VertexConsumer v,PoseStack pose,Vec3[] points,int color,float width,float alpha,boolean taper){
        Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3[] sides=new Vec3[points.length];
        for(int i=0;i<points.length;i++){
            Vec3 tangent=points[Math.min(i+1,points.length-1)].subtract(points[Math.max(0,i-1)]);
            sides[i]=camera.subtract(points[i]).cross(tangent).normalize();
            if(sides[i].lengthSqr()<.5)sides[i]=new Vec3(1,0,0);
            if(i>0&&sides[i].dot(sides[i-1])<0)sides[i]=sides[i].scale(-1);
        }
        // Shared cross sections keep every bend connected, including the bright core.
        for(int i=1;i<points.length;i++)for(int sign:new int[]{-1,1})for(int band=0;band<3;band++){
            double inner=sign*width*(band==0?0:band==1?.45:1.8),outer=sign*width*(band==0?.45:band==1?1.8:5);
            float ai=alpha*(band==0?1:band==1?.8F:.18F),ao=alpha*(band==0?.8F:band==1?.18F:0);
            int ci=band<2?0xF3F5FF:color,co=band==0?0xF3F5FF:color;
            double a=taper?1-(i-1)/(double)(points.length-1):1,b=taper?1-i/(double)(points.length-1):1;
            vertex(v,pose,points[i-1].add(sides[i-1].scale(inner*a)),ci,ai*(float)a);
            vertex(v,pose,points[i].add(sides[i].scale(inner*b)),ci,ai*(float)b);
            vertex(v,pose,points[i].add(sides[i].scale(outer*b)),co,ao*(float)b);
            vertex(v,pose,points[i-1].add(sides[i-1].scale(outer*a)),co,ao*(float)a);
        }
    }
    private static void gatherBeam(VertexConsumer v,PoseStack pose,Vec3 a,Vec3 b,float alpha){
        Vec3 direction=b.subtract(a);if(direction.lengthSqr()<1e-8)return;
        Vec3 facing=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(a.lerp(b,.5));
        Vec3 side=facing.cross(direction).normalize();
        // One shared envelope across the whole trail: no per-piece fade resets or rectangular caps.
        for(int row=0;row<12;row++)for(int band=0;band<8;band++)for(int corner=0;corner<4;corner++){
            double along=(row+(corner>=2?1:0))/12.0;
            double across=(band+(corner==1||corner==2?1:0))/4.0-1;
            double taper=Math.sin(Math.PI*along),radial=1-across*across;
            float opacity=alpha*(float)(.4*taper*taper*radial*radial);
            int color=net.minecraft.util.FastColor.ARGB32.lerp((float)Math.abs(across),PURPLE,GOLD);
            Vec3 point=a.lerp(b,along).add(side.scale(across*.5*Math.sqrt(Math.max(0,taper))));
            vertex(v,pose,point,color,opacity);
        }
    }
    private static void strip(VertexConsumer v,PoseStack pose,Vec3 a,Vec3 b,Vec3 inner,Vec3 outer,int ci,int co,float ai,float ao){
        // Fade each end as well as the two sides.
        for(int i=0;i<3;i++){double t0=i==0?0:i==1?.12:.88,t1=i==0?.12:i==1?.88:1;
            float f0=i==0?0:1,f1=i==2?0:1;Vec3 p=a.lerp(b,t0),q=a.lerp(b,t1);
            vertex(v,pose,p.add(inner),ci,ai*f0);vertex(v,pose,q.add(inner),ci,ai*f1);
            vertex(v,pose,q.add(outer),co,ao*f1);vertex(v,pose,p.add(outer),co,ao*f0);
        }
    }
    private static void orb(VertexConsumer v,PoseStack pose,Vec3 p,double width,double height,int color,float alpha){
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 right=new Vec3(new org.joml.Vector3f(1,0,0).rotate(camera.rotation()));
        Vec3 up=new Vec3(new org.joml.Vector3f(0,1,0).rotate(camera.rotation()));
        for(int j=0;j<8;j++){double r0=j/8.0,r1=(j+1)/8.0;float a0=alpha*(float)((Math.exp(-4*r0*r0)-Math.exp(-4))/(1-Math.exp(-4))),a1=alpha*(float)((Math.exp(-4*r1*r1)-Math.exp(-4))/(1-Math.exp(-4)));
            for(int i=0;i<24;i++){double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
                Vec3 d0=right.scale(Math.cos(a)*width).add(up.scale(Math.sin(a)*height)),d1=right.scale(Math.cos(b)*width).add(up.scale(Math.sin(b)*height));
                vertex(v,pose,p.add(d0.scale(r0)),color,a0);vertex(v,pose,p.add(d1.scale(r0)),color,a0);
                vertex(v,pose,p.add(d1.scale(r1)),color,a1);vertex(v,pose,p.add(d0.scale(r1)),color,a1);
            }
        }
    }
    public static void renderEntranceVolume(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,ShiHuangEntity boss,float partial){
        double t=boss.entranceTicks(partial);if(t<ShiHuangEntity.ENTRANCE_GHOST_TICK||t>=ShiHuangEntity.ENTRANCE_REVEAL_TICK||boss.dying())return;
        float fade=(float)Math.min(1,(t-ShiHuangEntity.ENTRANCE_GHOST_TICK)/ShiHuangEntity.ENTRANCE_GHOST_FADE_TICKS);
        renderGhostVolume(pose,buffers,t,fade);
    }
    public static void renderGhostVolume(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,double time,float fade){
        pose.pushPose();
        // Render layers receive world-aligned axes; the camera-facing volume needs no body-yaw correction.
        entranceVolume(buffers.getBuffer(GLOW),pose,new Vec3(0,2.148,0),time,fade);
        if(buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource source)source.endBatch(GLOW);
        pose.popPose();
    }
    private static void entranceVolume(VertexConsumer v,PoseStack pose,Vec3 p,double time,float fade){
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 right=new Vec3(new org.joml.Vector3f(1,0,0).rotate(camera.rotation()));
        Vec3 up=new Vec3(new org.joml.Vector3f(0,1,0).rotate(camera.rotation()));
        // Smooth vertex falloff hides armor edges without stacking enlarged copies of the model.
        for(int row=0;row<32;row++)for(int band=0;band<24;band++){
            for(int corner=0;corner<4;corner++){
                double y=(row+(corner>=2?1:0))/32.0;
                double side=(band+(corner==1||corner==2?1:0))/12.0-1;
                float alpha=fade*(float)(.48*Math.pow(Math.sin(Math.PI*y),.45)*Math.pow(1-side*side,3));
                int color=net.minecraft.util.FastColor.ARGB32.lerp((float)Math.min(1,Math.abs(side)*1.7),GOLD,PURPLE);
                vertex(v,pose,ghostPoint(p,right,up,y,side*1.9,time),color,alpha);
            }
        }
        // Fire tongues rise, curl and dissolve with a smooth birth/death envelope.
        for(int i=0;i<24;i++){
            double age=(time*.018+scatter(i,311))%1,y=scatter(i,419)*.8,sign=i%2==0?1:-1;
            Vec3 root=ghostPoint(p,right,up,y,sign*.78,time).add(up.scale(age*.7));
            Vec3 last=root;
            for(int j=1;j<=8;j++){
                double u=j/8.0,previous=(j-1)/8.0;
                Vec3 next=root.add(right.scale(sign*(.3+age*.9)*u+Math.sin(u*6-time*.1+i)*.3*u))
                        .add(up.scale((1.1+age*2.2)*u));
                float w0=(float)(.24*Math.sin(Math.PI*previous)+.02),w1=(float)(.24*Math.sin(Math.PI*u)+.02);
                float a0=fade*(float)(Math.sin(Math.PI*age)*Math.sin(Math.PI*previous)*.14),a1=fade*(float)(Math.sin(Math.PI*age)*Math.sin(Math.PI*u)*.14);
                ghostFlame(v,pose,last,next,right,w0,w1,a0,a1);last=next;
            }
        }
    }
    private static Vec3 ghostPoint(Vec3 p,Vec3 right,Vec3 up,double y,double side,double time){
        double[] profile={.1,.3,.42,.52,.6,.64,.72,.85,.45,.45,.58};
        double at=y*10;int index=Math.min(9,(int)at);
        double body=profile[index]+(profile[index+1]-profile[index])*(at-index);
        double ragged=1+.06*Math.sin(y*17-time*.1+(side<0?2:0));
        double width=2.1*body*ragged,shift=(1-y)*.12*Math.sin(y*9+time*.1);
        return p.add(right.scale(shift+side*width)).add(up.scale((y-.5)*4.8));
    }
    private static void ghostFlame(VertexConsumer v,PoseStack pose,Vec3 a,Vec3 b,Vec3 right,float wa,float wb,float aa,float ab){
        // Adjacent segments share their cross section, so the tongue has no dashed seams.
        for(int sign:new int[]{-1,1})for(int band=0;band<2;band++){
            double inner=band==0?0:1,outer=band==0?1:4;
            int color=band==0?GOLD:PURPLE;float ai=band==0?1:.45F,ao=band==0?.45F:0;
            vertex(v,pose,a.add(right.scale(sign*wa*inner)),color,aa*ai);
            vertex(v,pose,b.add(right.scale(sign*wb*inner)),color,ab*ai);
            vertex(v,pose,b.add(right.scale(sign*wb*outer)),PURPLE,ab*ao);
            vertex(v,pose,a.add(right.scale(sign*wa*outer)),PURPLE,aa*ao);
        }
    }
    static void vertex(VertexConsumer v,PoseStack p,Vec3 point,int color,float alpha){
        v.addVertex(p.last(),(float)point.x,(float)point.y,(float)point.z)
                .setColor((color>>16)&255,(color>>8)&255,color&255,Math.round(Math.max(0,Math.min(1,alpha))*255))
                .setUv(.5F,.5F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(p.last(),0,1,0);
    }
}
