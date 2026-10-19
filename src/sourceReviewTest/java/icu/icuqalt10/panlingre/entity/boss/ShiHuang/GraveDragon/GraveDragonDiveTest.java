package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonDiveTest {
    @GameTest(template="empty")
    public static void diveOwnsNeckDuringInversion(GameTestHelper h) throws Exception {
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());
        dragon.beginCombatAnimation("air_dive_slam");
        for(String name:List.of("lookWeight","lookWeightO","lookYaw","lookYawO","lookPitch","lookPitchO")) {
            var field=GraveDragonEntity.class.getDeclaredField(name);field.setAccessible(true);
            field.setFloat(dragon,name.startsWith("lookWeight")?1:30);
        }
        double maxDifference=0;
        for(double t:new double[]{0,.65,1.45,2.5,3.9,4.4,9.7}) {
            var expected=GraveDragonPose.sample("air_dive_slam",t,false);
            var actual=dragon.poseAt(t);
            for(String name:expected.matrices().keySet()) {
                float[] a=expected.matrices().get(name).get(new float[16]);
                float[] b=actual.matrices().get(name).get(new float[16]);
                for(int i=0;i<16;i++)maxDifference=Math.max(maxDifference,Math.abs(a[i]-b[i]));
            }
        }
        System.out.println("DIVE_LOOK matrix difference="+maxDifference);
        h.assertTrue(maxDifference<1e-5,"Target tracking overrides authored dive neck: "+maxDifference);
        h.assertTrue(!GraveDragonEntity.tracksTarget("air_dive_slam"),"Dive still feeds target tracking");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void connectedContinuousOpenArc(GameTestHelper h) {
        var labels=Arrays.asList(GraveDragonEntity.PART_LABELS);
        var spine=GraveDragonPose.spineBones();
        var idle=GraveDragonPose.sample("idle_air",0,false);
        var previous=GraveDragonPose.sample("air_dive_slam",0,false);
        double gap=0,step=0,turn=0,minTurn=0,maxTurn=0,settlingTurn=0;Vector3f previousForward=null;
        for(int sample=0;sample<=582;sample++) {
            double t=sample/60d;
            var frame=GraveDragonPose.sample("air_dive_slam",t,false);
            for(int i=1;i<spine.size();i++) {
                String a=spine.get(i-1),b=spine.get(i);
                float[] p=GraveDragonEntity.PART_BOUNDS[labels.indexOf(a)],q=GraveDragonEntity.PART_BOUNDS[labels.indexOf(b)];
                var joint=new Vector3f((p[3]+q[3])*.5F,(p[4]+q[4])*.5F,(p[5]+q[5])*.5F);
                double allowance=idle.matrices().get(a).transformPosition(new Vector3f(joint))
                        .distance(idle.matrices().get(b).transformPosition(new Vector3f(joint)));
                double distance=frame.matrices().get(a).transformPosition(new Vector3f(joint))
                        .distance(frame.matrices().get(b).transformPosition(new Vector3f(joint)));
                gap=Math.max(gap,distance-allowance);
            }
            for(String name:spine) {
                float[] bounds=GraveDragonEntity.PART_BOUNDS[labels.indexOf(name)];
                var center=new Vector3f(bounds[3],bounds[4],bounds[5]);
                step=Math.max(step,frame.matrices().get(name).transformPosition(new Vector3f(center))
                        .distance(previous.matrices().get(name).transformPosition(new Vector3f(center))));
                if(t>6.8) {
                    var rotation=frame.matrices().get(name).getUnnormalizedRotation(new org.joml.Quaternionf());
                    var old=previous.matrices().get(name).getUnnormalizedRotation(new org.joml.Quaternionf());
                    settlingTurn=Math.max(settlingTurn,Math.toDegrees(2*Math.acos(Math.min(1,Math.abs(rotation.dot(old)))))*60);
                }
            }
            var forward=frame.matrices().get("head").transformDirection(new Vector3f(0,0,-1)).normalize();
            if(previousForward!=null && t<=2.5) {
                turn+=Math.IEEEremainder(Math.atan2(forward.y,-forward.z)
                        -Math.atan2(previousForward.y,-previousForward.z),Math.PI*2);
                minTurn=Math.min(minTurn,turn);maxTurn=Math.max(maxTurn,turn);
            }
            previousForward=forward;previous=frame;
        }
        System.out.println("DIVE_CONTINUITY excess_joint_gap="+gap+" max_center_step_60fps="+step+" windup_pitch_range="+Math.toDegrees(maxTurn-minTurn));
        h.assertTrue(gap<.15,"Dive spine opens between keys: "+gap);
        h.assertTrue(step<3,"Dive pose snaps between render frames: "+step);
        h.assertTrue(settlingTurn<120,"Spine twists sharply while settling into idle: "+settlingTurn+" deg/s");
        System.out.println("DIVE_SETTLE peak_turn_deg_per_second="+settlingTurn);
        h.assertTrue(minTurn>Math.toRadians(-1)&&turn>Math.toRadians(270)&&turn<Math.toRadians(300),
                "Dive must lift then continuously turn backward through the upper half: "+Math.toDegrees(turn));
        h.succeed();
    }

    @GameTest(template="empty")
    public static void backwardUpperHalfKeepsBellyOutside(GameTestHelper h) {
        var raised=GraveDragonPose.sample("air_dive_slam",.65,false).matrices().get("head");
        var crest=GraveDragonPose.sample("air_dive_slam",1.25,false).matrices().get("head");
        var downward=GraveDragonPose.sample("air_dive_slam",2.5,false).matrices().get("head");
        var lift=raised.transformDirection(new Vector3f(0,0,-1)).normalize();
        h.assertTrue(lift.y>.4&&lift.z>.7,"Head should follow the backward arc instead of standing upright");
        h.assertTrue(crest.transformDirection(new Vector3f(0,0,-1)).normalize().z>.98,"Head turned forward instead of backward");
        h.assertTrue(crest.transformDirection(new Vector3f(0,-1,0)).normalize().y>.98,"Belly faces inside at the crest");
        h.assertTrue(downward.transformDirection(new Vector3f(0,0,-1)).normalize().y<-.98,"Head is not vertical at dive handoff");
        float[] bounds=GraveDragonEntity.PART_BOUNDS[11];
        var center=new Vector3f(bounds[3],bounds[4],bounds[5]);
        var start=GraveDragonPose.sample("air_dive_slam",0,false).matrices().get("head").transformPosition(new Vector3f(center));
        double high=start.y,lastZ=-Double.MAX_VALUE;
        for(int frame=39;frame<=150;frame++) {
            var point=GraveDragonPose.sample("air_dive_slam",frame/60d,false).matrices().get("head").transformPosition(new Vector3f(center));
            h.assertTrue(point.z>=lastZ-.02,"Upper arc doubles back before the downward handoff");
            high=Math.max(high,point.y);lastZ=point.z;
        }
        h.assertTrue(high-start.y<6.1,"Backroll climbs too high: "+(high-start.y));
        for(double t:new double[]{.65,1.25,2,2.5}) {
            var frame=GraveDragonPose.sample("air_dive_slam",t,false);double previous=Double.NaN,total=0;
            for(String name:GraveDragonPose.spineBones()) {
                var direction=frame.matrices().get(name).transformDirection(new Vector3f(0,0,-1)).normalize();
                double angle=Math.atan2(direction.y,-direction.z);
                if(!Double.isNaN(previous)) {
                    double bend=Math.IEEEremainder(previous-angle,Math.PI*2);
                    h.assertTrue(bend>-.005&&bend<Math.toRadians(55),"Spine reverses or kinks on the shared arc: "+name+" t="+t);
                    total+=bend;
                }
                previous=angle;
            }
            h.assertTrue(Math.abs(total-Math.PI)<.03,"Whole spine does not occupy one continuous half arc at "+t);
        }
        System.out.println("DIVE_BACKROLL belly_out=true backward=true downward_handoff=true head_rise="+(high-start.y));
        h.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,String name) throws Exception {
        var server=h.getLevel().getServer();
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),name),false);
        var p=new net.minecraft.server.level.ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var spawnProtection=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        spawnProtection.setAccessible(true);spawnProtection.setInt(p,0);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);
        p.setPos(h.absoluteVec(new Vec3(1,80,1)));h.getLevel().addNewPlayer(p);
        return p;
    }

    @GameTest(template="empty")
    public static void mouthLeadsThroughoutDescent(GameTestHelper h) {
        var origin=new Vec3(0,12,0);
        for(Vec3 landing:List.of(new Vec3(0,0,9),new Vec3(-8,0,7),new Vec3(8,0,7))) {
            var path=new GraveDragonDivePath(origin,0,1,landing);
            for(int frame=150;frame<=198;frame++) {
                double t=frame/60d;
                var pose=path.pose(GraveDragonPose.sample("air_dive_slam",t,false),t);
                var transform=GraveDragonPose.modelToEntity(0,0,1);
                double jawY=Arrays.stream(GraveDragonPose.box(pose,"jaw",GraveDragonEntity.PART_BOUNDS[12],transform,origin)
                        .corners()).mapToDouble(p->p.y).min().orElseThrow();
                for(int part:new int[]{75,77}) {
                    double hornY=Arrays.stream(GraveDragonPose.box(pose,GraveDragonEntity.PART_LABELS[part],
                            GraveDragonEntity.PART_BOUNDS[part],transform,origin).corners()).mapToDouble(p->p.y).min().orElseThrow();
                    h.assertTrue(jawY<hornY,"Head turns back to lead with horns: t="+t);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void mouthDrivesIntoCapturedFloor(GameTestHelper h) {
        var pose=GraveDragonPose.sample("air_dive_slam",3.3,false);
        var mouth=GraveDragonPose.box(pose,"jaw",GraveDragonEntity.PART_BOUNDS[12],GraveDragonPose.modelToEntity(0,0,1),new Vec3(0,12,0));
        double bottom=Arrays.stream(mouth.corners()).mapToDouble(p->p.y).min().orElseThrow();
        var edge=Arrays.stream(mouth.corners()).sorted(java.util.Comparator.comparingDouble(p->p.y)).limit(2).toList();
        Vec3 contact=edge.get(0).add(edge.get(1)).scale(.5);
        System.out.println("DIVE_MOUTH contact="+contact);
        h.assertTrue(bottom < -.9 && bottom > -1.5,"Mouth did not penetrate floor: "+bottom);
        h.assertTrue(contact.multiply(1,0,1).distanceTo(new Vec3(0,0,9))<.2,"Mouth missed captured landing point: "+contact);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void landingLocksNearestAtHandoff(GameTestHelper h) throws Exception {
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());
        dragon.setPos(h.absoluteVec(new Vec3(20,20,20)));
        var combat=new GraveDragonCombat(dragon);
        var begin=GraveDragonCombat.class.getDeclaredMethod("begin",String.class,List.class,boolean.class,net.minecraft.server.level.ServerPlayer.class);
        begin.setAccessible(true);
        var tick=GraveDragonCombat.class.getDeclaredMethod("tickDiveSlam",double.class);tick.setAccessible(true);
        var first=player(h,"FirstTarget");var second=player(h,"HandoffTarget");
        try {
            first.setPos(dragon.position().add(1,0,0));second.setPos(dragon.position().add(8,0,0));
            begin.invoke(combat,"air_dive_slam",List.of(first.position()),false,first);
            tick.invoke(combat,2.45);h.assertTrue(dragon.diveLanding()==null,"Landing locked before handoff");
            first.setPos(dragon.position().add(20,0,0));second.setPos(dragon.position().add(3,0,4));
            tick.invoke(combat,2.5);Vec3 locked=dragon.diveLanding();
            h.assertTrue(locked!=null&&locked.multiply(1,0,1).distanceTo(second.position().multiply(1,0,1))<.01,"Did not select nearest player at handoff");
            first.setPos(dragon.position());second.setPos(second.position().add(30,0,0));
            for(int t=51;t<=194;t++)tick.invoke(combat,t/20d);
            h.assertTrue(locked.equals(dragon.diveLanding()),"Landing followed a moving or newly nearest player");
            System.out.println("DIVE_LOCK nearest_at_handoff=true fixed_after_movement=true point="+locked);
        } finally {first.discard();second.discard();dragon.discard();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void recoveryRoundsOutwardBeforeIdle(GameTestHelper h) {
        var path=new GraveDragonDivePath(new Vec3(0,12,0),0,1,new Vec3(0,0,9));
        var points=new ArrayList<Vec3>();
        for(double t:new double[]{3.3,4.6,6,9.7}) {
            var pose=path.pose(GraveDragonPose.sample("air_dive_slam",t,false),t);
            points.add(GraveDragonPose.box(pose,"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),new Vec3(0,12,0)).center);
        }
        var departure=path.pose(GraveDragonPose.sample("air_dive_slam",3.5,false),3.5);
        var outward=GraveDragonPose.box(departure,"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),new Vec3(0,12,0)).center;
        h.assertTrue(outward.z>points.getFirst().z+1&&points.get(1).y>points.getFirst().y+3,"Recovery did not round forward and upward after impact");
        h.assertTrue(points.get(2).distanceTo(points.getFirst().lerp(points.getLast(),.5))>3,"Recovery collapsed into a straight translation");
        var idle=GraveDragonPose.box(GraveDragonPose.sample("idle_air",3.2,false),"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),path.rootAt(9.7));
        h.assertTrue(points.getLast().distanceTo(idle.center)<.01,"Recovery does not join idle_air");
        var returning=path.pose(GraveDragonPose.sample("air_dive_slam",9.69,false),9.69);
        var fourth=GraveDragonPose.box(returning,"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),new Vec3(0,12,0));
        h.assertTrue(fourth.center.z>path.landing.z+21&&fourth.center.y>path.landing.y+30,"Fourth waypoint must be ahead of and above the impact");
        h.assertTrue(fourth.center.distanceTo(idle.center)<.02,"Fourth waypoint does not join idle at the new position");
        double lastForward=points.getFirst().z;
        for(int tick=198;tick<=408;tick++) {
            double t=tick/60d;
            var pose=path.pose(GraveDragonPose.sample("air_dive_slam",t,false),t);
            var head=new org.joml.Matrix4f(GraveDragonPose.modelToEntity(0,0,1)).mul(pose.matrices().get("head"));
            var up=head.transformDirection(new Vector3f(0,1,0)).normalize();
            var direction=head.transformDirection(new Vector3f(0,0,-1)).normalize();
            var point=GraveDragonPose.box(pose,"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),path.origin).center;
            h.assertTrue(up.y>=-.001&&direction.z>0,"Recovery flips belly upward or turns backward: "+t);
            h.assertTrue(point.z>=lastForward-.001,"Recovery doubles back after impact: "+t);
            lastForward=point.z;
        }
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());
        dragon.setPos(path.origin);dragon.beginCombatAnimation("air_dive_slam");dragon.lockDivePath(path.origin,0,path.landing);
        for(double t:new double[]{3.3,4.5,6.7,7.8,9.7}) {
            dragon.advanceDiveRoot(t);
            var moving=GraveDragonPose.box(dragon.poseAt(t),"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),dragon.position());
            var fixed=GraveDragonPose.box(path.pose(GraveDragonPose.sample("air_dive_slam",t,false),t),"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),path.origin);
            h.assertTrue(moving.center.distanceTo(fixed.center)<.001,"Entity motion was applied on top of the curve");
        }
        dragon.beginCombatAnimation("idle_air");
        var finished=GraveDragonPose.box(dragon.poseAt(0),"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),dragon.position());
        h.assertTrue(finished.center.distanceTo(idle.center)<.02,"Idle snapped back to the takeoff position");
        dragon.discard();
        var before=path.pose(GraveDragonPose.sample("air_dive_slam",3.29,false),3.29).matrices().get("head");
        var after=path.pose(GraveDragonPose.sample("air_dive_slam",3.31,false),3.31).matrices().get("head");
        float[] b=GraveDragonEntity.PART_BOUNDS[11];var center=new Vector3f(b[3],b[4],b[5]);
        Vec3 previous=new Vec3(before.transformPosition(new Vector3f(center)));
        Vec3 next=new Vec3(after.transformPosition(new Vector3f(center)));
        var impact=path.pose(GraveDragonPose.sample("air_dive_slam",3.3,false),3.3).matrices().get("head");
        Vec3 contact=new Vec3(impact.transformPosition(new Vector3f(center)));
        Vec3 incoming=contact.subtract(previous),outgoing=next.subtract(contact);
        h.assertTrue(outgoing.length()>.15,"Head stops at impact");
        h.assertTrue(incoming.normalize().dot(outgoing.normalize())>.97,"Impact breaks the curve tangent");
        h.assertTrue(Math.abs(GraveDragonPose.duration("air_dive_slam")-9.7)<.001,"Combat action still ends before the extended flight");
        var flying=path.pose(GraveDragonPose.sample("air_dive_slam",6.7,false),6.7);
        var restingPose=GraveDragonPose.sample("idle_air",6.7*3.2/9.7,false);
        double remainingBend=0;
        for(String bone:GraveDragonPose.spineBones()) {
            var flightRotation=flying.matrices().get(bone).getUnnormalizedRotation(new org.joml.Quaternionf());
            var restRotation=restingPose.matrices().get(bone).getUnnormalizedRotation(new org.joml.Quaternionf());
            remainingBend=Math.max(remainingBend,2*Math.acos(Math.min(1,Math.abs(flightRotation.dot(restRotation)))));
        }
        h.assertTrue(remainingBend>Math.toRadians(15),"Body settled into idle before completing the extended arc");
        for(double t:new double[]{4.5,5.8,6.7,8.2}) {
            var exported=GraveDragonPose.sample("air_dive_slam",t,false);
            var runtime=path.pose(exported,t);
            for(String bone:GraveDragonPose.spineBones()) {
                float[] bounds=GraveDragonEntity.PART_BOUNDS[Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(bone)];
                var point=new Vector3f(bounds[3],bounds[4],bounds[5]);
                h.assertTrue(exported.matrices().get(bone).transformPosition(new Vector3f(point)).distance(
                        runtime.matrices().get(bone).transformPosition(point))<.1,"Flight blend applied twice after export: "+bone+" t="+t);
            }
        }
        for(String bone:GraveDragonPose.spineBones()) {
            var finish=path.pose(GraveDragonPose.sample("air_dive_slam",9.69,false),9.69).matrices().get(bone).getUnnormalizedRotation(new org.joml.Quaternionf());
            var resting=GraveDragonPose.sample("idle_air",3.2,false).matrices().get(bone).getUnnormalizedRotation(new org.joml.Quaternionf());
            h.assertTrue(Math.abs(finish.dot(resting))>.999,"Head/neck does not lift back into idle: "+bone);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void distantLandingKeepsDescending(GameTestHelper h) {
        for(double distance:new double[]{0,30,70,90}) {
            var path=new GraveDragonDivePath(new Vec3(0,12,0),0,1,new Vec3(0,0,distance));
            double lastY=Double.POSITIVE_INFINITY;
            for(int frame=150;frame<=198;frame++) {
                double t=frame/60d;var pose=path.pose(GraveDragonPose.sample("air_dive_slam",t,false),t);
                var head=GraveDragonPose.box(pose,"head",GraveDragonEntity.PART_BOUNDS[11],GraveDragonPose.modelToEntity(0,0,1),new Vec3(0,12,0));
                h.assertTrue(head.center.y<=lastY+.001,"Long Bezier handles made the dive rise again before impact");lastY=head.center.y;
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void landingDamageUsesCapturedPoint(GameTestHelper h) throws Exception {
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());dragon.setPos(h.absoluteVec(new Vec3(1,80,1)));
        var combat=new GraveDragonCombat(dragon);var p=player(h,"LandingTarget");
        var begin=GraveDragonCombat.class.getDeclaredMethod("begin",String.class,List.class,boolean.class,net.minecraft.server.level.ServerPlayer.class);begin.setAccessible(true);
        var strike=GraveDragonCombat.class.getDeclaredMethod("strikeLanding",String.class,float.class,long.class);strike.setAccessible(true);
        try {
            Vec3 landing=dragon.position().add(60,0,0);p.setPos(landing);
            begin.invoke(combat,"air_dive_slam",List.of(landing),false,p);dragon.lockDivePath(dragon.position(),0,landing);
            strike.invoke(combat,"landing:fixed",50f,0L);
            h.assertTrue(Math.abs(p.getHealth()-(1000-100F*2/3))<.01,"Landing damage was still centered on the unmoving entity root");
            p.setHealth(1000);p.invulnerableTime=0;p.setPos(landing.add(25,0,0));
            strike.invoke(combat,"landing:moved",50f,1L);
            h.assertTrue(p.getHealth()==1000,"Landing damage followed the moving target");
        } finally {p.discard();dragon.discard();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void damageReducedByOneThirdAtAllThreeSinks(GameTestHelper h) throws Exception {
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());dragon.setPos(h.absoluteVec(new Vec3(20,5,20)));
        var combat=new GraveDragonCombat(dragon);
        var damage=GraveDragonCombat.class.getDeclaredMethod("damage",net.minecraft.server.level.ServerPlayer.class,String.class,float.class,int.class,long.class);
        damage.setAccessible(true);
        for(float base:new float[]{3,6,10,30,50}) {
            var p=player(h,"DragonMelee");
            try {
                damage.invoke(combat,p,"test",base,0,0L);
                h.assertTrue(Math.abs(p.getHealth()-(1000-base*2*2/3))<.01,"Direct damage did not decrease by one third: "+base+" health="+p.getHealth());
            } finally {p.discard();}
        }
        var fire=player(h,"DragonFire");
        try {
            var ball=new GraveDragonFireballEntity(h.getLevel(),dragon,new Vec3(1,0,0),20,false);
            ball.onHit(new net.minecraft.world.phys.EntityHitResult(fire,fire.position()));
            h.assertTrue(Math.abs(fire.getHealth()-(1000-40F*2/3))<.01,"Fireball damage did not decrease by one third: "+fire.getHealth());
        } finally {fire.discard();}
        var rockTarget=player(h,"DragonRock");
        try {
            var rock=new GraveDragonRockEntity(h.getLevel(),dragon,new Vec3(1,0,0),20);
            rock.setPos(rockTarget.position().add(-1,0,0));rock.tick();
            h.assertTrue(Math.abs(rockTarget.getHealth()-(1000-40F*2/3))<.01,"Rock damage did not decrease by one third: "+rockTarget.getHealth());
        } finally {rockTarget.discard();}
        h.succeed();
    }
}
