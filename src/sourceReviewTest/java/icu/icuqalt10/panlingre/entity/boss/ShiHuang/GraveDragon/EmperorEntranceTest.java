package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix4f;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class EmperorEntranceTest {
    @GameTest(template="empty",timeoutTicks=510)
    public static void entranceSpawnsOneDragonAtReveal(GameTestHelper h) {
        var level=h.getLevel();
        var boss=icu.icuqalt10.panlingre.init.ModEntities.SHI_HUANG.get().create(level);
        var landing=h.absoluteVec(new Vec3(0,4,0));
        boss.moveTo(landing.add(0,0,30),180,0);boss.yBodyRot=boss.yHeadRot=180;
        boss.setEnhanced(true);boss.beginEntrance();level.addFreshEntity(boss);
        long start=level.getGameTime();java.util.UUID[] dragonId={null};
        double[] ascentHeight=new double[2];
        int exitTicks=(int)Math.round(GraveDragonPose.duration("ally_emerge_exit")*20);
        h.onEachTick(()->{
            long t=level.getGameTime()-start;
            if(t<=ShiHuangEntity.ENTRANCE_END_TICK)boss.tickEntrance(landing);
            if(t<ShiHuangEntity.ENTRANCE_REVEAL_TICK){
                h.assertTrue(boss.isInvisible(),"Textured emperor appeared before the column/apparition sequence ended");
                h.assertTrue(boss.position().distanceTo(landing)<.001,"Emperor still flew between the two columns");
            }
            var dragons=level.getEntitiesOfClass(GraveDragonEntity.class,boss.getBoundingBox().inflate(140));
            if(t<ShiHuangEntity.ENTRANCE_REVEAL_TICK)h.assertTrue(dragons.isEmpty(),"Dragon appeared before emerge");
            if(t==ShiHuangEntity.ENTRANCE_REVEAL_TICK){
                h.assertTrue(dragons.size()==1,"Reveal did not create exactly one dragon");dragonId[0]=dragons.getFirst().getUUID();
                h.assertTrue(boss.animation().equals("emerge")&&dragons.getFirst().animation().equals("ally_emerge"),"Entrance clocks did not start together");
                h.assertTrue(dragons.getFirst().poseAt(0).bones().get("head").scale().x<.03,"Dragon flashed full size at spawn");
            }
            if(t>ShiHuangEntity.ENTRANCE_REVEAL_TICK){
                h.assertTrue(dragons.size()==1&&dragons.getFirst().getUUID().equals(dragonId[0]),"Entrance created another dragon");
                if(t<ShiHuangEntity.ENTRANCE_END_TICK)h.assertTrue(dragons.getFirst().animation().equals("ally_emerge"),"Dragon departed before emperor finished");
                if(t==ShiHuangEntity.ENTRANCE_END_TICK+25)h.assertTrue(dragons.getFirst().animation().equals("ally_emerge_exit"),"Missing ascent transition");
                if(t==ShiHuangEntity.ENTRANCE_END_TICK){
                    var dragon=dragons.getFirst();
                    ascentHeight[0]=dragon.posedPart(11,dragon.animationSeconds(0)).center.y;
                    ascentHeight[1]=dragon.posedPart(20,dragon.animationSeconds(0)).center.y;
                }
                if(t==ShiHuangEntity.ENTRANCE_END_TICK+16){
                    var dragon=dragons.getFirst();
                    double headRise=dragon.posedPart(11,dragon.animationSeconds(0)).center.y-ascentHeight[0];
                    double tailRise=dragon.posedPart(20,dragon.animationSeconds(0)).center.y-ascentHeight[1];
                    h.assertTrue(headRise>6&&tailRise<headRise*.35,"Ascent lifted the whole coil instead of leading with the head");
                }
                if(t==ShiHuangEntity.ENTRANCE_END_TICK+exitTicks+5){
                    h.assertTrue(dragons.getFirst().animation().equals("ally_orbit"),"Ascent did not join orbit");
                    h.assertTrue(!boss.isInvulnerable()&&!boss.isInvisible(),"Entrance left boss invulnerable/invisible");
                    dragons.getFirst().discard();boss.discard();h.succeed();
                }
            }
        });
    }
    @GameTest(template="empty")
    public static void columnShakeOnlyReachesItsInstance(GameTestHelper h){
        var level=h.getLevel();var origin=h.absoluteVec(new Vec3(0,3,0));
        var bounds=new net.minecraft.world.phys.AABB(origin,origin.add(177,106,763));
        var positions=java.util.List.of(origin.add(88,20,700),origin.add(88,20,10),origin.add(-1,20,700),origin.add(600,20,700));
        var players=new java.util.ArrayList<net.minecraft.server.level.ServerPlayer>();
        int[] counts=new int[4];
        try{
            for(int i=0;i<positions.size();i++){
                final int index=i;
                var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"column-review-"+i),false);
                var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
                player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,cookie){
                    @Override public void send(net.minecraft.network.protocol.Packet<?> packet){
                        if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof icu.icuqalt10.panlingre.network.TombLightPayload effect){
                            h.assertTrue(effect.shape()==icu.icuqalt10.panlingre.network.TombLightPayload.ARENA_SHAKE,"Wrong effect packet");
                            h.assertTrue(new net.minecraft.world.phys.AABB(effect.from(),effect.to()).equals(bounds),"Shake lost its instance bounds");
                            counts[index]++;
                        }
                    }
                };
                player.setPos(positions.get(i));players.add(player);level.players().add(player);
            }
            icu.icuqalt10.panlingre.network.TombLightPayload.shakeArena(level,bounds,85);
            h.assertTrue(counts[0]==1&&counts[1]==1,"Shake missed a player in the boss room or distant entrance room");
            h.assertTrue(counts[2]==0&&counts[3]==0,"Shake reached outside the instance or another slot");
            players.getFirst().setPos(origin.add(-1,20,700));
            icu.icuqalt10.panlingre.network.TombLightPayload.shakeArena(level,bounds,60);
            h.assertTrue(counts[0]==1&&counts[1]==2&&counts[2]==0&&counts[3]==0,"Second column shook a player who left the instance");
            System.out.println("COLUMN_SHAKE_CHECK near/far instance players receive; outside/other-slot/departed players excluded");
            h.succeed();
        }finally{
            for(var player:players){level.players().remove(player);player.getTextFilter().leave();player.discard();}
        }
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void emergenceFitsBodyAndJoinsFlight(GameTestHelper h) {
        h.assertTrue(GraveDragonPose.duration("ally_emerge")==ShiHuangPose.duration("emerge"),"Dragon and emperor entrance lengths differ");
        h.assertTrue(ShiHuangEntity.ENTRANCE_END_TICK-ShiHuangEntity.ENTRANCE_REVEAL_TICK==100,"Entrance no longer covers the entire action");
        compare(h,GraveDragonPose.sample("ally_emerge",5,false),GraveDragonPose.sample("ally_emerge_exit",0,false));
        double exitDuration=GraveDragonPose.duration("ally_emerge_exit");
        compare(h,GraveDragonPose.sample("ally_emerge_exit",exitDuration,false),GraveDragonPose.sample("ally_orbit",0,false));
        var hold=GraveDragonPose.sample("ally_emerge",2.85,false);
        var middle=GraveDragonPose.sample("ally_emerge",1.6,false);
        h.assertTrue(middle.bones().get("tail_tip").scale().x>.99&&middle.bones().get("head").scale().x<.03,"Emergence must reveal the tail before the head");
        var floating=GraveDragonPose.sample("ally_emerge",3.5,false);
        var tailRest=hold.matrices().get("tail_tip").transformDirection(new org.joml.Vector3f(0,0,1)).normalize();
        float maxTailSway=0;
        for(String bone:java.util.List.of("head","neck_01","tail_tip")){
            int index=java.util.Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(bone);
            var a=GraveDragonPose.box(hold,bone,GraveDragonEntity.PART_BOUNDS[index],new Matrix4f(),Vec3.ZERO);
            var b=GraveDragonPose.box(floating,bone,GraveDragonEntity.PART_BOUNDS[index],new Matrix4f(),Vec3.ZERO);
            h.assertTrue(a.center.distanceTo(b.center)>.015,"Coiled dragon froze instead of gently floating: "+bone);
        }
        for(int tick=0;tick<=100;tick++) {
            var frame=GraveDragonPose.sample("ally_emerge",tick/20.0,false);
            if(tick>=57)maxTailSway=Math.max(maxTailSway,tailRest.angle(frame.matrices().get("tail_tip").transformDirection(new org.joml.Vector3f(0,0,1)).normalize()));
            for(String bone:GraveDragonPose.spineBones()) {
                int index=java.util.Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(bone);
                var box=GraveDragonPose.box(frame,bone,GraveDragonEntity.PART_BOUNDS[index],new Matrix4f(),Vec3.ZERO);
                h.assertTrue(Double.isFinite(box.center.x)&&Double.isFinite(box.center.y),"Non-finite entrance pose");
                if(tick>=57){
                    h.assertTrue(Math.abs(frame.bones().get(bone).scale().x-1)<.0001,"Segment still growing when emperor points: "+bone);
                    var held=GraveDragonPose.box(hold,bone,GraveDragonEntity.PART_BOUNDS[index],new Matrix4f(),Vec3.ZERO);
                    h.assertTrue(box.center.distanceTo(held.center)<1,"Dragon moved out of its gentle coiled flight wave: "+bone);
                    if(bone.equals("head")){
                        var bounds=box.enclosingAabb();var eye=new Vec3(0,3.5,0);
                        var nearest=new Vec3(net.minecraft.util.Mth.clamp(eye.x,bounds.minX,bounds.maxX),net.minecraft.util.Mth.clamp(eye.y,bounds.minY,bounds.maxY),net.minecraft.util.Mth.clamp(eye.z,bounds.minZ,bounds.maxZ));
                        h.assertTrue(nearest.distanceTo(eye)<=3,"Dragon head is farther than three blocks from emperor");
                    }
                }
            }
            var spine=GraveDragonPose.spineBones();
            for(int i=1;i<spine.size();i++){
                String a=spine.get(i-1),b=spine.get(i);
                if(tick<56)h.assertTrue(frame.bones().get(b).scale().x+.00001>=frame.bones().get(a).scale().x,"Growth did not travel from tail to head: "+a+" / "+b);
                var ba=GraveDragonEntity.PART_BOUNDS[java.util.Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(a)];
                var bb=GraveDragonEntity.PART_BOUNDS[java.util.Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(b)];
                var joint=new org.joml.Vector3f((ba[3]+bb[3])*.5F,(ba[4]+bb[4])*.5F,(ba[5]+bb[5])*.5F);
                var pa=frame.matrices().get(a).transformPosition(new org.joml.Vector3f(joint));
                var pb=frame.matrices().get(b).transformPosition(new org.joml.Vector3f(joint));
                h.assertTrue(pa.distance(pb)<.002,"Disconnected spine joint: "+a+" / "+b+" tick="+tick);
            }
        }
        h.assertTrue(maxTailSway>.02,"Coiled body lacks the segment wave of fly across the hold");
        var orbit=GraveDragonPose.sample("ally_orbit",0,false);
        var finalMount=GraveDragonPose.box(orbit,"shihuang_mount",new float[]{0,0,0,0,155.5F/16,-3F/16},new Matrix4f(),Vec3.ZERO).center;
        var travel=new Vec3(-28,35.99,0).subtract(finalMount);
        var headTrail=new java.util.ArrayList<Vec3>();
        for(int tick=0;tick<=640;tick++){
            double phase=tick/640.0,u=phase*phase*(3-2*phase);
            var frame=GraveDragonPose.sample("ally_emerge_exit",exitDuration*phase,false);
            headTrail.add(GraveDragonPose.box(frame,"head",GraveDragonEntity.PART_BOUNDS[11],new Matrix4f(),travel.scale(u)).center);
            for(String bone:GraveDragonPose.spineBones()){
                int index=java.util.Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(bone);
                var center=GraveDragonPose.box(frame,bone,GraveDragonEntity.PART_BOUNDS[index],new Matrix4f(),travel.scale(u)).center;
                if(center.y>17)h.assertTrue(headTrail.stream().mapToDouble(center::distanceTo).min().orElseThrow()<1.3,"Segment cut across the head's ascent route: "+bone+" tick="+tick);
            }
            var spine=GraveDragonPose.spineBones();
            for(int i=1;i<spine.size();i++){
                String a=spine.get(i-1),b=spine.get(i);
                h.assertTrue(jointDistance(frame,a,b)<=jointDistance(orbit,a,b)+.02,"Ascent separated spine segments: "+a+" / "+b+" tick="+tick);
            }
        }
        System.out.println("ENTRANCE_CHECK 101 poses: connected spine, individual growth and flight transition endpoints passed");
        h.succeed();
    }
    private static float jointDistance(GraveDragonPose.Frame frame,String a,String b){
        var labels=java.util.Arrays.asList(GraveDragonEntity.PART_LABELS);
        var ba=GraveDragonEntity.PART_BOUNDS[labels.indexOf(a)];var bb=GraveDragonEntity.PART_BOUNDS[labels.indexOf(b)];
        var joint=new org.joml.Vector3f((ba[3]+bb[3])*.5F,(ba[4]+bb[4])*.5F,(ba[5]+bb[5])*.5F);
        return frame.matrices().get(a).transformPosition(new org.joml.Vector3f(joint))
                .distance(frame.matrices().get(b).transformPosition(new org.joml.Vector3f(joint)));
    }
    private static void compare(GameTestHelper h,GraveDragonPose.Frame a,GraveDragonPose.Frame b) {
        for(String bone:a.bones().keySet()) {
            float[] ma=a.matrices().get(bone).get(new float[16]),mb=b.matrices().get(bone).get(new float[16]);
            for(int i=0;i<16;i++)h.assertTrue(Math.abs(ma[i]-mb[i])<.0005,"Entrance transition jumps at "+bone);
        }
    }
}
