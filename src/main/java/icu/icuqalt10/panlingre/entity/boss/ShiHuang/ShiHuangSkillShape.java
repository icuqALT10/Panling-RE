package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared attack surfaces: render interpolation and server hit sampling use the same shape. */
public final class ShiHuangSkillShape {
    public static final double GUARD_COUNTER_RADIUS=8;
    public static final int WOOD_LIFE=200;
    public static double woodHeight(double age){
        return 3.5*Mth.clamp(age/2,0,1)*Mth.clamp((WOOD_LIFE-age)/7,0,1);
    }
    public static Vec3 woodPoint(Vec3 origin,Vec3 forward,int stem,double u,double height){
        double phase=origin.x*1.73+origin.z*2.19+stem*2.39996;
        Vec3 side=new Vec3(-forward.z,0,forward.x);
        double twist=phase+u*(4.8+2*Math.sin(phase)),radius=.7*(1-u*.6);
        return origin.add(Math.cos(twist)*radius,height*u*(.95+.5*Math.sin(phase+1)),Math.sin(twist)*radius)
                .add(side.scale((1.7*Math.sin(phase)*u+.65*Math.sin(u*7+phase)*Math.sin(Math.PI*u))*height/3.5))
                .add(forward.scale(1.4*Math.cos(phase*1.7)*u*u*height/3.5));
    }
    public static final double FAN_START=.3, FAN_END=.7, FAN_RADIUS=12;
    public static final double WATER_RISE=5, WATER_INTERVAL=3.25, WATER_LIFE=33;
    public static final int SWORD_DROP_START=5, SWORD_DROP_END=9;
    public static double cardLead(String clip){var action=ShiHuangCombat.ACTIONS.get(clip);return action==null?0:action.cardLead();}
    public static Vec3 crescent(Vec3 center,Vec3 forward,double radius,double angle,double band){
        return crescent(center,forward,new Vec3(0,1,0),radius,angle,band);
    }
    public static Vec3 crescent(Vec3 center,Vec3 forward,Vec3 axis,double radius,double angle,double band){
        double taper=Math.pow(Math.max(0,Math.cos(angle)),.65);
        return center.add(forward.scale(radius*Math.cos(angle)-(1-band)*radius*.34*taper))
                .add(axis.scale(radius*Math.sin(angle)));
    }
    public static Vec3 bladeAxis(String clip,double from,double to,float yaw,Vec3 forward){
        // The blade can point straight forwards halfway through a slash. Use the widest
        // projected blade sample in the real stroke, so that instant cannot flip its plane.
        Vec3 axis=Vec3.ZERO;
        for(int i=0;i<=6;i++){
            var frame=ShiHuangPose.sample(clip,from+(to-from)*i/6,false);
            Vec3 base=ShiHuangPose.point(frame,"sword_trail_start",ShiHuangCombat.PIVOTS.get("sword_trail_start"),yaw,Vec3.ZERO);
            Vec3 tip=ShiHuangPose.point(frame,"sword_trail_end",ShiHuangCombat.PIVOTS.get("sword_trail_end"),yaw,Vec3.ZERO);
            Vec3 blade=tip.subtract(base),candidate=blade.subtract(forward.scale(blade.dot(forward)));
            if(candidate.lengthSqr()>axis.lengthSqr())axis=candidate;
        }
        if(axis.lengthSqr()<.01){
            var before=ShiHuangPose.sample(clip,from,false);var after=ShiHuangPose.sample(clip,to,false);
            Vec3 travel=ShiHuangPose.point(after,"sword_trail_end",ShiHuangCombat.PIVOTS.get("sword_trail_end"),yaw,Vec3.ZERO)
                    .subtract(ShiHuangPose.point(before,"sword_trail_end",ShiHuangCombat.PIVOTS.get("sword_trail_end"),yaw,Vec3.ZERO));
            axis=travel.subtract(forward.scale(travel.dot(forward)));
        }
        return axis.lengthSqr()<.001?new Vec3(0,1,0):axis.normalize();
    }
    public static Vec3 fanPoint(Vec3 origin,float yaw,double time,double radius){
        var frame=ShiHuangPose.sample("skill_s04_chixiao",cardLead("skill_s04_chixiao")+Mth.clamp(time,FAN_START,FAN_END),false);
        Vec3 base=ShiHuangPose.point(frame,"sword_trail_start",ShiHuangCombat.PIVOTS.get("sword_trail_start"),yaw,origin);
        Vec3 tip=ShiHuangPose.point(frame,"sword_trail_end",ShiHuangCombat.PIVOTS.get("sword_trail_end"),yaw,origin);
        Vec3 direction=tip.subtract(base).multiply(1,0,1).normalize();
        return origin.add(0,1.15,0).add(direction.scale(radius));
    }
    public static double waterHeight(double age){return age<0||age>=WATER_LIFE?0:30*Mth.clamp(age/WATER_RISE,0,1);}
    public static double swordDrop(double age){double u=Mth.clamp((age-SWORD_DROP_START)/(SWORD_DROP_END-SWORD_DROP_START),0,1);return 10*(1-u*u*(3-2*u));}
    private ShiHuangSkillShape(){}
}
