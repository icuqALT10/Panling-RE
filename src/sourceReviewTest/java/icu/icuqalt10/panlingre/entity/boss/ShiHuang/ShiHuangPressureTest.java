package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class ShiHuangPressureTest {
    @GameTest(template="empty",timeoutTicks=100)
    public static void meleeVolumesAndStationaryHits(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"sector-target");
        try {
            for(float yaw:new float[]{0,90,180,270}){
                Vec3 forward=Vec3.directionFromRotation(0,yaw),side=new Vec3(-forward.z,0,forward.x);
                for(boolean thrust:new boolean[]{false,true}){
                    h.assertTrue(ShiHuangCombat.meleeIntersects(origin,yaw,thrust,box(origin.add(forward.scale(2.9)))),"Front reach failed at yaw "+yaw);
                    h.assertTrue(!ShiHuangCombat.meleeIntersects(origin,yaw,thrust,box(origin.subtract(forward))),"Attack hit behind the emperor");
                    h.assertTrue(!ShiHuangCombat.meleeIntersects(origin,yaw,thrust,box(origin.add(forward.scale(3.5)))),"Attack exceeded three-block reach");
                    h.assertTrue(!ShiHuangCombat.meleeIntersects(origin,yaw,thrust,box(origin.add(forward).add(0,3.1,0))),"Attack hit a different floor");
                }
                AABB flank=box(origin.add(forward.scale(1.7)).add(side.scale(1.7)));
                h.assertTrue(ShiHuangCombat.meleeIntersects(origin,yaw,false,flank)&&!ShiHuangCombat.meleeIntersects(origin,yaw,true,flank),"Slash and thrust have identical widths");
                h.assertTrue(!ShiHuangCombat.meleeIntersects(origin,yaw,false,box(origin.add(side.scale(2.5)))),"Sector hit outside its 120-degree arc");
            }
            int checked=0;
            for(boolean rage:new boolean[]{false,true})for(var action:ShiHuangCombat.ACTIONS.values())if(action.name().startsWith("combo_")){
                var b=boss(h,origin);p.setPos(origin.add(0,0,2.5));p.setHealth(p.getMaxHealth());b.play(action.name());
                if(rage){
                    @SuppressWarnings("unchecked") var clock=(net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"RAGE_START").get(null);
                    b.getEntityData().set(clock,0L);h.assertTrue(b.enraged(),"Rage test was not enraged");
                }
                var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);var window=action.windows().getFirst();
                int tick=(int)Math.ceil(window.from()*20/(rage?1.5:1));
                float health=p.getHealth();time(b,tick);combat.tick();
                h.assertTrue(p.getHealth()<health,"Stationary frontal target missed by "+action.name()+" rage="+rage);
                float after=p.getHealth();combat.tick();
                h.assertTrue(p.getHealth()==after,"Same swing damaged its target twice: "+action.name());
                h.assertTrue(b.position().distanceToSqr(origin)<1e-8,"Stationary hit test relied on closing movement");b.discard();checked++;
            }
            var b=boss(h,origin);p.setPos(origin.add(0,0,2.5));p.setHealth(p.getMaxHealth());
            for(int x=-2;x<=2;x++)for(int y=0;y<=4;y++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,y,1)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            b.play("combo_a01_duanyue");var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);
            time(b,(int)Math.ceil(ShiHuangCombat.ACTIONS.get(b.animation()).windows().getFirst().from()*20));float health=p.getHealth();combat.tick();
            h.assertTrue(p.getHealth()==health,"Sector attack passed through a wall");b.discard();
            System.out.println("MELEE_SECTOR_PASS stationary attacks="+checked+", four headings, arc, thrust width, reach, height, single hit and wall blocking");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }
    private static AABB box(Vec3 feet){return new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);}
    @GameTest(template="empty",timeoutTicks=20)
    public static void skillChanceFiftyPercent(GameTestHelper h)throws Exception {
        var b=boss(h,h.absoluteVec(new Vec3(0,180,0)));b.addMana(20);
        var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);int skills=0;
        for(int seed=0;seed<4000;seed++){
            long value=seed*7919L;b.getRandom().setSeed(value);
            boolean expected=net.minecraft.util.RandomSource.create(value).nextInt(100)>=50;
            boolean skill=combat.choose(3).startsWith("skill_");
            h.assertTrue(skill==expected,"Skill chance is not exactly 50 percent");if(skill)skills++;
        }
        b.addMana(-20);for(int i=0;i<100;i++)h.assertTrue(combat.choose(3).startsWith("combo_"),"Skill bypassed its mana cost");
        b.discard();System.out.println("SKILL_CHANCE_PASS exact 50 percent threshold and mana gate; skills="+skills);h.succeed();
    }
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception {
        var f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    @SuppressWarnings("unchecked")
    private static void time(ShiHuangEntity boss,int tick)throws Exception {
        boss.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"START").get(null),boss.level().getGameTime()-tick);
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,String name)throws Exception {
        var server=h.getLevel().getServer();
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),name),false);
        var p=new net.minecraft.server.level.ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        field(net.minecraft.server.level.ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);
        h.getLevel().players().add(p);return p;
    }
    private static ShiHuangEntity boss(GameTestHelper h,Vec3 origin) {
        var b=icu.icuqalt10.panlingre.init.ModEntities.SHI_HUANG.get().create(h.getLevel());
        b.moveTo(origin,0,0);b.yBodyRot=b.yBodyRotO=b.yHeadRot=0;b.setNoGravity(true);
        b.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(1);return b;
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void thrustClosesOnAirborneMovingPlayer(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var level=h.getLevel();
        for(int x=-4;x<=4;x++)for(int z=-2;z<=16;z++)level.setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var p=player(h,"airborne-thrust");p.setInvulnerable(true);
        try {
            for(double height:new double[]{0,.25,.6,1.2}){
                var b=boss(h,origin);p.setPos(origin.add(0,height,5.5));b.play("combo_a02_guanjia");
                for(int tick=0;tick<=9;tick++){
                    if(tick>=3&&tick<8)p.setPos(p.position().add(.12,0,0));
                    time(b,tick);b.tick();
                }
                h.assertTrue(p.position().subtract(b.position()).horizontalDistance()<1.55&&Math.abs(b.getY()-origin.y)<.01,
                        "Thrust cancelled or followed target into the air: height="+height);
                b.discard();
            }
            var b=boss(h,origin);p.setPos(origin.add(0,0,20));b.play("walk");
            for(int tick=0;tick<20;tick++){time(b,tick);b.tick();}
            h.assertTrue(Math.abs(b.getZ()-origin.z-3)<.001,"Walking speed is not 3 blocks/sec");b.discard();h.succeed();
        }finally{level.players().remove(p);}
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void effectCancellationDuringDamage(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var b=boss(h,origin);
        var combat=(ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);
        int[] calls={0};var server=h.getLevel().getServer();
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"cancel-hit"),false);
        var p=new net.minecraft.server.level.ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation()){
            @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){calls[0]++;b.whenFroozen();return true;}
        };
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}};
        p.setPos(origin);h.getLevel().players().add(p);
        try {
            b.setTarget(p);b.play("skill_s03_fengjiang");
            var release=ShiHuangCombat.class.getDeclaredMethod("releaseSkill");release.setAccessible(true);release.invoke(combat);
            var effects=(java.util.List<?>)field(ShiHuangCombat.class,"effects").get(combat);
            var e=effects.getFirst();var from=e.getClass().getDeclaredMethod("from");from.setAccessible(true);var to=e.getClass().getDeclaredMethod("to");to.setAccessible(true);
            p.setPos(((Vec3)from.invoke(e)).lerp((Vec3)to.invoke(e),.5));
            var tick=ShiHuangCombat.class.getDeclaredMethod("tickEffects",int.class);tick.setAccessible(true);tick.invoke(combat,0);
            h.assertTrue(calls[0]==1&&effects.isEmpty(),"Cancelled action kept damaging players");
            System.out.println("PRESSURE_CANCEL_PASS damage callback cleared active effects without crash");h.succeed();
        }finally{h.getLevel().players().remove(p);b.discard();}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void intervalSelectionAndDeadline(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var b=boss(h,origin);var p=player(h,"pressure-target");
        try {
            var pressure=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(b);
            java.util.Set<ShiHuangPressure.Move> seen=new java.util.HashSet<>();
            for(int i=0;i<500;i++)seen.add(pressure.choose(2.7,.1,.1));
            h.assertTrue(seen.size()==7&&!seen.contains(ShiHuangPressure.Move.FLASH),"Only the seven proactive behaviors belong in the interval pool: "+seen);
            for(int i=0;i<500;i++){
                h.assertTrue(pressure.choose(2.7,0,0)!=ShiHuangPressure.Move.BACK_HOP,"Stationary player triggered a back flip");
                h.assertTrue(pressure.choose(3,.1,.1)!=ShiHuangPressure.Move.BACK_HOP,"Back flip allowed at 3 blocks");
                var far=pressure.choose(5,.1,.1);
                h.assertTrue(far!=ShiHuangPressure.Move.BACK_HOP&&far!=ShiHuangPressure.Move.SIDE_HOP,"Flip allowed at 5 blocks");
            }
            int calm=0,lateral=0;
            for(int i=0;i<2000;i++){
                if(pressure.choose(4,0,0)==ShiHuangPressure.Move.SIDE_HOP)calm++;
                if(pressure.choose(4,0,.1)==ShiHuangPressure.Move.SIDE_HOP)lateral++;
            }
            h.assertTrue(lateral>calm*1.5,"Lateral movement did not increase side-flip weight");
            for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            p.setPos(origin.add(0,0,3));
            java.util.ArrayDeque<String> recent=new java.util.ArrayDeque<>();
            for(int i=0;i<20;i++){
                b.play("idle");pressure.beginCooldown();pressure.tick(p);time(b,7);pressure.tick(p);String chosen=b.animation();
                String behavior=chosen.replaceAll("_[lr]$","");h.assertTrue(!recent.contains(behavior),"Repeated a recent interval behavior");
                recent.addLast(behavior);if(recent.size()>2)recent.removeFirst();
            }
            b.play("idle");field(ShiHuangEntity.class,"nextAttack").setLong(b,b.level().getGameTime());
            b.tick();h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation()),"Interval delayed the attack deadline");
            for(var name:ShiHuangPose.animationNames())if(name.startsWith("interval_")){
                var start=ShiHuangPose.sample(name,0,false);var end=ShiHuangPose.sample(name,ShiHuangPose.duration(name),false);
                for(var bone:start.matrices().keySet()){
                    var a=start.matrices().get(bone).get(new float[16]);var z=end.matrices().get(bone).get(new float[16]);
                    for(int i=0;i<16;i++)h.assertTrue(Math.abs(a[i]-z[i])<.001,"Interval does not return to rest: "+name+" "+bone);
                }
            }
            System.out.println("PRESSURE_BEHAVIOR_PASS 7 proactive behaviors; moving-player and distance gates; lateral weighting; no repeat; pose endpoints");h.succeed();
        }finally{h.getLevel().players().remove(p);b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void decisiveEvadesAndWallRejection(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var b=boss(h,origin);var p=player(h,"evade-target");
        for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        p.setPos(origin.add(0,0,2.7));p.xo=p.getX()-.1;p.yo=p.getY();p.zo=p.getZ();
        var pressure=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(b);
        var begin=ShiHuangPressure.class.getDeclaredMethod("beginGesture",net.minecraft.world.entity.LivingEntity.class);begin.setAccessible(true);
        try {
            for(var move:new ShiHuangPressure.Move[]{ShiHuangPressure.Move.BACK_HOP,ShiHuangPressure.Move.SIDE_HOP,ShiHuangPressure.Move.FLASH})for(int side:new int[]{-1,1}){
                b.play("idle");b.moveTo(origin,0,0);field(ShiHuangPressure.class,"current").set(pressure,move);field(ShiHuangPressure.class,"side").setInt(pressure,side);begin.invoke(pressure,p);
                double peak=0;
                for(int tick=0;tick<=(move==ShiHuangPressure.Move.SIDE_HOP?15:17);tick++){
                    time(b,tick);pressure.tick(p);peak=Math.max(peak,b.getY()-origin.y);
                    if(pressure.airborne()){Vec3 before=b.position();b.travel(Vec3.ZERO);h.assertTrue(b.position().distanceToSqr(before)<1e-8,"Gravity competed with the authored jump");}
                    if(move!=ShiHuangPressure.Move.FLASH&&tick==(move==ShiHuangPressure.Move.SIDE_HOP?6:8)){
                        var pose=b.pose(0);Vec3 head=b.bonePoint(pose,"head",ShiHuangCombat.PIVOTS.get("head"));Vec3 hip=b.bonePoint(pose,"pelvis",ShiHuangCombat.PIVOTS.get("pelvis"));
                        h.assertTrue(move==ShiHuangPressure.Move.BACK_HOP?head.y<hip.y-.3:head.y>hip.y+.6,"Back flip must rotate fully; lateral dodge must stay upright");
                        h.assertTrue(head.y>origin.y+.2,"Flip drives the head into the floor");
                    }
                    if(move==ShiHuangPressure.Move.FLASH&&tick==6)break;
                }
                if(move==ShiHuangPressure.Move.BACK_HOP)h.assertTrue(Math.abs(b.getZ()-origin.z+3.3)<.001&&Math.abs(b.getX()-origin.x)<.001&&peak>1.4&&peak<=1.5,"Back flip must travel 3.3 blocks without increasing height");
                if(move==ShiHuangPressure.Move.SIDE_HOP)h.assertTrue(Math.abs((b.getX()-origin.x)*-side-3.3)<.001&&Math.abs(b.getZ()-origin.z+.75)<.001&&peak>.30&&peak<=.32,"Side flip must scale both horizontal components by 1.5");
                if(move==ShiHuangPressure.Move.FLASH)h.assertTrue(Math.abs((b.getX()-origin.x)*-side-3.75)<.001&&Math.abs(b.getZ()-origin.z-.6)<.001&&b.blinkTicks(0)>=0,"Flash must scale both horizontal components by 1.5 with its afterimage");
                h.assertTrue(Math.abs(b.getY()-origin.y)<.001,"Evade did not return to the floor");
                if(move==ShiHuangPressure.Move.SIDE_HOP){
                    h.assertTrue(pressure.busy(),"Side dodge ended before its recovery pose");
                    time(b,16);pressure.tick(p);h.assertTrue(!pressure.busy(),"Faster side dodge did not finish within 16 ticks");
                }
            }
            b.play("idle");b.moveTo(origin,0,0);p.setPos(origin.add(0,0,2.7));
            for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,y,-1)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            field(ShiHuangPressure.class,"current").set(pressure,ShiHuangPressure.Move.BACK_HOP);begin.invoke(pressure,p);
            h.assertTrue(b.animation().equals("interval_guard"),"Back hop ignored a solid wall");
            System.out.println("CRISP_PRESSURE_PASS mirrored jump arcs, real flash, wall rejection");h.succeed();
        }finally{h.getLevel().players().remove(p);b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void reactiveFlashOncePerCooldown(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"reactive-flash");
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        p.setPos(origin.add(0,0,2.7));p.xo=p.getX()-.1;p.yo=p.getY();p.zo=p.getZ();
        var b=boss(h,origin);var pressure=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(b);
        try {
            int fired=0;
            for(int seed=0;seed<1000;seed++){
                b.play("idle");b.moveTo(origin,0,0);pressure.beginCooldown();b.getRandom().setSeed(seed*7919L);
                pressure.onHurt(p);
                boolean expected=net.minecraft.util.RandomSource.create(seed*7919L).nextFloat()<.1F;
                h.assertTrue(b.animation().startsWith("interval_flash_")==expected,"Flash roll is not exactly below 10 percent");
                if(expected)fired++;
            }
            h.assertTrue(fired>0&&fired<1000,"Random test did not cover both outcomes");
            ShiHuangEntity hitBoss=null;
            for(int seed=0;seed<200&&hitBoss==null;seed++){
                var candidate=boss(h,origin);candidate.getRandom().setSeed(seed*7919L);
                float health=candidate.getHealth();boolean hit=candidate.hurt(candidate.damageSources().playerAttack(p),5);
                if(candidate.animation().startsWith("interval_flash_")){
                    h.assertTrue(!hit&&candidate.getHealth()==health&&candidate.hurtTime==0,"Flash must intercept damage before health, hurt animation or death processing");
                    hitBoss=candidate;
                }else {h.assertTrue(hit&&candidate.getHealth()<health,"Unsuccessful dodge roll incorrectly cancelled damage");candidate.discard();}
            }
            h.assertTrue(hitBoss!=null,"Real damage never triggered a cooldown flash");
            var reacting=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(hitBoss);
            float health=hitBoss.getHealth();
            h.assertTrue(hitBoss.hurt(hitBoss.damageSources().playerAttack(p),5)&&hitBoss.getHealth()<health,"Flash created lasting immunity beyond its triggering hit");
            String clip=hitBoss.animation();time(hitBoss,4);reacting.tick(p);Vec3 destination=hitBoss.position();
            for(int i=0;i<200;i++)reacting.onHurt(p);
            h.assertTrue(hitBoss.animation().equals(clip)&&Math.abs(hitBoss.seconds(0)-.2)<1e-6&&hitBoss.position().equals(destination),"Repeated hit restarted the flash in one cooldown");
            time(hitBoss,17);reacting.tick(p);
            h.assertTrue(!reacting.busy(),"Reactive flash queued another gesture or evade");
            for(int i=0;i<200;i++)reacting.onHurt(p);
            h.assertTrue(hitBoss.animation().equals("interval_walk"),"Flash allowance reset before the next attack");
            hitBoss.play("combo_a01_duanyue");time(hitBoss,32);hitBoss.tick();
            h.assertTrue(!field(ShiHuangPressure.class,"flashUsed").getBoolean(reacting),"Completed attack did not reset the next cooldown's flash allowance");
            hitBoss.discard();

            // Attack, frozen and NoAI states take damage without starting a reactive dodge.
            for(String mode:new String[]{"attack","frozen","noai"})for(int seed=0;seed<30;seed++){
                var candidate=boss(h,origin);candidate.getRandom().setSeed(seed*7919L);
                if(mode.equals("attack"))candidate.play("combo_a01_duanyue");
                if(mode.equals("frozen"))candidate.whenFroozen();
                if(mode.equals("noai"))candidate.setNoAi(true);
                candidate.hurt(candidate.damageSources().playerAttack(p),5);
                h.assertTrue(!candidate.animation().startsWith("interval_flash_"),"Flash interrupted "+mode);candidate.discard();
            }

            b.play("idle");b.moveTo(origin,0,0);pressure.beginCooldown();
            field(ShiHuangPressure.class,"current").set(pressure,ShiHuangPressure.Move.BACK_HOP);
            var begin=ShiHuangPressure.class.getDeclaredMethod("beginGesture",net.minecraft.world.entity.LivingEntity.class);begin.setAccessible(true);begin.invoke(pressure,p);
            time(b,8);pressure.tick(p);Vec3 airborne=b.position();
            for(int i=0;i<200;i++)pressure.onHurt(p);
            h.assertTrue(b.animation().equals("interval_back_hop")&&b.position().equals(airborne)&&field(ShiHuangPressure.class,"flashPending").getBoolean(pressure),"Midair hit snapped or failed to queue a single flash");
            time(b,14);pressure.tick(p);time(b,20);pressure.tick(p);
            h.assertTrue(b.animation().startsWith("interval_flash_")&&Math.abs(b.getY()-origin.y)<.001,"Queued flash did not wait until landing");
            @SuppressWarnings("unchecked") var recent=(java.util.ArrayDeque<ShiHuangPressure.Move>)field(ShiHuangPressure.class,"recent").get(pressure);
            for(var last:new ShiHuangPressure.Move[]{ShiHuangPressure.Move.BACK_HOP,ShiHuangPressure.Move.SIDE_HOP,ShiHuangPressure.Move.FLASH}){
                recent.clear();recent.add(last);
                for(int i=0;i<100;i++){
                    var next=pressure.choose(2.7,.1,.1);
                    h.assertTrue(next!=ShiHuangPressure.Move.BACK_HOP&&next!=ShiHuangPressure.Move.SIDE_HOP,"Consecutive interval evades are allowed");
                }
            }
            System.out.println("REACTIVE_FLASH_PASS exact 10 percent rolls, triggering damage intercepted, following hit retained, once per cooldown, attack reset, state exclusions, deferred midair flash, no consecutive proactive evades");h.succeed();
        }finally{h.getLevel().players().remove(p);b.discard();}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void retreatAndGestureShareFullCooldown(GameTestHelper h)throws Exception {
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"retreat-target");
        for(int x=-6;x<=6;x++)for(int z=-8;z<=16;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(origin.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var cooldown=ShiHuangEntity.class.getDeclaredMethod("beginAttackCooldown");cooldown.setAccessible(true);
        try {
            // Exercise the actual entity controller for all 60 ticks, including rage speed.
            for(boolean rage:new boolean[]{false,true})for(int side:new int[]{-1,1}){
                var b=boss(h,origin);p.setPos(origin.add(0,0,8));p.xo=p.getX();p.yo=p.getY();p.zo=p.getZ();
                if(rage){
                    @SuppressWarnings("unchecked") var clock=(net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"RAGE_START").get(null);
                    b.getEntityData().set(clock,0L);h.assertTrue(b.enraged(),"Rage test was not enraged");
                }
                cooldown.invoke(b);field(ShiHuangEntity.class,"retreatSide").setInt(b,side);
                int age=0,gesturingSteps=0;String gesture=null;
                for(int tick=0;tick<60;tick++){
                    field(ShiHuangEntity.class,"nextAttack").setLong(b,b.level().getGameTime()+60-tick);
                    time(b,age);String before=b.animation();Vec3 from=b.position();b.tick();
                    h.assertTrue(!ShiHuangCombat.ACTIONS.containsKey(b.animation()),"Cooldown attacked before 60 ticks: "+tick);
                    if(!before.equals(b.animation()))age=0;else age++;
                    if(!b.animation().equals("interval_walk")){
                        gesture=b.animation();
                        if(b.position().distanceToSqr(from)>1e-8)gesturingSteps++;
                        var pose=b.pose(0);var raw=ShiHuangPose.sample(gesture,b.seconds(0),false);
                        h.assertTrue(pose.bones().get("arm_r_upper").equals(raw.bones().get("arm_r_upper")),"Gait overwrote the sword-arm gesture");
                    }
                }
                h.assertTrue(gesture!=null&&gesturingSteps>=12,"Sword gesture stopped the retreat gait");
                h.assertTrue((b.getX()-origin.x)*side<-.8&&b.getZ()<origin.z-.8,"Cooldown did not retreat diagonally: "+b.position().subtract(origin));
                p.setPos(b.position().add(0,0,5));field(ShiHuangEntity.class,"nextAttack").setLong(b,b.level().getGameTime());b.tick();
                h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation()),"60-tick cooldown did not release the next attack");b.discard();
            }
            for(double distance:new double[]{11.9,12,12.4}){
                var b=boss(h,origin);p.setPos(origin.add(0,0,distance));b.walkDuringCooldown(p,true);
                h.assertTrue(distance<=12?b.getZ()<origin.z:b.getZ()>origin.z,"Wrong 12-block retreat boundary: "+distance);b.discard();
            }
            // A gesture switch must not reset the independently advancing leg phase.
            var b=boss(h,origin);p.setPos(origin.add(0,0,8));b.play("interval_point");
            for(int tick=0;tick<12;tick++)b.walkDuringCooldown(p,true);
            var point=b.pose(0);
            h.assertTrue(Math.abs(point.bones().get("pelvis").rotation().y)<Math.toRadians(16),"Retreat still twists the pelvis by 45 degrees");
            @SuppressWarnings("unchecked") var gaitKey=(net.minecraft.network.syncher.EntityDataAccessor<Float>)field(ShiHuangEntity.class,"INTERVAL_GAIT").get(null);
            @SuppressWarnings("unchecked") var dirKey=(net.minecraft.network.syncher.EntityDataAccessor<Integer>)field(ShiHuangEntity.class,"INTERVAL_DIRECTION").get(null);
            var old=new java.util.LinkedHashMap<>(ShiHuangPose.sample("walk_back",b.getEntityData().get(gaitKey),true).bones());
            var pelvis=old.get("pelvis");old.put("pelvis",new ShiHuangPose.BonePose(pelvis.rotation().add(0,b.getEntityData().get(dirKey)*Math.PI/4,0),pelvis.position(),pelvis.scale()));
            var oldFrame=ShiHuangPose.rebuild(old);
            for(String foot:new String[]{"foot_r","foot_l"})
                h.assertTrue(b.bonePoint(point,foot,ShiHuangCombat.PIVOTS.get(foot)).distanceTo(b.bonePoint(oldFrame,foot,ShiHuangCombat.PIVOTS.get(foot)))<.001,"Reduced hip twist changed the supporting foot path: "+foot+" actual="+b.bonePoint(point,foot,ShiHuangCombat.PIVOTS.get(foot)).subtract(b.position())+" expected="+b.bonePoint(oldFrame,foot,ShiHuangCombat.PIVOTS.get(foot)).subtract(b.position())+" upper="+point.bones().get(foot.equals("foot_r")?"leg_r_upper":"leg_l_upper")+" old="+oldFrame.bones().get(foot.equals("foot_r")?"leg_r_upper":"leg_l_upper"));
            b.play("interval_guard");var guard=b.pose(0);
            for(String bone:new String[]{"pelvis","leg_r_upper","leg_l_upper","foot_r","foot_l"})
                h.assertTrue(point.bones().get(bone).equals(guard.bones().get(bone)),"Gesture restarted the gait: "+bone);
            b.discard();
            System.out.println("RETREAT_COOLDOWN_PASS 12-block boundary, both diagonal directions, concurrent sword gestures, continuous leg phase, 60 ticks normal and rage");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }
}
