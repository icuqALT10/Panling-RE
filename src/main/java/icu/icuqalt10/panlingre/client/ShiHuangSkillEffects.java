package icu.icuqalt10.panlingre.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangSkillShape;
import icu.icuqalt10.panlingre.network.TombLightPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Elemental attack surfaces, with a translucent color bed beneath the emissive core. */
final class ShiHuangSkillEffects {
    private static final List<Vec3[]> SWORD=swordMesh();
    static void draw(VertexConsumer v,PoseStack p,TombLightPayload e,double age,boolean glow){
        if(age<0)return;
        float alpha=(glow?.24F:.8F)*(float)Mth.clamp((e.ticks()-age)/3,0,1);
        switch(e.shape()){
            case TombLightPayload.CRESCENT,TombLightPayload.ELEMENT_CRESCENT->crescent(v,p,e,age,alpha,glow);
            case TombLightPayload.FALLING_SWORD->fallingSword(v,p,e,age,alpha,glow);
            case TombLightPayload.WATER_COLUMN->water(v,p,e,age,alpha,glow);
            case TombLightPayload.LAVA_BOULDER->lava(v,p,e,age,alpha,glow);
            case TombLightPayload.HIT_SPARKS->sparks(v,p,e,age,alpha,glow);
            case TombLightPayload.WOOD_SURGE->wood(v,p,e,age,alpha,glow);
        }
    }
    private static void crescent(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        Vec3 forward=e.to().subtract(e.from()).normalize(),side=forward.cross(e.axis()).normalize();
        Vec3 center=e.from().lerp(e.to(),Math.min(1,age/(e.ticks()-1)));
        // Two faces and a colored outer rim give the moon blade thickness from every viewing angle.
        for(int face:new int[]{-1,1})for(int i=0;i<64;i++)for(int band=0;band<24;band++){
            for(int corner=0;corner<4;corner++){
                double a=(i+(corner==1||corner==2?1:0))*Math.PI/64-Math.PI/2;
                double r=(band+(corner>=2?1:0))/24.0;
                double jitter=.10*Math.sin(age*3.7+a*31)+.065*Math.sin(age*6.3-a*53);
                Vec3 point=ShiHuangSkillShape.crescent(center,forward,e.axis(),e.width(),a,r)
                        .add(side.scale(face*(.065+.045*Math.sin(Math.PI*r))+jitter)).add(forward.scale(jitter*r));
                double edge=.065+.028*Math.sin(a*9+age*.23)+.018*Math.sin(a*21-age*.37);
                int color=softGradient(r,edge,.24+.035*Math.sin(a*13-age*.2),e.color());
                float strength=(float)(Math.pow(Math.max(0,Math.cos(a)),.2)*(glow?1:.78)*(.78+.22*Math.sin(age*4.1+a*35+r*17)));
                TombLightEffects.vertex(v,p,point,color,alpha*strength);
            }
        }
        for(int i=0;i<64;i++){
            double a=i*Math.PI/64-Math.PI/2,b=(i+1)*Math.PI/64-Math.PI/2;
            Vec3 x=ShiHuangSkillShape.crescent(center,forward,e.axis(),e.width(),a,1),y=ShiHuangSkillShape.crescent(center,forward,e.axis(),e.width(),b,1);
            quad(v,p,x.add(side.scale(-.07)),y.add(side.scale(-.07)),y.add(side.scale(.07)),x.add(side.scale(.07)),e.color(),alpha);
        }
        if(glow)for(int strand=0;strand<3;strand++)for(int i=0;i<40;i++){
            double a=i*Math.PI/40-Math.PI/2,b=(i+1)*Math.PI/40-Math.PI/2;
            double band=.2+strand*.3;
            Vec3 x=ShiHuangSkillShape.crescent(center,forward,e.axis(),e.width(),a,band+.07*Math.sin(a*43-age*4.8+strand));
            Vec3 y=ShiHuangSkillShape.crescent(center,forward,e.axis(),e.width(),b,band+.07*Math.sin(b*43-age*4.8+strand));
            TombLightEffects.beam(v,p,x,y,0xFFFFFF,e.color(),.018F,alpha*.65F);
        }
    }
    static void fan(VertexConsumer v,PoseStack p,ShiHuangEntity boss,float partial,boolean glow){
        double now=boss.seconds(partial)-ShiHuangSkillShape.cardLead(boss.animation());if(!boss.animation().equals("skill_s04_chixiao")||now<.3||now>.85)return;
        double end=Math.min(.7,now);float alpha=(glow?.23F:.7F)*(float)Mth.clamp((.85-now)/.15,0,1);
        Vec3 origin=boss.getPosition(partial);float yaw=Mth.rotLerp(partial,boss.yBodyRotO,boss.yBodyRot);
        Vec3 center=origin.add(0,1.15,0);
        Vec3[] directions=new Vec3[49];
        for(int i=0;i<=48;i++)directions[i]=ShiHuangSkillShape.fanPoint(origin,yaw,.3+(end-.3)*i/48,1).subtract(center);
        for(int i=0;i<48;i++)for(int band=0;band<32;band++)for(int corner=0;corner<4;corner++){
            int edgeIndex=i+(corner==1||corner==2?1:0);
            double t=.3+(end-.3)*edgeIndex/48;
            double u=(band+(corner>=2?1:0))/32.0;
            double radius=12*u;
            double ripple=(.035+.10*u)*Math.sin(now*115+t*170+u*24);
            Vec3 point=center.add(directions[edgeIndex].scale(radius)).add(0,ripple,0);
            double edge=4+.55*Math.sin(t*39+now*4)+.28*Math.sin(t*91-now*6);
            int color=softGradient(radius,edge,3.5+.5*Math.sin(t*53-now*3),0xEF1806);
            TombLightEffects.vertex(v,p,point,color,alpha);
        }
        if(glow)for(int i=0;i<32;i++){
            double t=.3+(end-.3)*i/31;
            Vec3 tip=ShiHuangSkillShape.fanPoint(origin,yaw,t,12);
            double length=.5+.7*(.5+.5*Math.sin(now*95+i*2.3));
            TombLightEffects.beam(v,p,tip,tip.add(0,length,0),0xFFAD64,0xEF1806,.09F,alpha*2);
        }
    }
    private static int gradient(double u,int color){return net.minecraft.util.FastColor.ARGB32.lerp((float)Mth.clamp((u-.15)/.85,0,1),0xFFFFFF,color);}
    private static int softGradient(double distance,double edge,double width,int color){
        double u=Mth.clamp((distance-edge)/width,0,1);u=u*u*(3-2*u);
        return net.minecraft.util.FastColor.ARGB32.lerp((float)u,0xFFFFFF,color);
    }
    static void cards(VertexConsumer v,PoseStack p,ShiHuangEntity boss,float partial,boolean glow){
        var elements=boss.cardElements();if(elements.isEmpty())return;
        double time=boss.seconds(partial),start=boss.cardDissolve(),end=boss.cardGatherEnd();
        if(boss.animation().equals("skill_s07_rongyan")&&time>=start&&time<1.75){
            var frame=boss.pose(partial);
            Vec3 base=boss.swordTrailPoint(frame,false,partial),tip=boss.swordTrailPoint(frame,true,partial);
            float charge=(float)(Mth.clamp((time-start)/(end-start),0,1)*(1-Mth.clamp((time-1.7)/.05,0,1)));
            TombLightEffects.beam(v,p,base,tip,0xFF5825,0xDCA832,glow?.13F:.07F,charge*(glow?.5F:.85F));
        }
        if(time<start||time>end+.1)return;
        var released=icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.sample(boss.animation(),start,false);
        var current=boss.pose(partial);
        Vec3 destination=boss.swordTrailPoint(current,false,partial).lerp(boss.swordTrailPoint(current,true,partial),.45);
        double progress=Mth.clamp((time-start)/(end-start),0,1);
        for(String element:elements){
            Vec3 origin=boss.cardPoint(released,element,partial);
            int color=element.equals("water")?0x577BBA:ShiHuangSwordTrails.color(element);
            for(int i=0;i<36;i++){
                double phase=i*2.39996,delay=(i%6)*.022;
                double u=Mth.clamp((progress-delay)/(1-delay),0,1);u=u*u*(3-2*u);
                Vec3 offset=new Vec3(Math.cos(phase)*.40,Math.sin(phase)*.58,Math.sin(i*1.7)*.12).scale((1-u)*icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE);
                Vec3 point=origin.lerp(destination,u).add(offset).add(0,Math.sin(u*Math.PI)*.55*icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.MODEL_SCALE,0);
                float alpha=(glow?.65F:.9F)*(float)(1-Mth.clamp((progress-.88)/.12,0,1));
                spark(v,p,point,color,glow?.055:.032,alpha);
            }
        }
    }
    private static void spark(VertexConsumer v,PoseStack p,Vec3 center,int color,double size,float alpha){
        Vec3 x=new Vec3(size,0,0),y=new Vec3(0,size,0),z=new Vec3(0,0,size);
        quad(v,p,center.subtract(x).subtract(y),center.add(x).subtract(y),center.add(x).add(y),center.subtract(x).add(y),color,alpha);
        quad(v,p,center.subtract(z).subtract(y),center.add(z).subtract(y),center.add(z).add(y),center.subtract(z).add(y),color,alpha);
    }
    private static void sparks(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        double u=age/e.ticks();
        for(int i=0;i<32;i++){
            double y=1-2*(i+.5)/32,angle=i*2.39996,r=Math.sqrt(1-y*y);
            Vec3 direction=new Vec3(Math.cos(angle)*r,y,Math.sin(angle)*r);
            Vec3 point=e.from().add(direction.scale(e.width()*(.15+2.1*u))).add(0,-u*u*.55,0);
            spark(v,p,point,e.color(),(glow?.09:.045)*(1-u),alpha*(float)(1-u));
            if(glow)TombLightEffects.beam(v,p,point.subtract(direction.scale(.13*(1-u))),point,0xF5F9FF,e.color(),.018F,alpha*.6F);
        }
    }
    private static Vec3 rockPoint(int face,double u,double w,double radius,double age){
        double warp=(1-Math.abs(u))*(1-Math.abs(w));
        double source=u;
        u+=.16*warp*Math.sin(u*9+w*7+face*1.7);
        w+=.16*warp*Math.sin(source*6-w*11+face*2.3);
        Vec3 raw=switch(face){case 0->new Vec3(1,u,w);case 1->new Vec3(-1,u,w);case 2->new Vec3(u,1,w);case 3->new Vec3(u,-1,w);case 4->new Vec3(u,w,1);default->new Vec3(u,w,-1);};
        Vec3 direction=raw.normalize();
        double irregular=.95+.035*Math.sin(direction.x*9+direction.y*7)*Math.cos(direction.z*8);
        Vector3f point=direction.scale(.78).add(raw.scale(.22)).scale(radius*irregular).toVector3f();
        point.rotate(new Quaternionf().rotationYXZ((float)age*.15F,(float)age*.095F,.2F));
        return new Vec3(point);
    }
    private static void lava(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        Vec3 center=e.from().lerp(e.to(),Math.min(1,age/(e.ticks()-1)));
        for(int face=0;face<6;face++)for(int row=0;row<5;row++)for(int column=0;column<5;column++){
            for(int corner=0;corner<4;corner++){
                double u=-1+(row+(corner==1||corner==2?1:0))*2.0/5;
                double w=-1+(column+(corner>=2?1:0))*2.0/5;
                TombLightEffects.vertex(v,p,center.add(rockPoint(face,u,w,e.width(),age)),0xFF681A,alpha*(glow?1.6F:1));
            }
        }
        if(glow){Vec3 behind=e.from().subtract(e.to()).normalize();
            for(int i=0;i<20;i++){
                double u=(age*.13+i*.061)%1;Vec3 offset=new Vec3(Math.sin(i*2.4),Math.cos(i*2.4),Math.sin(i*1.7)).scale(.7*(1-u));
                spark(v,p,center.add(behind.scale(e.width()+u*4)).add(offset),0xFF7C25,.09*(1-u),alpha*(float)(1-u));
            }
        }
    }
    /** Opaque crust writes depth before the molten core, including under shader packs. */
    static void lavaCrust(VertexConsumer v,PoseStack p,TombLightPayload e,double age){
        if(e.shape()!=TombLightPayload.LAVA_BOULDER)return;
        Vec3 center=e.from().lerp(e.to(),Math.min(1,age/(e.ticks()-1)));
        for(int face=0;face<6;face++)for(int row=0;row<5;row++)for(int column=0;column<5;column++)for(int corner=0;corner<4;corner++){
            double margin=.035+.04*(.5+.5*Math.sin(face*2.3+row*4.7+column*3.1));
            double u=-1+(row+(corner==1||corner==2?1-margin:margin))*2/5;
            double w=-1+(column+(corner>=2?1-margin:margin))*2/5;
            int shade=45+(face*7+row*5+column*3)%28;
            int color=(shade<<16)|((shade/2)<<8)|shade/3;
            TombLightEffects.vertex(v,p,center.add(rockPoint(face,u,w,e.width()*1.03,age)),color,1);
        }
    }
    private static void fallingSword(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        if(age<ShiHuangSkillShape.SWORD_DROP_END&&glow){
            TombLightEffects.ring(v,p,e.from().add(0,.055,0),.85,0xFFFFFF,0xFFFFFF,.065F,1);
            TombLightEffects.ring(v,p,e.from().add(0,10,0),.85,0xFFFFFF,0xFFFFFF,.065F,1);
        }
        if(age<ShiHuangSkillShape.SWORD_DROP_START)return;
        Vec3 tip=e.from().add(0,ShiHuangSkillShape.swordDrop(age)-.25,0);
        for(var face:SWORD)quad(v,p,tip.add(face[0]),tip.add(face[1]),tip.add(face[2]),tip.add(face[3]),0xEDF7FF,alpha*(glow?.5F:.4F));
    }
    private static Vec3 rootPoint(TombLightPayload e,int stem,double u,double height){
        return ShiHuangSkillShape.woodPoint(e.from(),e.axis(),stem,u,height);
    }
    static void woodStems(VertexConsumer v,PoseStack p,TombLightPayload e,double age){
        if(e.shape()!=TombLightPayload.WOOD_SURGE||age<0)return;
        double height=ShiHuangSkillShape.woodHeight(age);if(height<=0)return;
        // Solid green stems write depth so overlapping water cannot bleach the whole thicket.
        for(int stem=0;stem<3;stem++)for(int row=0;row<16;row++){
            for(int side=0;side<8;side++)for(int corner=0;corner<4;corner++){
                double u=(row+(corner>=2?1:0))/16.0;
                double a=(side+(corner==1||corner==2?1:0))*Math.PI/4;
                double radius=.27*Math.pow(1-u,.65);
                Vec3 point=rootPoint(e,stem,u,height).add(Math.cos(a)*radius,0,Math.sin(a)*radius);
                TombLightEffects.vertex(v,p,point,side%3==0?0x419D54:0x195C39,1);
            }
        }
    }
    private static void wood(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        double height=ShiHuangSkillShape.woodHeight(age);if(height<=0)return;
        // Thin colored streams stay on the stems; only scattered tips use additive light.
        if(!glow)for(int stem=0;stem<3;stem++)for(int row=0;row<16;row++){
            double u=row/16.0,next=(row+1)/16.0;
            Vec3 a=rootPoint(e,stem,u,height),b=rootPoint(e,stem,next,height);
            double twist=age*.45+u*14+stem;
            Vec3 offset=new Vec3(Math.cos(twist),0,Math.sin(twist)).scale(.29*(1-u)+.04);
            Vec3 offsetB=new Vec3(Math.cos(twist+.875),0,Math.sin(twist+.875)).scale(.29*(1-next)+.04);
            TombLightEffects.beam(v,p,a.add(offset),b.add(offsetB),0x3E9BA7,0x27787E,.025F,alpha*.4F);
        }
        if(!glow)for(int stem=0;stem<3;stem++)for(int branch=0;branch<2;branch++){
            double u=.4+branch*.23,angle=e.from().x*1.73+e.from().z*2.19+stem*2.4+branch*1.8;
            Vec3 a=rootPoint(e,stem,u,height),b=a.add(Math.cos(angle)*.55,height*.12,Math.sin(angle)*.55);
            Vec3 c=b.add(Math.cos(angle+.6)*.4,height*.17,Math.sin(angle+.6)*.4);
            TombLightEffects.beam(v,p,a,b,0x448E39,0x235E31,.12F,alpha);
            TombLightEffects.beam(v,p,b,c,0x63B944,0x235E31,.07F,alpha);
        }
        if(glow)for(int i=0;i<6;i++){
            double u=(age*.09+i*.071)%1,a=i*2.39996+age*.08;
            Vec3 point=e.from().add(Math.cos(a)*e.width()*(1-u*.5),u*height,Math.sin(a)*e.width()*(1-u*.5));
            spark(v,p,point,i%2==0?0x72FBB4:0x78DFFF,.04*(1-u),alpha*.25F);
        }
    }
    private static void water(VertexConsumer v,PoseStack p,TombLightPayload e,double age,float alpha,boolean glow){
        double local=age-e.to().x,height=ShiHuangSkillShape.waterHeight(local);if(height<=0)return;
        alpha*=Math.min(1,(ShiHuangSkillShape.WATER_LIFE-local)/6);
        for(int ring=0;ring<12;ring++)for(int i=0;i<40;i++)for(int corner=0;corner<4;corner++){
            double a=(i+(corner==1||corner==2?1:0))*Math.PI/20,y=(ring+(corner>=2?1:0))*height/12;
            double radius=e.width()*(.96+.04*Math.sin(a*7-y*3+age*2));
            Vec3 point=e.from().add(Math.cos(a)*radius,y,Math.sin(a)*radius);
            TombLightEffects.vertex(v,p,point,gradient(.35+.65*(.5+.5*Math.sin(a*5-y*3+age*1.8)),0x207FED),alpha*.65F);
        }
        // Continuous helical sheets move upward around the surface, like winding streams.
        for(int strand=0;strand<3;strand++)for(int step=0;step<120;step++)for(int corner=0;corner<4;corner++){
            double y=height*(step+(corner==1||corner==2?1:0))/120;
            double a=strand*Math.PI*2/3+y*.85-age*.42;
            double across=corner>=2?1:-1;
            double r=e.width()*(1.025+.015*Math.sin(y*2-age));
            Vec3 point=e.from().add(Math.cos(a+across*.13)*r,y,Math.sin(a+across*.13)*r);
            TombLightEffects.vertex(v,p,point,glow?0xB6F4FF:0x7AD8F5,alpha*(glow?.8F:.9F));
        }
    }
    private static List<Vec3[]> swordMesh(){
        var stream=ShiHuangSkillEffects.class.getClassLoader().getResourceAsStream("assets/panlingre/geo/entity/boss/shihuang/shihuang_spirit.geo.json");
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var bones=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            List<Vec3[]> faces=new ArrayList<>();int[][] indices={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
            for(var value:bones){var bone=value.getAsJsonObject();if(!bone.get("name").getAsString().equals("sword"))continue;
                for(var cv:bone.getAsJsonArray("cubes")){
                    var cube=cv.getAsJsonObject();var o=cube.getAsJsonArray("origin");var size=cube.getAsJsonArray("size");Vec3[] corners=new Vec3[8];
                    for(int i=0;i<8;i++){
                        Vector3f point=new Vector3f(o.get(0).getAsFloat()+((i&1)>0?size.get(0).getAsFloat():0),o.get(1).getAsFloat()+((i&2)>0?size.get(1).getAsFloat():0),o.get(2).getAsFloat()+((i&4)>0?size.get(2).getAsFloat():0));
                        if(cube.has("rotation")){var pivot=cube.getAsJsonArray("pivot");var r=cube.getAsJsonArray("rotation");Vector3f center=new Vector3f(pivot.get(0).getAsFloat(),pivot.get(1).getAsFloat(),pivot.get(2).getAsFloat());
                            point.sub(center).rotate(new Quaternionf().rotationZYX(r.get(2).getAsFloat()*Mth.DEG_TO_RAD,r.get(1).getAsFloat()*Mth.DEG_TO_RAD,r.get(0).getAsFloat()*Mth.DEG_TO_RAD)).add(center);}
                        corners[i]=new Vec3((point.x+10.00062)*.1125,(point.y+12.59921)*.1125,point.z*.1125);
                    }
                    for(var f:indices)faces.add(new Vec3[]{corners[f[0]],corners[f[1]],corners[f[2]],corners[f[3]]});
                }
            }return faces;
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    private static void quad(VertexConsumer v,PoseStack p,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,float alpha){
        for(var point:new Vec3[]{a,b,c,d})TombLightEffects.vertex(v,p,point,color,alpha);
    }
}
