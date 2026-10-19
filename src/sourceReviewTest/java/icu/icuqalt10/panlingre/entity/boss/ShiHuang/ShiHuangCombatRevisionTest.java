package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class ShiHuangCombatRevisionTest {
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception {var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> key(String name)throws Exception{return (net.minecraft.network.syncher.EntityDataAccessor<T>)field(ShiHuangEntity.class,name).get(null);}
    private static void time(ShiHuangEntity b,int ticks)throws Exception{b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("START"),b.level().getGameTime()-ticks);}
    private static ShiHuangEntity boss(GameTestHelper h,Vec3 origin){var b=icu.icuqalt10.panlingre.init.ModEntities.SHI_HUANG.get().create(h.getLevel());b.moveTo(origin,0,0);b.yBodyRot=b.yBodyRotO=b.yHeadRot=0;b.setNoGravity(true);return b;}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,String name)throws Exception{
        var method=ShiHuangPressureTest.class.getDeclaredMethod("player",GameTestHelper.class,String.class);method.setAccessible(true);
        return (net.minecraft.server.level.ServerPlayer)method.invoke(null,h,name);
    }
    private static void floor(GameTestHelper h,Vec3 origin){for(int x=-10;x<=10;x++)for(int z=-10;z<=45;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);}

    @GameTest(template="empty",timeoutTicks=100,batch="stroke_approach")
    public static void approachEveryStrokeAndCooldown(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"stroke-moving-target");p.setInvulnerable(true);int strokes=0;
        try{
            for(boolean rage:new boolean[]{false,true})for(var action:ShiHuangCombat.ACTIONS.values())if(action.name().startsWith("combo_")){
                var b=boss(h,origin);b.play(action.name());if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                double speed=rage?1.5:1;
                for(int i=0;i<action.windows().size();i++){
                    var w=action.windows().get(i);p.setPos(b.position().add(0,0,5.8));
                    int from=i==0?0:(int)Math.ceil((action.windows().get(i-1).to()+.05)*20/speed);
                    for(int tick=from;tick<=(int)Math.ceil(w.from()*20/speed);tick++){
                        Vec3 before=b.position();time(b,tick);b.tick();
                        h.assertTrue(b.position().distanceTo(before)<2.5,"Approach teleported instead of moving: "+action.name());
                    }
                    h.assertTrue(ShiHuangCombat.meleeIntersects(b.position(),b.yBodyRot,w.thrust(),p.getBoundingBox()),"Unreachable stroke "+action.name()+" #"+i+" rage="+rage+" distance="+b.distanceTo(p));
                    h.assertTrue(b.blinkTicks(0)<0,"Attack approach created a teleport ghost");strokes++;
                }b.discard();
            }
            for(double range:new double[]{2,6,6.01}){
                var b=boss(h,origin);p.setPos(origin.add(0,0,range));b.play("combo_a02_guanjia");
                for(int tick=0;tick<=7;tick++){time(b,tick);b.tick();}
                h.assertTrue(range==6?b.getZ()>origin.z+3:b.position().distanceToSqr(origin)<1e-8,"Wrong six-block dash boundary / needless point-blank dash: "+range);b.discard();
            }
            var b=boss(h,origin);p.setPos(origin.add(0,0,9));field(ShiHuangEntity.class,"nextAttack").setLong(b,0);b.tick();
            h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation())&&b.distanceTo(p)<2&&b.blinkTicks(0)==0,"Cooldown did not blink to a distant player");b.discard();
            b=boss(h,origin);p.setPos(origin.add(0,0,5));field(ShiHuangEntity.class,"nextAttack").setLong(b,0);b.tick();h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation())&&b.position().distanceToSqr(origin)<1e-8&&b.blinkTicks(0)<0,"Close cooldown entry teleported");b.discard();
            System.out.println("STROKE_APPROACH_PASS strokes="+strokes+", normal/rage, six-block stroke approach cap, point-blank stays, distant cooldown blink");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="moving_stroke_approach")
    public static void approachRechecksMovingTargetDuringWindup(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"windup-distance-change");p.setInvulnerable(true);int checked=0;
        var actions=new java.util.ArrayList<>(ShiHuangCombat.ACTIONS.values().stream().filter(a->a.name().startsWith("combo_")).toList());
        actions.sort(java.util.Comparator.comparing(a->!a.name().equals("combo_a15_cuopai")));
        try{
            for(boolean rage:new boolean[]{false,true})for(var action:actions)for(int i=0;i<action.windows().size();i++)for(double initialRange:new double[]{2,6.5}){
                var b=boss(h,origin);b.play(action.name());if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                double speed=rage?1.5:1;var w=action.windows().get(i);
                int from=i==0?0:(int)Math.ceil((action.windows().get(i-1).to()+.05)*20/speed),strike=(int)Math.ceil(w.from()*20/speed);
                p.setPos(origin.add(0,0,initialRange));time(b,from);b.tick();
                h.assertTrue(b.position().distanceTo(origin)<.001,"Unexpected approach before target changes range");
                p.setPos(origin.add(0,0,5.2));
                for(int tick=from+1;tick<=strike;tick++){time(b,tick);b.tick();}
                h.assertTrue(ShiHuangCombat.meleeIntersects(b.position(),b.yBodyRot,w.thrust(),p.getBoundingBox()),"Windup range change ignored: "+action.name()+" #"+i+" rage="+rage+" initial="+initialRange+" distance="+b.distanceTo(p));
                h.assertTrue(b.blinkTicks(0)<0,"Approach became a teleport");
                Vec3 settled=b.position();p.setPos(settled.add(0,0,5.2));
                for(int tick=strike+1;tick<=(int)Math.floor(w.to()*20/speed);tick++){time(b,tick);b.tick();}
                h.assertTrue(b.position().distanceTo(settled)<.001,"Approach kept chasing during the swing");
                b.discard();checked++;
            }
            System.out.println("MOVING_WINDUP_APPROACH_PASS cases="+checked+", all 20 combos, every stroke, normal/rage, close retreat and entry from beyond six blocks, no swing chase");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ranged_skill_position")
    public static void rangedSkillsOnlyTurn(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"ranged-skill-target");p.setInvulnerable(true);int checked=0;
        try{
            for(boolean rage:new boolean[]{false,true})for(var action:ShiHuangCombat.ACTIONS.values())if(action.name().startsWith("skill_")||action.name().startsWith("counter_")){
                var b=boss(h,origin);b.addMana(20);b.play(action.name());if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                boolean fire=action.name().equals("skill_s04_chixiao");p.setPos(origin.add(fire?0:5.8,0,fire?5.8:0));
                double speed=rage?1.5:1;
                for(int tick=0;tick<=(int)Math.ceil(ShiHuangPose.duration(action.name())*20/speed)&&b.animation().equals(action.name());tick++){time(b,tick);b.tick();}
                h.assertTrue(fire?b.position().distanceTo(origin)>2:b.position().distanceTo(origin)<.001,"Incorrect skill approach: "+action.name()+" rage="+rage+" travel="+b.position().distanceTo(origin));
                if(!fire)h.assertTrue(Math.abs(b.yBodyRot)>1,"Ranged skill did not turn toward target: "+action.name());
                b.discard();checked++;
            }
            System.out.println("RANGED_SKILL_POSITION_PASS cases="+checked+", single fire approaches, other regular skills and counter only turn, normal/rage");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=80,batch="sword_rain_cooldown")
    public static void swordRainCooldownIgnoresLinger(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"sword-rain-cooldown");p.setInvulnerable(true);
        var packets=new java.util.ArrayList<icu.icuqalt10.panlingre.network.TombLightPayload>();
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false);
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){
                if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof icu.icuqalt10.panlingre.network.TombLightPayload light&&light.shape()==icu.icuqalt10.panlingre.network.TombLightPayload.FALLING_SWORD)packets.add(light);
            }
        };
        var tickCombat=ShiHuangEntity.class.getDeclaredMethod("tickCombat");tickCombat.setAccessible(true);
        try{
            for(boolean rage:new boolean[]{false,true}){
                packets.clear();var b=boss(h,origin);p.setPos(origin.add(0,0,2));b.play("skill_s02_jianzhen");
                if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                long start=b.level().getGameTime();double speed=rage?1.5:1;int end=(int)Math.ceil(ShiHuangPose.duration(b.animation())*20/speed);
                for(int tick=0;tick<=end;tick++){b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+tick);tickCombat.invoke(b);}
                h.assertTrue(b.animation().equals("idle")&&field(ShiHuangEntity.class,"nextAttack").getLong(b)>b.level().getGameTime(),"Sword rain waits for white swords to disappear before cooldown rage="+rage);
                h.assertTrue(packets.size()==12,"Sword rain lost its twelve white swords");
                for(var light:packets){double age=b.actionClock(0)-light.clockStart();h.assertTrue(age>=ShiHuangSkillShape.SWORD_DROP_END&&age<light.ticks()-20,"White sword must have landed and remain visible during cooldown");}
                b.discard();
            }
            System.out.println("SWORD_RAIN_COOLDOWN_PASS normal/rage, cooldown at animation end, twelve landed swords retain visual lifetime");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=80,batch="interval_guard_damage")
    public static void raisedIntervalGuardBlocksAttack(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"guard-attacker");p.setPos(origin.add(0,0,2));int[] sounds={0};
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==h.getLevel()&&e.getSound().value()==net.minecraft.sounds.SoundEvents.ANVIL_LAND)sounds[0]++;};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{
            for(boolean rage:new boolean[]{false,true})for(int tick:new int[]{4,5,10,16,17}){
                var b=boss(h,origin);
                b.moveTo(origin,0,0);b.play("interval_guard");if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                var pressure=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(b);field(ShiHuangPressure.class,"flashUsed").setBoolean(pressure,true);
                time(b,tick);float health=b.getHealth();int before=sounds[0];boolean blocked=tick>=5&&tick<=16;
                boolean damaged=b.hurt(b.damageSources().playerAttack(p),10);
                h.assertTrue(blocked?!damaged&&b.getHealth()==health:damaged&&b.getHealth()<health,"Wrong guard damage window tick="+tick+" rage="+rage);
                h.assertTrue(sounds[0]-before==(blocked?1:0),"Anvil sound must occur exactly once for a blocked hit");
                if(blocked){
                    b.hurt(b.damageSources().playerAttack(p),10);h.assertTrue(b.getHealth()==health&&sounds[0]==before+2,"Repeated guard hit did not block and ring once");
                    h.assertTrue(b.animation().equals("interval_guard"),"Guard hit triggered a flash instead of holding the sword");
                    b.setNoAi(true);b.invulnerableTime=0;h.assertTrue(b.hurt(b.damageSources().playerAttack(p),10),"Inactive guard blocked an attack");
                }b.discard();
            }
            System.out.println("INTERVAL_GUARD_PASS raised window 0.25–0.8 s, normal/rage, one anvil sound per hit, repeated blocks, vulnerable lift/recovery and NoAI");h.succeed();
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="lava_projectile_damage")
    public static void lavaBoulderHitsStandingPlayer(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"lava-boulder-target");
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        try{
            for(boolean rage:new boolean[]{false,true})for(double range:new double[]{2,4,6,8,16}){
                p.setHealth(200);p.invulnerableTime=0;p.clearFire();p.setPos(origin.add(0,0,range));
                var b=boss(h,origin);b.setTarget(p);b.play("skill_s07_rongyan");if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);double speed=rage?1.5:1;boolean logged=false;
                for(int tick=0;tick<=100/speed;tick++){
                    time(b,tick);combat.tick();var effects=(java.util.List<?>)field(ShiHuangCombat.class,"effects").get(combat);
                    if(!logged&&!effects.isEmpty()){System.out.println("LAVA_PATH origin="+origin+" rage="+rage+" range="+range+" "+effects.getFirst());logged=true;}
                }
                h.assertTrue(p.getHealth()==120,"Lava boulder failed to deal one 80-damage hit to standing player range="+range+" rage="+rage+" health="+p.getHealth());
                b.discard();
            }
            for(int x=-3;x<=3;x++)for(int y=0;y<=5;y++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,y,6)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            p.setPos(origin.add(0,0,8));p.setHealth(200);p.invulnerableTime=0;
            var blocked=boss(h,origin);blocked.setTarget(p);blocked.play("skill_s07_rongyan");
            var blockedCombat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(blocked);
            for(int tick=0;tick<=100;tick++){time(blocked,tick);blockedCombat.tick();}
            h.assertTrue(p.getHealth()==200,"Lava boulder dealt damage through a wall");blocked.discard();
            System.out.println("LAVA_BOULDER_DAMAGE_PASS 2/4/6/8/16 blocks normal/rage, one 80-damage hit per projectile, wall blocks damage");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="dragon_command_aim")
    public static void dragonCommandsAimAndStayStill(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));
        for(int x=-45;x<=45;x++)for(int z=-45;z<=45;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var p=player(h,"dragon-command-target");var near=player(h,"dragon-command-nearest");p.setInvulnerable(true);near.setInvulnerable(true);
        var tick=ShiHuangDragonAlly.class.getDeclaredMethod("tickCombo");tick.setAccessible(true);
        try{
            for(String name:new String[]{"ally_d01_longting","ally_d02_chongfeng","ally_d03_longxi"}){
                var b=boss(h,origin);h.getLevel().addFreshEntity(b);p.setPos(origin.add(0,0,8));near.setPos(origin.add(0,0,10));b.setTarget(p);b.play("combo_a15_cuopai");b.approachTarget(p,1.4,7);b.setDeltaMovement(0,0,1);
                b.play(name);h.assertTrue(b.getDeltaMovement().lengthSqr()<1e-8&&field(ShiHuangEntity.class,"rapidDestination").get(b)==null,"Command inherited a previous movement");
                var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat);
                var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);long start=b.level().getGameTime();Vec3 chargeHead=null;
                for(int t=0;t<=110;t++){
                    b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+t);combat.adjustPosition();
                    h.assertTrue(b.position().distanceTo(origin)<.001,"Command charging moved emperor: "+name);
                    if(name.equals("ally_d02_chongfeng")){
                        if(t==83)near.setPos(origin.add(4,0,0));
                        if(t==84)near.setPos(origin.add(-2,0,0));
                    }
                    tick.invoke(ally);
                    if(name.equals("ally_d02_chongfeng")&&t==90)chargeHead=dragon.posedPart(11,dragon.animationSeconds(0)).center;
                    if(name.equals("ally_d02_chongfeng")&&t>=83){
                        h.assertTrue(((Vec3)field(ShiHuangDragonAlly.class,"jointDirection").get(ally)).distanceTo(new Vec3(1,0,0))<.001,"D02 did not lock nearest player's direction at tick 83");
                        if(t==110){
                            h.assertTrue(Math.abs(dragon.animationSeconds(0)-1.5)<.001,"D02 charge animation did not speed up by 50 percent");
                            Vec3 travel=dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(chargeHead);
                            h.assertTrue(travel.x>5&&Math.abs(travel.z)<3,"D02 actual charge does not follow locked direction: "+travel);
                        }
                    }
                    if(name.equals("ally_d03_longxi")&&t==54){
                        var flow=icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath.flowAtPose(dragon,"ground_turn_sweep",1.2);
                        System.out.println("D03_FLOW origin="+origin+" "+flow);
                        h.assertTrue(flow.ground()!=null&&flow.spreadRadius()==15,"D03 fire failed to reach and spread on the floor");
                        double gap=flow.ground().subtract(origin).horizontalDistance()-flow.spreadRadius();
                        h.assertTrue(gap>=0&&gap<=1.5,"D03 leaves a gap beside emperor: "+gap);
                        p.setPos(origin.add(0,0,2));h.assertTrue(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath.hits(dragon,flow,p),"D03 misses a player beside emperor");
                    }
                    if(!name.equals("ally_d02_chongfeng")&&t==54)break;
                }combat.stop();b.discard();
            }
            System.out.println("DRAGON_COMMAND_AIM_PASS stationary casts, D02 tick-83 lock and 1.5x charge, D03 ground edge reaches emperor");h.succeed();
        }finally{h.getLevel().players().remove(p);h.getLevel().players().remove(near);}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void rageOnlyAcceleratesAttacks(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));var normal=boss(h,origin);var rage=boss(h,origin);rage.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
        for(String clip:new String[]{"walk_back","interval_walk","interval_back_hop","interval_side_hop_l","interval_side_hop_r","interval_flash_l","interval_point","idle"}){
            normal.play(clip);rage.play(clip);time(normal,8);time(rage,8);
            h.assertTrue(Math.abs(normal.seconds(0)-rage.seconds(0))<1e-8&&rage.actionSpeed()==1,"Rage accelerates non-attack "+clip);
            h.assertTrue(normal.pose(0).bones().equals(rage.pose(0).bones()),"Rage changed evade pose timing "+clip);
        }
        normal.play("combo_a01_duanyue");rage.play("combo_a01_duanyue");time(normal,8);time(rage,8);
        h.assertTrue(Math.abs(rage.seconds(0)-normal.seconds(0)*1.5)<1e-8,"Attack lost rage speed");
        normal.discard();rage.discard();System.out.println("RAGE_ATTACK_ONLY_PASS 8 non-attack clocks and poses, attack remains 1.5x");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ultimate_dragon")
    public static void ultimateDragonFollowsRelease(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));var b=boss(h,origin);b.setNoAi(true);h.getLevel().addFreshEntity(b);b.play("ultimate_full");
        var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat);
        var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);
        long start=b.level().getGameTime();double lowest=100,maxError=0,minBellyDown=1,minChargeY=Double.POSITIVE_INFINITY,maxChargeY=Double.NEGATIVE_INFINITY,turn=0,previousAngle=0,maxStep=0;
        Vec3[] previous=new Vec3[79];double maxSpeedAdjustedStep=0;
        for(int tick=36;tick<=192;tick++){
            b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+tick);combat.tick();
            Vec3 wanted=origin.add(dragon.sceneHeadOffset("ally_ultimate_coil",dragon.animationSeconds(0),b.yBodyRot)),actual=dragon.posedPart(11,dragon.animationSeconds(0)).center;
            maxError=Math.max(maxError,wanted.distanceTo(actual));lowest=Math.min(lowest,dragon.getY()+dragon.sceneLowestOffset()-origin.y);
            var crown=ShiHuangPose.box(b.pose(0),"head",ShiHuangCombat.BOUNDS.get("head"),ShiHuangPose.modelToEntity(b.yBodyRot),b.position()).enclosingAabb().inflate(.4,.75,.4);
            for(int part=0;part<79;part++){
                var box=dragon.posedPart(part,dragon.animationSeconds(0));h.assertTrue(!box.intersects(crown),"Dragon crosses emperor crown at tick "+tick+" part "+part);
                if(previous[part]!=null){
                    double step=box.center.distanceTo(previous[part]);maxStep=Math.max(maxStep,step);
                    maxSpeedAdjustedStep=Math.max(maxSpeedAdjustedStep,step/(tick>130&&tick<=160?3.2/1.5:1));
                }previous[part]=box.center;
            }
            h.assertTrue(dragon.position().distanceTo(origin)<.02,"Authored path origin drifted away from emperor");
            if(tick<=117){minChargeY=Math.min(minChargeY,actual.y);maxChargeY=Math.max(maxChargeY,actual.y);}
            if(tick<=125){
                double angle=Math.atan2(actual.z-origin.z,actual.x-origin.x);
                if(tick>36){double step=Math.atan2(Math.sin(angle-previousAngle),Math.cos(angle-previousAngle));h.assertTrue(step>.07,"Orbit paused or reversed before launch at tick "+tick+": "+step);turn+=step;}
                previousAngle=angle;
            }
            if(tick==128||tick==129){
                h.assertTrue(actual.subtract(origin).horizontalDistance()<.15&&actual.y-origin.y>17,"Head missed emperor's overhead position immediately before release");
                h.assertTrue(dragon.posedPart(11,dragon.animationSeconds(0)).axisZ.y<-.9,"Dragon must look upward for the roar");
                @SuppressWarnings("unchecked") var events=(java.util.Set<String>)field(ShiHuangCombat.class,"events").get(combat);
                h.assertTrue(events.contains("ultimate_roar")&&!events.contains("ultimate_hit"),"Roar must lead the attack");
            }
            if(tick>=128&&tick<=130)h.assertTrue(Math.abs(actual.y-origin.y-18)<.02,"Ascent started before the burst");
            if(tick==131)h.assertTrue(actual.y-origin.y>18.5,"Ascent did not start with the burst");
            if(tick==144)h.assertTrue(actual.y-origin.y>35,"Dragon failed to rush upward after release");
            if(tick>=160){
                h.assertTrue(Math.abs(actual.y-origin.y-108.6)<.02,"Ascent failed to finish with the burst");
                h.assertTrue(dragon.getY()+dragon.sceneLowestOffset()-origin.y>45,"Tail remains below the completed ascent when the burst ends");
            }
            if(tick<=117)for(int part:new int[]{0,1,2,3,4,13,14,15,16,17,18,19,20}){
                var box=dragon.posedPart(part,dragon.animationSeconds(0));
                minBellyDown=Math.min(minBellyDown,box.axisY.y);
            }
        }
        h.assertTrue(maxError<.02,"Authored motion drifted from emperor: "+maxError);
        h.assertTrue(lowest>=.49,"Serpent claws or jaw cross the floor: "+lowest);
        h.assertTrue(minBellyDown>.85,"Charging belly must face down: "+minBellyDown);
        h.assertTrue(maxChargeY-minChargeY>2.4,"Charge movement remains too small");
        h.assertTrue(turn>Math.PI*3,"Dragon must keep circling through the charge: "+turn);
        h.assertTrue(maxSpeedAdjustedStep<9,"Part snaps beyond the authored ascent speed: "+maxSpeedAdjustedStep);
        h.assertTrue(!dragon.loopingAnimation(),"Ultimate launch must not loop back to the charging pose");
        combat.stop();b.discard();System.out.println("DRAGON_CHOREOGRAPHY_PASS frames=157 max_head_error="+maxError+" lowest="+lowest+" charge_range="+(maxChargeY-minChargeY)+" orbit="+turn+" max_step="+maxStep);h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ultimate_dragon")
    public static void ultimateAndChargeAlignment(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"command-target");var near=player(h,"near-command-target");p.setInvulnerable(true);near.setInvulnerable(true);
        try{
            for(boolean rage:new boolean[]{false,true})for(float yaw:new float[]{0,90,180,270}){
                var b=boss(h,origin);b.setNoAi(true);h.getLevel().addFreshEntity(b);Vec3 forward=Vec3.directionFromRotation(0,yaw);p.setPos(origin.add(forward.scale(5)));b.setTarget(p);
                if(rage)b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("RAGE_START"),0L);
                b.play("ultimate_full");var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat);
                long start=b.level().getGameTime();b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+40);combat.tick();var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);
                Vec3 head=dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(origin);
                h.assertTrue(head.horizontalDistance()>13&&head.horizontalDistance()<22,"Dragon left the charging orbit yaw="+yaw);
                h.assertTrue(head.distanceTo(dragon.sceneHeadOffset("ally_ultimate_coil",dragon.animationSeconds(0),yaw))<.02,"Orbit origin does not follow emperor yaw="+yaw);
                h.assertTrue(b.position().equals(origin)&&b.hasCombatGlow(0),"Ultimate moved emperor / lacks glow");
                b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+(rage?86:129));combat.tick();@SuppressWarnings("unchecked") var events=(java.util.Set<String>)field(ShiHuangCombat.class,"events").get(combat);
                h.assertTrue(!events.contains("ultimate_hit")&&events.contains("ultimate_roar"),"Roar/release timing mismatch rage="+rage);
                h.assertTrue(dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(origin).horizontalDistance()<.15,"Head did not gather overhead before hit yaw="+yaw+" rage="+rage);
                b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+(rage?87:130));combat.tick();h.assertTrue(events.contains("ultimate_hit"),"Ultimate missed release");
                b.getEntityData().set(ShiHuangCombatRevisionTest.<Long>key("FROZEN_AT"),start+(rage?107:160));combat.tick();
                h.assertTrue(b.seconds(0)>=6&&Math.abs(dragon.posedPart(11,dragon.animationSeconds(0)).center.y-origin.y-108.6)<.02&&dragon.getY()+dragon.sceneLowestOffset()-origin.y>45,"Ascent and burst end out of sync yaw="+yaw+" rage="+rage);
                combat.stop();b.discard();
            }
            var b=boss(h,origin);b.setNoAi(true);h.getLevel().addFreshEntity(b);p.setPos(origin.add(0,0,8));near.setPos(origin.add(0,0,12));b.setTarget(p);b.play("ally_d02_chongfeng");
            var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat);
            var tick=ShiHuangDragonAlly.class.getDeclaredMethod("tickCombo");tick.setAccessible(true);
            field(ShiHuangDragonAlly.class,"jointStart").setDouble(ally,b.actionClock(0));tick.invoke(ally);
            p.setPos(origin.add(0,0,8));near.setPos(origin.add(4,0,0));field(ShiHuangDragonAlly.class,"jointStart").setDouble(ally,b.actionClock(0)-53);tick.invoke(ally);
            Vec3 direction=(Vec3)field(ShiHuangDragonAlly.class,"jointDirection").get(ally);
            var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);
            h.assertTrue(direction.distanceTo(new Vec3(1,0,0))<.001&&Math.abs(net.minecraft.util.Mth.wrapDegrees(dragon.yBodyRot-b.yBodyRot))<.001,"Pointing did not retarget nearest player / dragon yaw mismatch");
            near.setPos(origin.add(-2,0,0));field(ShiHuangDragonAlly.class,"jointStart").setDouble(ally,b.actionClock(0)-60);tick.invoke(ally);
            h.assertTrue(direction.equals(field(ShiHuangDragonAlly.class,"jointDirection").get(ally)),"Charge direction retargeted after lock");combat.stop();b.discard();
            System.out.println("ULTIMATE_COMMAND_PASS four headings normal/rage, continuous orbit, overhead roar before 6.5s release, glow, one nearest-player retarget before pointing");h.succeed();
        }finally{h.getLevel().players().remove(p);h.getLevel().players().remove(near);}
    }
}
