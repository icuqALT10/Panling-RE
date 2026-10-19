package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.network.SkillWheelPayload;
import icu.icuqalt10.panlingre.skill.SkillCastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class ShiHuangWatchTest {
    @GameTest(template="empty",timeoutTicks=40)
    public static void eatingNearestAndSingleStrike(GameTestHelper h)throws Exception {
        Vec3 origin=floor(h);var b=boss(h,origin);
        var near=player(h,"near-food",origin.add(0,0,7));var far=player(h,"far-food",origin.add(0,0,11));
        var idle=player(h,"near-idle",origin.add(0,0,2));
        try {
            eat(near);eat(far);b.play("interval_watch");b.tick();
            h.assertTrue(b.animation().equals("counter_watch_liaotian"),"Watch did not use the independent counter clip");
            h.assertTrue(!near.isUsingItem()&&far.isUsingItem(),"Did not interrupt only the nearest user");
            h.assertTrue(b.position().distanceTo(near.position())<2.1&&b.blinkFrom().distanceTo(origin)<.1,"Did not blink beside selected user");
            h.assertTrue(near.getMainHandItem().getCount()==3&&near.getFoodData().getFoodLevel()==10,"Cancelled food was consumed");
            h.assertTrue(near.getCooldowns().isOnCooldown(Items.APPLE),"Food cooldown missing");
            for(int i=0;i<99;i++)near.getCooldowns().tick();
            h.assertTrue(near.getCooldowns().isOnCooldown(Items.APPLE),"Food cooldown shorter than five seconds");
            near.getCooldowns().tick();h.assertTrue(!near.getCooldowns().isOnCooldown(Items.APPLE),"Food cooldown longer than five seconds");
            // Put an uninvolved player even closer during the swing: target must remain locked.
            idle.setPos(b.position().add(.2,0,.5));
            var c=combat(b);
            for(int tick=0;tick<=22;tick++){time(b,tick);c.tick();}
            h.assertTrue(near.getHealth()==70&&idle.getHealth()==100&&far.getHealth()==100,"Counter damage/target/one-hit invariant failed: "+near.getHealth());
            h.assertTrue(!near.hasEffect(ModEffects.freeze)&&!near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!near.hasEffect(MobEffects.DIG_SLOWDOWN),"Watch received feint debuffs");
            long deadline=field(ShiHuangEntity.class,"nextAttack").getLong(b);
            b.tick();h.assertTrue(b.animation().equals("idle")&&field(ShiHuangEntity.class,"nextAttack").getLong(b)==deadline,"Counter restarted the attack cooldown");
            h.assertTrue(ShiHuangCombat.ACTIONS.get("combo_a04_liaotian").windows().size()==3&&ShiHuangPose.duration("combo_a04_liaotian")==2.1,"Ordinary combo was changed");
            System.out.println("WATCH_FOOD_PASS nearest active user, blink, stop eating, 100 ticks CD, locked 30 damage once, independent recovery");h.succeed();
        }finally{cleanup(h,near,far,idle);b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void castCooldownReductionAndMixedSelection(GameTestHelper h)throws Exception {
        Vec3 origin=floor(h);var p=player(h,"skill-caster",origin.add(0,0,6));var food=player(h,"far-eater",origin.add(0,0,10));
        try {
            for(double reduction:new double[]{0,.2}){
                var b=boss(h,origin);p.setHealth(100);p.getPersistentData().remove("cd.panlingre:chi_tong_jian.skill_0");
                p.getAttribute(ModAttributes.COOLDOWN_REMOVE).setBaseValue(reduction);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.chi_tong_jian.get()));
                p.getData(ModAttachments.LINGQI).setCurrent(100,p);
                var id=BuiltInRegistries.ITEM.getKey(ModItems.chi_tong_jian.get());
                SkillWheelPayload.requestHandUse(p,id,0,InteractionHand.MAIN_HAND);
                h.assertTrue(SkillCastManager.isCasting(p)&&!p.isUsingItem(),"Real hand-use request failed to enter custom cast queue");
                float mana=p.getData(ModAttachments.LINGQI).getCurrent();eat(food);
                b.play("interval_watch");long before=System.currentTimeMillis();b.tick();long after=System.currentTimeMillis();
                h.assertTrue(!SkillCastManager.isCasting(p)&&food.isUsingItem(),"Mixed target selection missed closer custom cast");
                long expiry=p.getPersistentData().getLong("cd.panlingre:chi_tong_jian.skill_0");
                long expected=(long)((1-reduction)*15000);
                h.assertTrue(expiry>=before+expected&&expiry<=after+expected,"Penalty did not use reduced normal cooldown times 1.5: "+(expiry-before));
                SkillWheelPayload.requestHandUse(p,id,0,InteractionHand.MAIN_HAND);
                h.assertTrue(!SkillCastManager.isCasting(p)&&p.getData(ModAttachments.LINGQI).getCurrent()==mana,"Interrupted cast released/consumed mana or could restart during CD");
                b.discard();
            }
            System.out.println("WATCH_CAST_PASS real five-tick windup cancelled, mixed nearest selection, 15000/12000 ms CDs, no release or mana cost, retry blocked");h.succeed();
        }finally{cleanup(h,p,food);}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void onlyWatchAndValidTargets(GameTestHelper h)throws Exception {
        Vec3 origin=floor(h);var b=boss(h,origin);var p=player(h,"watch-gates",origin.add(0,0,6));
        try {
            var c=combat(b);eat(p);
            for(String clip:new String[]{"idle","interval_guard","interval_feint","combo_a04_liaotian"}){
                b.play(clip);h.assertTrue(!c.tryWatchInterrupt()&&p.isUsingItem(),"Counter triggered outside interval_watch");
            }
            b.play("interval_watch");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(!c.tryWatchInterrupt(),"Counter chose a creative player");
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertTrue(!c.tryWatchInterrupt(),"Counter chose a spectator");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.stopUsingItem();
            h.assertTrue(!c.tryWatchInterrupt(),"Counter triggered while nobody was using an item");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.startUsingItem(InteractionHand.MAIN_HAND);
            h.assertTrue(c.tryWatchInterrupt()&&!p.isUsingItem(),"Vanilla held charge was not interrupted");
            System.out.println("WATCH_GATES_PASS only watch, active use, survival targets; vanilla held charge cancelled");h.succeed();
        }finally{cleanup(h,p);b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void pointInterruptsAndFeintCountersAttacker(GameTestHelper h)throws Exception {
        Vec3 origin=floor(h);var b=boss(h,origin);var p=player(h,"point-food",origin.add(0,0,6));
        var other=player(h,"feint-other",origin.add(0,0,2));
        try{
            eat(p);b.play("interval_point");b.tick();
            h.assertTrue(b.animation().equals("counter_watch_liaotian")&&!p.isUsingItem()&&p.getCooldowns().isOnCooldown(Items.APPLE),"Point did not interrupt food");
            for(int tick=0;tick<=22;tick++){time(b,tick);combat(b).tick();}
            h.assertTrue(p.getHealth()==70&&!p.hasEffect(ModEffects.freeze)&&!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!p.hasEffect(MobEffects.DIG_SLOWDOWN),"Point damage or unwanted feint debuffs");
            for(int hitTick:new int[]{13,14,15,16,17,18}){
                b.setPos(origin);b.play("interval_feint");time(b,hitTick);
                cleanup(h,other);other=player(h,"feint-"+hitTick,origin.add(0,0,5));p.setPos(origin.add(0,0,2));
                b.hurt(b.damageSources().playerAttack(other),1);
                h.assertTrue(b.animation().equals("counter_watch_liaotian")&&b.getTarget()==other,"Feint did not counter actual attacker at "+hitTick);
                for(int tick=0;tick<=22;tick++){time(b,tick);combat(b).tick();}
                h.assertTrue(other.getHealth()==70,"Feint must hit once for 30 at "+hitTick);
                var freeze=other.getEffect(ModEffects.freeze);var slow=other.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var fatigue=other.getEffect(MobEffects.DIG_SLOWDOWN);
                if(hitTick>=14&&hitTick<=17){
                    h.assertTrue(freeze!=null&&freeze.getDuration()==20,"Feint must freeze for one second at "+hitTick);
                    h.assertTrue(slow!=null&&slow.getDuration()==60&&slow.getAmplifier()==2,"Feint must apply three seconds of Slowness III at "+hitTick);
                    h.assertTrue(fatigue!=null&&fatigue.getDuration()==60&&fatigue.getAmplifier()==1,"Feint must apply three seconds of Mining Fatigue II at "+hitTick);
                }else h.assertTrue(freeze==null&&slow==null&&fatigue==null,"Feint applied debuffs outside 14-17 ticks at "+hitTick);
                h.assertTrue(p.getHealth()==70&&!p.hasEffect(ModEffects.freeze)&&!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!p.hasEffect(MobEffects.DIG_SLOWDOWN),"Feint hit nearest bystander instead of attacker");
            }
            System.out.println("POINT_FEINT_PASS 13/18 damage only; 14/15/16/17: 30 damage, 20-tick freeze, 60-tick Slowness III and Mining Fatigue II; point/bystander unaffected");h.succeed();
        }finally{cleanup(h,p,other);b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void independentAnimationAndRageDamage(GameTestHelper h)throws Exception {
        Vec3 origin=floor(h);var b=boss(h,origin);var p=player(h,"rage-watch",origin.add(0,0,6));
        try {
            String clip="counter_watch_liaotian";
            for(double t:new double[]{0,.1,.25,.35,.45,.5,.55})
                samePose(h,ShiHuangPose.sample(clip,t,false),ShiHuangPose.sample("combo_a04_liaotian",t,false),"First stroke at "+t);
            samePose(h,ShiHuangPose.sample(clip,1.1,false),ShiHuangPose.sample("idle",0,false),"Recovery endpoint");
            b.getEntityData().set(key("RAGE_START"),0L);eat(p);b.play("interval_watch");b.tick();
            for(int tick=0;tick<=15;tick++){time(b,tick);combat(b).tick();}
            h.assertTrue(p.getHealth()==70,"Rage altered explicit 30 damage: "+p.getHealth());
            b.tick();h.assertTrue(b.animation().equals("idle"),"Enraged counter did not finish");
            System.out.println("WATCH_CLIP_PASS exact first stroke, idle endpoint, rage timing and 30 damage");h.succeed();
        }finally{cleanup(h,p);b.discard();}
    }

    private static Vec3 floor(GameTestHelper h){
        Vec3 origin=h.absoluteVec(new Vec3(0,180,0));
        for(int x=-5;x<=5;x++)for(int z=-3;z<=15;z++)h.getLevel().setBlock(BlockPos.containing(origin.add(x,-1,z)),Blocks.STONE.defaultBlockState(),3);
        return origin;
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void guardThreeHitsSweepAndReset(GameTestHelper h)throws Exception{
        Vec3 origin=floor(h);var b=boss(h,origin);var p=player(h,"guard-attacker",origin.add(0,0,2));
        var far=player(h,"guard-outside",origin.add(0,0,8.1));
        try{
            b.play("interval_guard");time(b,10);
            b.hurt(b.damageSources().playerAttack(p),1);b.hurt(b.damageSources().playerAttack(p),1);
            h.assertTrue(b.animation().equals("interval_guard"),"Guard counter before three hits");
            b.play("idle");b.play("interval_guard");time(b,10);b.hurt(b.damageSources().playerAttack(p),1);
            h.assertTrue(b.animation().equals("interval_guard"),"Guard count survived leaving guard");
            b.hurt(b.damageSources().playerAttack(p),1);b.hurt(b.damageSources().playerAttack(p),1);
            h.assertTrue(b.animation().equals("counter_guard_sweep"),"Third guard hit did not sweep");
            for(int tick=0;tick<=16;tick++){time(b,tick);combat(b).tick();}
            h.assertTrue(p.getHealth()==65&&far.getHealth()==100,"Guard sweep damage/radius/once failed");
            h.assertTrue(!combat(b).ultimateUsed(),"Small counter consumed full ultimate");
            b.tick();h.assertTrue(b.animation().equals("idle"),"Guard counter failed to recover");
            System.out.println("GUARD_COUNTER_PASS three hits, per-guard reset, 35 damage once, 8-block radius, no full ultimate");h.succeed();
        }finally{cleanup(h,p,far);b.discard();}
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void guardSweepContinuesBlockingPose(GameTestHelper h)throws Exception{
        Vec3 origin=floor(h);var b=boss(h,origin);
        try{
            samePose(h,ShiHuangPose.sample("counter_guard_sweep",0,false),ShiHuangPose.sample("interval_guard",.5,false),"Authored guard sweep start");
            samePose(h,ShiHuangPose.sample("counter_guard_sweep",.8,false),ShiHuangPose.sample("idle",0,false),"Guard sweep recovery");
            b.setNoAi(true);
            for(int tick:new int[]{0,5,10,16,20})for(float weight:new float[]{0,1}){
                b.play("interval_guard");time(b,tick);
                b.getEntityData().set(key("INTERVAL_WEIGHT"),weight);b.getEntityData().set(key("INTERVAL_GAIT"),.3F);
                b.getEntityData().set(key("INTERVAL_DIRECTION"),-1);
                var before=b.pose(0);b.play("counter_guard_sweep");
                samePose(h,b.pose(0),before,"Guard transition tick "+tick+" gait "+weight);
                time(b,2);samePose(h,b.pose(0),ShiHuangPose.sample("counter_guard_sweep",.1,false),"Guard transition release");
            }
            System.out.println("GUARD_BLEND_PASS source hold, idle recovery, current guard pose with standing/walking gait, release at 2 ticks");h.succeed();
        }finally{b.discard();}
    }

    @GameTest(template="empty",timeoutTicks=80,batch="cooldown_blink")
    public static void cooldownBlinksToDistantCombatTarget(GameTestHelper h)throws Exception{
        Vec3 origin=floor(h);
        for(boolean rage:new boolean[]{false,true})for(double distance:new double[]{6,6.01,9,40,127,129,512}){
            Vec3 destination=origin.add(0,0,distance);
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)h.getLevel().setBlock(BlockPos.containing(destination.add(x,-1,z)),Blocks.STONE.defaultBlockState(),3);
            var b=boss(h,origin);var p=player(h,"far-blink",destination);b.setTarget(p);
            if(rage)b.getEntityData().set(key("RAGE_START"),0L);
            try{
                field(ShiHuangEntity.class,"nextAttack").setLong(b,h.getLevel().getGameTime()+1);b.tick();
                h.assertTrue(b.blinkTicks(0)<0&&!ShiHuangCombat.ACTIONS.containsKey(b.animation()),"Blink/attack before cooldown finished at "+distance);
                b.play("idle");b.setPos(origin);b.setDeltaMovement(Vec3.ZERO);
                field(ShiHuangEntity.class,"nextAttack").setLong(b,h.getLevel().getGameTime());b.tick();
                h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation()),"Cooldown stayed in "+b.animation()+" at distance "+distance);
                if(distance>6){
                    h.assertTrue(b.blinkTicks(0)==0&&b.position().distanceTo(p.position())<2,"Distant target was not reached by blink at "+distance);
                    h.assertTrue(b.blinkFrom().distanceTo(new Vec3(origin.toVector3f()))<.001&&b.blinkTo().distanceTo(new Vec3(b.position().toVector3f()))<.001,"Blink endpoints were not synchronized at "+distance);
                    h.assertTrue(field(ShiHuangEntity.class,"rapidDestination").get(b)==null,"Blink became a long physical dash");
                }else h.assertTrue(b.blinkTicks(0)<0&&b.position().distanceTo(origin)<.001,"Close target unnecessarily blinked");
            }finally{cleanup(h,p);b.discard();}
        }
        System.out.println("COOLDOWN_BLINK_PASS normal/rage, 6/6.01/9/40/127/129/512 blocks, cooldown boundary, acquired target retained beyond search range, synchronized teleport");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60,batch="watch_once")
    public static void watchOnlyOnceUntilNextOffensiveAttack(GameTestHelper h)throws Exception{
        Vec3 origin=floor(h);
        for(boolean rage:new boolean[]{false,true}){
            var b=boss(h,origin);var p=player(h,"watch-once",origin.add(0,0,6));var other=player(h,"watch-other",origin.add(0,0,8));
            if(rage)b.getEntityData().set(key("RAGE_START"),0L);
            try{
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.chi_tong_jian.get()));
                p.getData(ModAttachments.LINGQI).setCurrent(100,p);
                SkillWheelPayload.requestHandUse(p,BuiltInRegistries.ITEM.getKey(ModItems.chi_tong_jian.get()),0,InteractionHand.MAIN_HAND);
                h.assertTrue(SkillCastManager.isCasting(p),"Initial real skill windup did not start");
                b.play("interval_watch");b.tick();
                h.assertTrue(b.animation().equals("counter_watch_liaotian")&&!SkillCastManager.isCasting(p),"First watch did not interrupt real skill");
                for(int tick=0;tick<=22;tick++){time(b,tick);combat(b).tick();}
                h.assertTrue(p.getHealth()==70,"First watch must strike once");
                b.tick();h.assertTrue(b.animation().equals("idle"),"Watch counter did not finish");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.startUsingItem(InteractionHand.MAIN_HAND);
                for(String clip:new String[]{"interval_point","interval_watch"}){
                    b.play(clip);
                    h.assertTrue(!combat(b).tryWatchInterrupt()&&p.isUsingItem(),"Second windup was watched again in the same attack interval: "+clip);
                    h.assertTrue(!p.getCooldowns().isOnCooldown(Items.BOW)&&p.getHealth()==70,"Repeated watch altered item cooldown or damage");
                }
                p.stopUsingItem();other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));other.startUsingItem(InteractionHand.MAIN_HAND);
                h.assertTrue(!combat(b).tryWatchInterrupt()&&other.isUsingItem(),"Changing players reset the shared watch allowance");
                b.play("counter_guard_sweep");time(b,16);b.tick();b.play("interval_point");
                h.assertTrue(!combat(b).tryWatchInterrupt()&&other.isUsingItem(),"Finishing another counter reset watch allowance");
                other.stopUsingItem();p.startUsingItem(InteractionHand.MAIN_HAND);
                b.play("combo_a01_duanyue");time(b,40);b.tick();
                h.assertTrue(b.animation().equals("idle"),"Next offensive attack did not finish");
                b.play("interval_watch");
                h.assertTrue(combat(b).tryWatchInterrupt()&&!p.isUsingItem(),"Watch did not reset for the next offensive attack interval");
            }finally{cleanup(h,p,other);b.discard();}
        }
        System.out.println("WATCH_ONCE_PASS normal/rage, real skill intercepted, repeated bow/watch/point/other player/guard counter cannot reset shared allowance, next offensive attack resets");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="counter_cooldown")
    public static void intervalCountersKeepOriginalCooldownDeadline(GameTestHelper h)throws Exception{
        record Case(ShiHuangEntity boss,ServerPlayer player,String stance,int trigger,int nextAttack){}
        Vec3 origin=floor(h);long start=h.getLevel().getGameTime();var cases=new java.util.ArrayList<Case>();
        var complete=new java.util.HashSet<ShiHuangEntity>();
        for(boolean rage:new boolean[]{false,true})for(String stance:new String[]{"interval_watch","interval_point","interval_feint","interval_guard"})for(int trigger:new int[]{10,50}){
            var b=boss(h,origin);var p=player(h,"counter-clock",origin.add(0,0,5));h.getLevel().players().remove(p);p.setInvulnerable(true);
            if(rage)b.getEntityData().set(key("RAGE_START"),0L);
            field(ShiHuangEntity.class,"nextAttack").setLong(b,start+60);
            var pressure=field(ShiHuangEntity.class,"pressure").get(b);
            @SuppressWarnings("unchecked") var used=(java.util.Set<ShiHuangPressure.Move>)field(ShiHuangPressure.class,"used").get(pressure);
            used.add(ShiHuangPressure.Move.GUARD);
            double duration=ShiHuangPose.duration(stance.equals("interval_guard")?"counter_guard_sweep":"counter_watch_liaotian");
            cases.add(new Case(b,p,stance,trigger,Math.max(60,trigger+(int)Math.ceil(duration*20/(rage?1.5:1))+1)));
        }
        h.onEachTick(()->{try{
            int elapsed=(int)(h.getLevel().getGameTime()-start);
            for(var c:cases){
                var b=c.boss();var p=c.player();if(complete.contains(b)||elapsed<c.trigger())continue;
                h.getLevel().players().add(p);
                try{
                    if(elapsed==c.trigger()){
                        b.play(c.stance());
                        if(c.stance().equals("interval_guard")){
                            time(b,10);for(int i=0;i<3;i++)b.hurt(b.damageSources().playerAttack(p),1);
                        }else if(c.stance().equals("interval_feint")){
                            time(b,14);b.hurt(b.damageSources().playerAttack(p),1);
                        }else{
                            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.startUsingItem(InteractionHand.MAIN_HAND);
                            h.assertTrue(combat(b).tryWatchInterrupt(),"Watch/point trigger failed");
                        }
                        h.assertTrue(b.animation().startsWith("counter_"),"Reaction did not start: "+c.stance());
                    }
                    b.tick();
                    h.assertTrue(field(ShiHuangEntity.class,"nextAttack").getLong(b)==start+60,"Reaction restarted cooldown: "+c.stance()+" tick="+elapsed);
                    var pressure=field(ShiHuangEntity.class,"pressure").get(b);
                    var used=(java.util.Set<?>)field(ShiHuangPressure.class,"used").get(pressure);
                    h.assertTrue(used.contains(ShiHuangPressure.Move.GUARD),"Reaction cleared cooldown gesture history");
                    boolean attacking=ShiHuangCombat.ACTIONS.containsKey(b.animation())&&!b.animation().startsWith("counter_");
                    if(!attacking&&!b.animation().startsWith("counter_")){
                        h.assertTrue(b.animation().equals("idle")||b.animation().equals("interval_walk"),"Another interval gesture started after reaction: "+b.animation());
                        h.assertTrue(used.size()==ShiHuangPressure.Move.values().length&&!((ShiHuangPressure)pressure).onHurt(p),"Reaction failed to disable remaining gestures/reactive flash");
                    }
                    if(elapsed<c.nextAttack())h.assertTrue(!attacking,"Formal attack started too early: "+c.stance());
                    else{
                        h.assertTrue(attacking,"Formal attack delayed after original cooldown/reaction ended: "+c.stance()+" tick="+elapsed+" animation="+b.animation());
                        time(b,100);b.tick();
                        h.assertTrue(b.attackCooldownTicks()==60&&used.isEmpty()&&!field(ShiHuangPressure.class,"flashUsed").getBoolean(pressure),"New offensive attack did not restore normal cooldown gestures");
                        complete.add(b);b.discard();
                    }
                }finally{cleanup(h,p);}
            }
            if(complete.size()==cases.size()){
                System.out.println("COUNTER_COOLDOWN_PASS 16 real-clock cases: watch/point/feint/guard, normal/rage, early/late; original 60-tick deadline, walking only after reaction, reactive flash blocked, next offense on schedule, new round gestures restored");h.succeed();
            }
        }catch(Exception e){for(var c:cases){cleanup(h,c.player());c.boss().discard();}throw new RuntimeException(e);}});
    }

    private static void samePose(GameTestHelper h,ShiHuangPose.Frame actual,ShiHuangPose.Frame expected,String context){
        for(var entry:expected.bones().entrySet()){
            var a=actual.bones().get(entry.getKey());var e=entry.getValue();
            h.assertTrue(a.rotation().distanceTo(e.rotation())<1e-6&&a.position().distanceTo(e.position())<1e-6&&a.scale().distanceTo(e.scale())<1e-6,
                    context+" differs on "+entry.getKey()+": "+a+" expected "+e);
        }
    }
    private static void eat(ServerPlayer p){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,3));p.getFoodData().setFoodLevel(10);p.startUsingItem(InteractionHand.MAIN_HAND);}
    private static ShiHuangEntity boss(GameTestHelper h,Vec3 origin){var b=ModEntities.SHI_HUANG.get().create(h.getLevel());b.moveTo(origin,0,0);b.yBodyRot=b.yHeadRot=0;b.setNoGravity(true);return b;}
    private static ServerPlayer player(GameTestHelper h,String name,Vec3 pos)throws Exception{
        var server=h.getLevel().getServer();var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),name),false);
        var p=new ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}};
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);field(ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);p.setData(ModAttachments.ZHIYE,new ZhiyeData(true,true,true));
        p.setPos(pos);h.getLevel().players().add(p);return p;
    }
    private static void cleanup(GameTestHelper h,ServerPlayer... players){for(var p:players){SkillCastManager.cancel(p);h.getLevel().players().remove(p);}}
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> key(String name)throws Exception{return (net.minecraft.network.syncher.EntityDataAccessor<T>)field(ShiHuangEntity.class,name).get(null);}
    private static void time(ShiHuangEntity b,int ticks)throws Exception{b.getEntityData().set(key("START"),b.level().getGameTime()-ticks);}
    private static ShiHuangCombat combat(ShiHuangEntity b)throws Exception{return (ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);}
}
