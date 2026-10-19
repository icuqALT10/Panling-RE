package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class ShiHuangBalanceTest {
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> key(String name)throws Exception{return (net.minecraft.network.syncher.EntityDataAccessor<T>)field(ShiHuangEntity.class,name).get(null);}
    private static void time(ShiHuangEntity b,int ticks)throws Exception{b.getEntityData().set(ShiHuangBalanceTest.<Long>key("START"),b.level().getGameTime()-ticks);}
    private static ShiHuangEntity boss(GameTestHelper h,Vec3 p){var b=icu.icuqalt10.panlingre.init.ModEntities.SHI_HUANG.get().create(h.getLevel());b.moveTo(p,0,0);b.yBodyRot=b.yHeadRot=0;b.setNoGravity(true);return b;}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,String name)throws Exception{
        var method=ShiHuangPressureTest.class.getDeclaredMethod("player",GameTestHelper.class,String.class);method.setAccessible(true);
        var p=(net.minecraft.server.level.ServerPlayer)method.invoke(null,h,name);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);return p;
    }
    private static ShiHuangCombat combat(ShiHuangEntity b)throws Exception{return (ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);}
    private static void playerTicks(net.minecraft.server.level.ServerPlayer p,int ticks){p.setNoGravity(true);for(int i=0;i<ticks;i++){p.setDeltaMovement(Vec3.ZERO);p.tick();p.doTick();}}
    private static void floor(GameTestHelper h,Vec3 p){for(int x=-12;x<=12;x++)for(int z=-12;z<=45;z++)h.getLevel().setBlock(net.minecraft.core.BlockPos.containing(p.add(x,-1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);}

    @GameTest(template="empty",timeoutTicks=100,batch="balance_damage")
    public static void damageAndWaterFatigue(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"balance-damage");p.setPos(origin.add(0,0,2.5));int checked=0;
        try{
            for(boolean rage:new boolean[]{false,true})for(var action:ShiHuangCombat.ACTIONS.values())if(action.name().startsWith("combo_")){
                var b=boss(h,origin);b.play(action.name());if(rage)b.getEntityData().set(ShiHuangBalanceTest.<Long>key("RAGE_START"),0L);
                p.invulnerableTime=0;p.setHealth(1000);time(b,(int)Math.ceil(action.windows().getFirst().from()*20/(rage?1.5:1)));combat(b).tick();
                h.assertTrue(Math.abs((1000-p.getHealth())-16*2/3.0)<.001,"Wrong normal damage: "+action.name()+" rage="+rage+" health="+p.getHealth());b.discard();checked++;
            }
            var b=boss(h,origin);b.setTarget(p);b.play("skill_s06_fanzhao");time(b,17);p.removeAllEffects();float hp=b.getHealth();
            h.assertTrue(!b.hurt(b.damageSources().playerAttack(p),10)&&b.getHealth()==hp&&b.animation().equals("counter_s06_fanzhao"),"Water parry did not block and counter");
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN),"Parried attacker lacks mining fatigue");
            p.removeAllEffects();p.setHealth(1000);p.invulnerableTime=0;time(b,3);combat(b).tick();
            h.assertTrue(p.getHealth()==930&&p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN),"Counter slash must deal 70 and apply fatigue");
            playerTicks(p,20);combat(b).stop();b.play("idle");b.play("counter_s06_fanzhao");p.setPos(origin.add(0,0,6));p.setHealth(1000);p.removeAllEffects();
            for(int tick=0;tick<=15;tick++){time(b,tick);combat(b).tick();}
            h.assertTrue(p.getHealth()==950&&p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN),"Water column must deal 50 and apply fatigue");
            playerTicks(p,20);combat(b).stop();b.play("idle");b.play("ultimate_full");p.setHealth(1000);time(b,130);combat(b).tick();
            h.assertTrue(p.getHealth()==900,"Ultimate damage must be 100");combat(b).stop();b.discard();
            System.out.println("BALANCE_DAMAGE_PASS normal/rage combos="+checked+", normal 10.6667, counter 70, column 50, ultimate 100, water fatigue");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="cooldown_sequence")
    public static void cooldownFitsUniqueGestures(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));floor(h,origin);var p=player(h,"cooldown-sequence");p.setInvulnerable(true);int total=0;
        try{
            for(boolean rage:new boolean[]{false,true})for(int seed=0;seed<30;seed++){
                var b=boss(h,origin);if(rage)b.getEntityData().set(ShiHuangBalanceTest.<Long>key("RAGE_START"),0L);b.getRandom().setSeed(seed);
                var pressure=(ShiHuangPressure)field(ShiHuangEntity.class,"pressure").get(b);pressure.beginCooldown();int clipStart=0;String last="idle";var used=new java.util.HashSet<String>();
                for(int tick=0;tick<60;tick++){
                    field(ShiHuangEntity.class,"nextAttack").setLong(b,b.level().getGameTime()+60-tick);time(b,tick-clipStart);
                    p.setPos(b.position().add(0,0,2.7));p.xo=p.getX()-.1;p.zo=p.getZ();pressure.tick(p);
                    String clip=b.animation();if(!clip.equals(last)){
                        clipStart=tick;last=clip;
                        if(!clip.equals("interval_walk")){
                            h.assertTrue(used.add(clip.replaceAll("_[lr]$","")),"Repeated cooldown gesture: "+clip);
                            double seconds=ShiHuangPose.duration(clip)/(clip.startsWith("interval_side_hop")?1.25:1);
                            h.assertTrue(tick+Math.ceil(seconds*20)<60,"Gesture cannot finish before cooldown: "+clip);total++;
                        }
                    }
                }
                h.assertTrue(used.size()>=2&&!pressure.busy(),"Cooldown failed to fit multiple complete gestures: "+used);
                field(ShiHuangEntity.class,"nextAttack").setLong(b,b.level().getGameTime());b.tick();h.assertTrue(ShiHuangCombat.ACTIONS.containsKey(b.animation()),"Gestures delayed next attack");b.discard();
            }
            System.out.println("COOLDOWN_SEQUENCE_PASS 60 normal/rage cooldowns, gestures="+total+", no repeats, no deadline extension");h.succeed();
        }finally{h.getLevel().players().remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100,batch="skill_release_aim")
    public static void skillsRetargetBeforeRelease(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"aim-old-target");var near=player(h,"aim-nearest");p.setInvulnerable(true);near.setInvulnerable(true);int checks=0;
        try{
            for(boolean rage:new boolean[]{false,true})for(var action:ShiHuangCombat.ACTIONS.values())if(action.name().startsWith("skill_")||action.name().startsWith("counter_")){
                int count=Math.max(1,action.windows().size());
                for(int i=0;i<count;i++){
                    var b=boss(h,origin);p.setPos(origin.add(0,0,8));near.setPos(origin.add(4,0,0));b.setTarget(p);b.play(action.name());if(rage)b.getEntityData().set(ShiHuangBalanceTest.<Long>key("RAGE_START"),0L);
                    double at=(action.name().equals("skill_s02_jianzhen")||action.name().equals("skill_s06_fanzhao"))?.7:Math.max(0,action.windows().get(i).from()-.15);
                    time(b,(int)Math.ceil(at*20/(rage?1.5:1)));combat(b).tick();
                    h.assertTrue(b.getTarget()==near&&b.getLookAngle().distanceTo(new Vec3(1,0,0))<.001,"Skill did not face nearest player before release: "+action.name()+" #"+i+" rage="+rage);
                    h.assertTrue(b.position().equals(origin),"Ranged release aim moved emperor");combat(b).stop();b.discard();checks++;
                }
            }
            System.out.println("SKILL_RELEASE_AIM_PASS checks="+checks+", every regular skill stroke and counter, nearest target, normal/rage");h.succeed();
        }finally{h.getLevel().players().remove(p);h.getLevel().players().remove(near);}
    }

    @GameTest(template="empty",timeoutTicks=80,batch="charge_repeat_damage")
    public static void dragonChargeRespectsInvulnerabilityFrames(GameTestHelper h)throws Exception{
        var origin=h.absoluteVec(new Vec3(0,240,0));var p=player(h,"charge-contact");p.setPos(origin.add(0,0,8));var b=boss(h,origin);h.getLevel().addFreshEntity(b);b.setTarget(p);b.play("ally_d02_chongfeng");
        var ally=(ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat(b));var dragon=(icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally);
        var tick=ShiHuangDragonAlly.class.getDeclaredMethod("tickCombo");tick.setAccessible(true);var hit=ShiHuangDragonAlly.class.getDeclaredMethod("tickJoint",long.class);hit.setAccessible(true);
        try{
            p.setInvulnerable(true);long start=b.level().getGameTime();for(int t=0;t<=100;t++){b.getEntityData().set(ShiHuangBalanceTest.<Long>key("FROZEN_AT"),start+t);tick.invoke(ally);}
            var contact=dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(0,.9,0);p.setPos(contact);p.setHealth(1000);p.setInvulnerable(false);
            hit.invoke(ally,70L);h.assertTrue(p.getHealth()==930,"Charge contact damage must be 70: health="+p.getHealth()+" stopped="+field(ShiHuangDragonAlly.class,"chargeStopped").get(ally)+" clip="+dragon.animation());
            hit.invoke(ally,71L);h.assertTrue(p.getHealth()==930&&p.invulnerableTime>10,"Charge bypassed vanilla hurt cooldown");
            playerTicks(p,10);p.setPos(contact);float before=p.getHealth();hit.invoke(ally,80L);h.assertTrue(before-p.getHealth()==70,"Charge cannot damage again after hurt cooldown expires: before="+before+" after="+p.getHealth()+" invulnerability="+p.invulnerableTime);
            System.out.println("CHARGE_REPEAT_PASS damage 70, repeated contact blocked by vanilla frames, subsequent hit allowed");h.succeed();
        }finally{combat(b).stop();b.discard();h.getLevel().players().remove(p);}
    }
}
