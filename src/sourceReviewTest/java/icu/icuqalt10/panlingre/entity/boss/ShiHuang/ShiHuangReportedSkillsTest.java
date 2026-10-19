package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.TombLightPayload;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class ShiHuangReportedSkillsTest {
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> key(String name)throws Exception{return (net.minecraft.network.syncher.EntityDataAccessor<T>)field(ShiHuangEntity.class,name).get(null);}
    public static ServerPlayer player(GameTestHelper h,List<TombLightPayload> packets)throws Exception {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SkillTarget"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){
                if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof TombLightPayload light)packets.add(light);
            }
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);field(ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);h.getLevel().players().add(p);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);p.setHealth(200);return p;
    }
    @GameTest(template="empty",timeoutTicks=280)
    public static void waterWoodCreatesVisibleDamagingStems(GameTestHelper h)throws Exception {
        h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,h.getLevel().getServer());
        class Case {
            ShiHuangEntity boss;ShiHuangCombat combat;ServerPlayer player;Vec3 origin,contact;
            List<TombLightPayload> packets=new ArrayList<>();long released=-1;boolean rage,wall,done;
        }
        var cases=new ArrayList<Case>();
        for(int i=0;i<3;i++){
            var c=new Case();c.rage=i==1;c.wall=i==2;c.origin=h.absoluteVec(new Vec3(i*200,120,0));
            for(int x=-10;x<=35;x++)for(int z=-4;z<=30;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(c.origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            if(c.wall)for(int x=-10;x<=10;x++)for(int y=0;y<6;y++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(c.origin.add(x,y,8)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            c.player=player(h,c.packets);c.player.setPos(c.origin.add(0,0,12));
            c.boss=ModEntities.SHI_HUANG.get().create(h.getLevel());c.boss.moveTo(c.origin,0,0);c.boss.yBodyRot=c.boss.yHeadRot=0;c.boss.setNoAi(true);
            if(c.rage)c.boss.getEntityData().set(ShiHuangReportedSkillsTest.<Long>key("RAGE_START"),h.getLevel().getGameTime());
            c.boss.play("skill_s09_shengxi");c.combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(c.boss);cases.add(c);
        }
        var effects=field(ShiHuangCombat.class,"effects");
        h.onEachTick(()->{try{
            for(var c:cases)if(!c.done){
                c.player.doTick();
                long now=h.getLevel().getGameTime(),age=c.released<0?-1:now-c.released;
                if(c.released>=0&&!c.wall){
                    if(age==60)c.player.setPos(c.origin.add(30,0,25));
                    if(age==75)h.assertTrue(!c.player.hasEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze),"Control persisted after leaving the vines");
                    if(age==80)c.player.setPos(c.contact);
                    if(age==90){c.player.removeEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze);c.player.invulnerableTime=20;}
                    if(age==40)c.boss.play("combo_a01_duanyue");
                }
                c.combat.background();
                if(c.boss.animation().equals("skill_s09_shengxi")){
                    c.combat.tick();
                    if(c.boss.seconds(0)>=c.combat.duration())c.boss.play("idle");
                }
                if(c.released<0){
                    var stems=c.packets.stream().filter(q->q.shape()==TombLightPayload.WOOD_SURGE).toList();
                    if(!stems.isEmpty()){
                        c.released=now;
                        h.assertTrue(c.wall?stems.size()<24:stems.size()==24,"Wrong visible stem count");
                        h.assertTrue(stems.stream().allMatch(q->q.ticks()==200&&q.clockOwner()==-1),"Vines must last 200 world ticks even in rage");
                        h.assertTrue(c.combat.duration()<3,"Vines stalled the emperor for ten seconds");
                        if(c.wall)h.assertTrue(stems.stream().allMatch(q->q.from().z<c.origin.z+8),"Vines passed through a wall");
                        else{var light=stems.get(12);var point=ShiHuangSkillShape.woodPoint(light.from(),light.axis(),0,.1,3.5);c.contact=new Vec3(point.x,light.from().y,point.z);c.player.setPos(c.contact);}
                    }
                }else{
                    if(!c.wall&&(age==30||age==55||age==90||age==150||age==190)){
                        h.assertTrue(c.player.getHealth()==150,"Vines must damage once per cast: "+c.player.getHealth());
                        h.assertTrue(c.player.hasEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze),"Contact did not refresh control at age "+age+" rage="+c.rage);
                        h.assertTrue(c.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)==0&&c.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)==0
                                &&icu.icuqalt10.panlingre.player.ProfessionEquipmentGuard.shouldBlockActions(c.player),"Control did not block movement, jumping and actions");
                    }
                    if(age==199)h.assertTrue(!((List<?>)effects.get(c.combat)).isEmpty(),"Vines ended before ten real seconds");
                    if(age==200)h.assertTrue(((List<?>)effects.get(c.combat)).isEmpty(),"Vines survived past ten real seconds");
                    if(age==215){
                        h.assertTrue(!c.player.hasEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze),"Control did not expire after the vines disappeared");
                        if(c.wall)h.assertTrue(c.player.getHealth()==200,"Vines damaged through a wall");
                        System.out.println("VINE_CONTROL_PASS rage="+c.rage+" wall="+c.wall+" lifetime=200 health="+c.player.getHealth()+" release_after_exit=true");
                        c.done=true;c.combat.stop();c.boss.discard();h.getLevel().players().remove(c.player);
                    }
                }
            }
            if(cases.stream().allMatch(c->c.done))h.succeed();
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chargeAimDoesNotSnapDragon(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,new ArrayList<>());p.setInvulnerable(true);
        try {for(boolean rage:new boolean[]{false,true})for(float targetYaw:new float[]{90,180,-90}) {
        var b=ModEntities.SHI_HUANG.get().create(h.getLevel());b.moveTo(origin,0,0);b.yBodyRot=b.yHeadRot=0;h.getLevel().addFreshEntity(b);
        h.assertTrue(h.getLevel().getEntity(b.getId())==b,"Charge fixture must expose the emperor's shared clock before simulating ticks");
        if(rage)b.getEntityData().set(ShiHuangReportedSkillsTest.<Long>key("RAGE_START"),h.getLevel().getGameTime());
        p.setPos(origin.add(0,0,8));b.setTarget(p);b.play("ally_d02_chongfeng");
        var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat);
        var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);
        var tick=ShiHuangDragonAlly.class.getDeclaredMethod("tickCombo");tick.setAccessible(true);
        long began=h.getLevel().getGameTime();double maxTurn=0,maxMove=0;Vec3 head=null;float previous=0;double speed=rage?1.5:1;
        Vec3 direction=Vec3.directionFromRotation(0,targetYaw);boolean locked=false;
        try {
            for(int realTick=0;realTick<=110/speed;realTick++) {
                double t=realTick*speed;
                b.getEntityData().set(ShiHuangReportedSkillsTest.<Long>key("FROZEN_AT"),began+realTick);
                if(t>=83&&!locked){p.setPos(origin.add(direction.scale(4)));locked=true;}
                else if(locked)p.setPos(origin.subtract(direction.scale(4)));
                tick.invoke(ally);Vec3 next=dragon.posedPart(11,dragon.animationSeconds(0)).center;
                if(t>=82&&t<=92)System.out.println("CHARGE_STEP rage="+rage+" targetYaw="+targetYaw+" t="+t+" clip="+dragon.animation()+" sec="+dragon.animationSeconds(0)+" yaw="+dragon.yBodyRot+" step="+(head==null?0:head.distanceTo(next)));
                if(t>=83&&t<=91){maxTurn=Math.max(maxTurn,Math.abs(net.minecraft.util.Mth.wrapDegrees(dragon.yBodyRot-previous)));maxMove=Math.max(maxMove,head.distanceTo(next));}
                if(t>=90)h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(dragon.yBodyRot-targetYaw))<.001,"Charge changed its locked direction after launch");
                previous=dragon.yBodyRot;head=next;
            }
            System.out.println("CHARGE_RETARGET rage="+rage+" targetYaw="+targetYaw+" max_yaw_step="+maxTurn+" max_head_step="+maxMove);
            h.assertTrue(maxTurn<45*speed,"Dragon instantly rotated at the pointing frame: "+maxTurn);
            h.assertTrue(maxMove<8*speed,"Dragon teleported while retargeting the charge: "+maxMove);
        }finally{combat.stop();b.discard();}
        }h.succeed();}finally{h.getLevel().players().remove(p);}
    }
}
