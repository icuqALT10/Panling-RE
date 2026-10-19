package icu.icuqalt10.panlingre.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** World-space blade sweeps, independent of the damage windows. */
final class ShiHuangSwordTrails {
    private static final double LIFE=.18, STEP=1.0/60;
    private static final int STEEL=0xC6CBD3;
    private record Slice(double time,Vec3 base,Vec3 tip,int[] colors,boolean moving,boolean command) {}
    private static final class Trail {
        String animation="";
        double time=-1;
        Vec3 localBase,localTip;
        final List<Slice> slices=new ArrayList<>();
    }
    private static final Map<Integer,Trail> trails=new HashMap<>();
    static void clear(){trails.clear();}
    static void retain(List<Integer> ids){trails.keySet().retainAll(ids);}
    static boolean enabled(String clip){
        if(clip.equals("counter_guard_sweep"))return false;
        return clip.equals("emerge")||clip.equals("death")||clip.equals("ultimate_full")
                ||clip.startsWith("combo_")||clip.startsWith("skill_")||clip.startsWith("counter_")||clip.startsWith("ally_");
    }
    static int color(String element){return switch(element){
        case "wood"->0x42D99B;case "fire"->0xF53125;case "earth"->0xDCA832;
        case "metal"->0xF4F7FF;case "water"->0x151B27;default->STEEL;
    };}
    private static int[] palette(ShiHuangEntity boss,double time){
        if(boss.animation().startsWith("ally_")||boss.animation().equals("ultimate_full"))return new int[]{0x9C41FF,0x9C41FF,0xF8C34C};
        var elements=boss.swordElements(time);
        return elements.isEmpty()?new int[]{STEEL}:elements.stream().mapToInt(ShiHuangSwordTrails::color).toArray();
    }
    static void update(ShiHuangEntity boss,float partial){
        String clip=boss.animation();int id=boss.getId();
        if(!enabled(clip)||boss.isInvisible()){trails.remove(id);return;}
        double now=boss.seconds(partial);
        Trail trail=trails.computeIfAbsent(id,k->new Trail());
        if(!clip.equals(trail.animation)||now<trail.time||now-trail.time>.25){
            trail.animation=clip;trail.time=Math.max(0,now-STEP);trail.slices.clear();trail.localBase=trail.localTip=null;
        }
        if(now<=trail.time)return;
        int steps=Math.max(1,(int)Math.ceil((now-trail.time)/STEP));double from=trail.time;
        for(int i=1;i<=steps;i++){
            double t=from+(now-from)*i/steps;
            var frame=ShiHuangPose.sample(clip,t,false);
            if(frame.bones().get("sword").scale().lengthSqr()<.01){trail.slices.clear();trail.localBase=trail.localTip=null;break;}
            // Local movement rejects translation, turning and the presentation hold.
            Vec3 localBase=ShiHuangEntity.swordTrailLocalPoint(frame,false);
            Vec3 localTip=ShiHuangEntity.swordTrailLocalPoint(frame,true);
            double interval=(now-from)/steps;
            boolean moving=trail.localBase!=null&&(localBase.distanceToSqr(trail.localBase)+localTip.distanceToSqr(trail.localTip)>.09*ShiHuangPose.MODEL_SCALE*ShiHuangPose.MODEL_SCALE*interval*interval);
            float p=Mth.clamp(partial-(float)((now-t)*20),0,1);
            trail.slices.add(new Slice(t,boss.swordTrailPoint(frame,false,p),boss.swordTrailPoint(frame,true,p),palette(boss,t),moving,clip.startsWith("ally_")||clip.equals("ultimate_full")));
            trail.localBase=localBase;trail.localTip=localTip;
        }
        trail.time=now;trail.slices.removeIf(s->now-s.time>LIFE);
        // Keep memory bounded even at very high frame rates.
        while(trail.slices.size()>32)trail.slices.removeFirst();
    }
    static void render(VertexConsumer v,PoseStack pose,boolean baseLayer){
        for(var trail:trails.values())for(int i=1;i<trail.slices.size();i++){
            Slice a=trail.slices.get(i-1),b=trail.slices.get(i);
            if(!b.moving)continue;
            double ageA=(trail.time-a.time)/LIFE,ageB=(trail.time-b.time)/LIFE;
            int ca=gradient(a.colors,ageA/.75),cb=gradient(b.colors,ageB/.75);
            float aa=(float)Math.pow(Math.max(0,1-ageA),1.5),ab=(float)Math.pow(Math.max(0,1-ageB),1.5);
            Vec3 blade=b.tip.subtract(b.base),travel=b.tip.subtract(a.tip);
            if(travel.lengthSqr()>1e-8&&blade.cross(travel).lengthSqr()<blade.lengthSqr()*travel.lengthSqr()*.04){
                // A straight thrust has no swept blade area; give its tip path a narrow luminous width.
                Vec3 camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                Vec3 side=camera.subtract(b.tip).cross(travel).normalize().scale(.045*ShiHuangPose.MODEL_SCALE);
                if(side.lengthSqr()<1e-8)side=new Vec3(new org.joml.Vector3f(.045F*ShiHuangPose.MODEL_SCALE,0,0).rotate(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation()));
                float strength=baseLayer?.75F:.3F;
                vertex(v,pose,a.tip.subtract(side),ca,aa*strength);vertex(v,pose,b.tip.subtract(side),cb,ab*strength);
                vertex(v,pose,b.tip.add(side),cb,ab*strength);vertex(v,pose,a.tip.add(side),ca,aa*strength);
            }
            // Taper at the guard and tip instead of drawing a hard rectangular sheet.
            double[] bands={-.025,0,.12,.82,1,1.025};
            float[] strength={0,.15F,.65F,.65F,.1F,0};
            for(int j=1;j<bands.length;j++){
                // A translucent color bed retains red/black in daylight; the additive layer supplies light.
                float factor=baseLayer?1.2F:.22F;
                vertex(v,pose,a.base.lerp(a.tip,bands[j-1]),edgeColor(a,ca,j-1),aa*strength[j-1]*factor);
                vertex(v,pose,b.base.lerp(b.tip,bands[j-1]),edgeColor(b,cb,j-1),ab*strength[j-1]*factor);
                vertex(v,pose,b.base.lerp(b.tip,bands[j]),edgeColor(b,cb,j),ab*strength[j]*factor);
                vertex(v,pose,a.base.lerp(a.tip,bands[j]),edgeColor(a,ca,j),aa*strength[j]*factor);
            }
        }
    }
    private static int edgeColor(Slice slice,int color,int band){return slice.command&&(band<2||band>3)?0xF8C34C:color;}
    static int gradient(int[] colors,double fraction){
        double u=Mth.clamp(fraction,0,1)*(colors.length-1);int i=(int)u,j=Math.min(i+1,colors.length-1);double f=u-i;
        int a=colors[i],b=colors[j],result=0;
        for(int shift:new int[]{16,8,0})result|=(int)Math.round(((a>>shift)&255)*(1-f)+((b>>shift)&255)*f)<<shift;
        return result;
    }
    private static void vertex(VertexConsumer v,PoseStack pose,Vec3 p,int color,float alpha){
        TombLightEffects.vertex(v,pose,p,color,alpha);
    }
}
