package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Nonrepeating gestures that fit the cooldown, over a continuous ground gait. */
final class ShiHuangPressure {
    private static final double TAKEOFF=.16, FLIGHT=.52;
    enum Move { PRESS, BACK_HOP, SIDE_HOP, FLASH, FEINT, GUARD, POINT, WATCH }
    private final ShiHuangEntity boss;
    private final ArrayDeque<Move> recent=new ArrayDeque<>();
    private final java.util.EnumSet<Move> used=java.util.EnumSet.noneOf(Move.class);
    private Move current;
    private int side=1,phase;
    private Vec3 from,to;
    private double height;
    private boolean flashed,flashUsed,flashPending;

    ShiHuangPressure(ShiHuangEntity boss){this.boss=boss;}
    void stop(){current=null;from=to=null;flashPending=false;}
    void beginCooldown(){stop();used.clear();flashUsed=false;}
    void finishCounter(){stop();used.addAll(java.util.EnumSet.allOf(Move.class));flashUsed=true;}
    boolean active(){return current!=null;}
    boolean busy(){return active()&&phase==1;}
    boolean airborne(){return active()&&phase==1&&to!=null&&height>0;}

    boolean onHurt(LivingEntity attacker){
        if(flashUsed||boss.getRandom().nextFloat()>=.1F)return false;
        // Finish the current somersault before reacting; never snap an inverted body to the floor.
        if(airborne()){flashUsed=true;flashPending=true;return true;}
        beginFlash(attacker);
        return flashUsed=current==Move.FLASH;
    }

    private void beginFlash(LivingEntity target){
        flashPending=false;current=Move.FLASH;side=boss.getRandom().nextBoolean()?1:-1;
        beginGesture(target);
    }

    boolean tick(LivingEntity target){
        if(!boss.animation().equals("idle")&&!boss.animation().startsWith("interval_"))return false;
        if(target==null){stop();boss.play("idle");return true;}
        Vec3 forward=target.position().subtract(boss.position()).multiply(1,0,1).normalize();
        double distance=target.position().subtract(boss.position()).horizontalDistance();
        if(phase==2&&boss.seconds(0)>=.1)current=null;
        if(current==null){
            var movement=target.position().subtract(new Vec3(target.xo,target.yo,target.zo));
            double lateral=movement.dot(new Vec3(-forward.z,0,forward.x));
            side=Math.abs(lateral)>.015?(lateral>0?-1:1):(boss.getRandom().nextBoolean()?1:-1);
            current=choose(distance,movement.dot(forward),Math.abs(lateral),boss.attackCooldownTicks()-(used.isEmpty()?7:0));
            if(current==null){phase=2;boss.play("interval_walk");boss.walkDuringCooldown(target,true);return true;}
            phase=0;
            boss.play("interval_walk");
        }
        double time=boss.seconds(0);
        if(phase==0&&time>=(used.isEmpty()?.35:.1))beginGesture(target);
        else if(phase==1&&time>=ShiHuangPose.duration(boss.animation())){
            if(flashPending)beginFlash(target);
            else {to=null;height=0;phase=2;boss.play("interval_walk");}
        }
        time=boss.seconds(0);
        if(phase!=1||to==null){
            boss.face(target.position(),(float)(8*boss.actionSpeed()));
            boss.walkDuringCooldown(target,!(phase==0&&time>=.15
                    &&(current==Move.BACK_HOP||current==Move.SIDE_HOP)));
            return true;
        }
        boss.pauseIntervalGait();
        if(to!=null){
            if(current==Move.FLASH){
                if(!flashed&&time>=.2){boss.reposition(to,target.position(),true);flashed=true;}
            }else if(time>=TAKEOFF){
                double u=Math.min(1,(time-TAKEOFF)/FLIGHT);
                Vec3 point=from.lerp(to,u).add(0,4*height*u*(1-u),0);
                boss.move(MoverType.SELF,point.subtract(boss.position()));boss.setDeltaMovement(Vec3.ZERO);boss.fallDistance=0;
            }
        }
        return true;
    }

    private void beginGesture(LivingEntity target){
        phase=1;from=boss.position();to=null;height=0;flashed=false;
        Vec3 forward=boss.getLookAngle().multiply(1,0,1).normalize();
        double distance=target.position().subtract(from).horizontalDistance();
        Vec3 movement=target.position().subtract(new Vec3(target.xo,target.yo,target.zo)).multiply(1,0,1);
        if(current==Move.BACK_HOP&&(distance>=3||movement.horizontalDistance()<=.015)
                ||current==Move.SIDE_HOP&&distance>=5)
            current=choose(distance,movement.dot(forward),Math.abs(movement.dot(new Vec3(-forward.z,0,forward.x))));
        if(current==null){phase=2;boss.play("interval_walk");return;}
        Vec3 across=new Vec3(-forward.z,0,forward.x).scale(side);
        Vec3 offset=switch(current){
            case BACK_HOP->forward.scale(-3.3);
            case SIDE_HOP->across.scale(3.3).add(forward.scale(-.75));
            case FLASH->across.scale(3.75).add(forward.scale(.6));
            default->Vec3.ZERO;
        };
        if(offset.lengthSqr()>0){
            height=current==Move.BACK_HOP?1.5:current==Move.SIDE_HOP?.32:0;
            to=landing(offset,height);
            if(to==null&&current!=Move.BACK_HOP){side=-side;to=landing(offset.subtract(across.scale(current==Move.FLASH?7.5:6.6)),height);}
            if(to==null){
                height=0;
                if(used.contains(Move.GUARD)||durationTicks(Move.GUARD)+1>boss.attackCooldownTicks()){
                    current=null;phase=2;boss.play("interval_walk");return;
                }
                current=Move.GUARD;
            }
        }
        used.add(current);
        recent.addLast(current);if(recent.size()>2)recent.removeFirst();
        String clip="interval_"+current.name().toLowerCase(java.util.Locale.ROOT);
        if(current==Move.SIDE_HOP||current==Move.FLASH)clip+=side>0?"_l":"_r";
        boss.play(clip);
    }

    private Vec3 landing(Vec3 offset,double lift){
        Vec3 destination=boss.standingPosition(from.add(offset));
        if(destination==null||Math.abs(destination.y-from.y)>.5)return null;
        var controller=boss.tomb();
        if(controller!=null&&!controller.scene().emperorArena().deflate(2,0,2).contains(destination))return null;
        Vec3 previous=from;
        for(int i=1;i<=16;i++){
            double u=i/16.0;Vec3 point=from.lerp(destination,u).add(0,4*lift*u*(1-u),0);
            if(!boss.level().noCollision(boss,boss.getBoundingBox().move(previous.subtract(from)).deflate(0,.01,0).expandTowards(point.subtract(previous))))return null;
            if(lift==0&&boss.standingPosition(point)==null)return null;
            previous=point;
        }
        return destination;
    }

    Move choose(double distance,double away,double lateral){
        return choose(distance,away,lateral,boss.attackCooldownTicks());
    }
    private int durationTicks(Move move){
        String clip="interval_"+move.name().toLowerCase(java.util.Locale.ROOT);
        if(move==Move.SIDE_HOP||move==Move.FLASH)clip+="_l";
        return (int)Math.ceil(ShiHuangPose.duration(clip)*20/(move==Move.SIDE_HOP?1.25:1));
    }
    private Move choose(double distance,double away,double lateral,int availableTicks){
        List<Move> choices=new ArrayList<>();
        for(var move:Move.values()){
            if(recent.contains(move)||used.contains(move)||durationTicks(move)+1>availableTicks)continue;
            if((move==Move.BACK_HOP||move==Move.SIDE_HOP)
                    &&(recent.peekLast()==Move.BACK_HOP||recent.peekLast()==Move.SIDE_HOP||recent.peekLast()==Move.FLASH))continue;
            int weight=switch(move){
                case PRESS->distance>2.5?(away>.02?8:5):0;
                case BACK_HOP->distance<3&&Math.hypot(away,lateral)>.015?5:0;
                case SIDE_HOP->distance<5?(lateral>.02?6:2):0;
                case FLASH->0;
                case FEINT->distance<5?4:0;
                case GUARD->distance<5?2:0;
                case POINT->distance>2?2:0;
                case WATCH->1;
            };
            for(int i=0;i<weight;i++)choices.add(move);
        }
        return choices.isEmpty()?null:choices.get(boss.getRandom().nextInt(choices.size()));
    }
}
