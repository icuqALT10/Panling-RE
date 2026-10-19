package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonRulesTest {
    @GameTest(template="empty",timeoutTicks=40)
    public static void phaseOverridesFreezeNoAiRecoveryAndForm(GameTestHelper h)throws Exception {
        for(boolean air:new boolean[]{false,true}){
            var d=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());d.setPos(h.absoluteVec(new Vec3(0,180,0)));
            if(air)d.beginPhaseEntry("idle_air",80,d.position());else d.beginCombatAnimation("idle_ground");
            var c=combat(d);field(GraveDragonCombat.class,"recoveryUntil").setLong(c,Long.MAX_VALUE);
            field(GraveDragonCombat.class,"queuedAction").set(c,"ground_double_bite");
            h.assertTrue(d.addEffect(new MobEffectInstance(ModEffects.freeze,200)),"Freeze setup failed");
            d.setHealth(d.getMaxHealth()*.5F);d.setNoAi(true);d.tick();
            h.assertTrue(d.animation().startsWith("phase50_enter_")&&!d.isNoAi()&&!d.hasEffect(ModEffects.freeze),"Threshold failed to preempt freeze/NoAI/recovery: "+d.animation());
            field(GraveDragonCombat.class,"phaseStarted").setLong(c,h.getLevel().getGameTime()-10000);
            d.tick();h.assertTrue(d.animation().equals("phase50_firefield"),"No-target phase failed to start breath");
            h.assertTrue(!d.addEffect(new MobEffectInstance(ModEffects.freeze,200)),"Freeze interrupted forced breath");
            d.setNoAi(true);d.tick();h.assertTrue(!d.isNoAi()&&d.animation().equals("phase50_firefield"),"NoAI stalled forced breath");
            var saved=new net.minecraft.nbt.CompoundTag();c.save(saved);
            var loaded=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());var lc=combat(loaded);lc.load(saved);
            h.assertTrue(lc.forcingPhase()&&!lc.phaseDone(),"Unload skipped unfinished forced breath");loaded.discard();
            for(int tick=1;tick<=Math.ceil(GraveDragonPose.duration("phase50_firefield")*20)+1;tick++){
                time(d,tick);c.tick();if(!d.animation().equals("phase50_firefield"))break;
            }
            h.assertTrue(c.phaseDone()&&!d.isInvulnerable(),"Forced phase did not finish/release invulnerability");
            d.kill();h.assertTrue(d.isDeadOrDying()||d.isRemoved(),"Force phase prevented explicit kill");
        }
        System.out.println("DRAGON_FORCE_PASS ground/air, freeze, NoAI, cooldown, queued attack, no target, reload, completion and kill");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void roomBoundaryReplacesInternalBlockCollision(GameTestHelper h)throws Exception {
        var controller=controller(h);var scene=controller.scene();
        var d=new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(),h.getLevel()){@Override ShiHuangController tombController(){return controller;}};
        d.setPos(scene.dragonSpawn());d.beginCombatAnimation("idle_ground");
        Vec3 next=d.position().add(3,0,0);
        for(int y=0;y<12;y++)h.getLevel().setBlock(BlockPos.containing(next.add(0,y,0)),Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(d.combatPoseFitsAt("ground_coil",1,0,0,next,next.y),"Interior pillar vetoed a pose");
        d.move(MoverType.SELF,new Vec3(3,0,0));h.assertTrue(d.position().distanceTo(next)<.001,"Interior pillar blocked movement");
        h.assertTrue(d.moveSceneTo(next.add(2,4,0),90,next.y),"Interior obstacle blocked scene movement");
        var bounds=d.roomMovementBounds();
        d.move(MoverType.SELF,new Vec3(1000,-1000,1000));
        h.assertTrue(bounds.contains(d.position()),"Physics escaped room boundary: "+d.position());
        d.setPos(bounds.minX-500,bounds.maxY+500,bounds.minZ-500);
        h.assertTrue(bounds.contains(d.position()),"Scripted movement escaped room boundary");
        d.setPos(next);d.refreshCombatPose();
        h.assertTrue(d.getWorldParts()[11].getOrientedBox().halfExtents.lengthSqr()>0,"Combat part OBBs were removed");
        d.discard();System.out.println("DRAGON_BOUNDARY_PASS interior blocks ignored; physics/scripted root clamped; combat OBB retained");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void platformTimeoutAndSpectatorExclusion(GameTestHelper h)throws Exception {
        var c=controller(h);var scene=c.scene();field(ShiHuangController.class,"stage").set(c,ShiHuangController.Stage.DRAGON);
        var tick=ShiHuangController.class.getDeclaredMethod("tickPlatformLimit",net.minecraft.server.level.ServerLevel.class);tick.setAccessible(true);
        @SuppressWarnings("unchecked") var since=(Map<UUID,?>)field(ShiHuangController.class,"platformTime").get(c);
        Vec3 platform=scene.dragonSpawn().add(15,2,0),center=scene.dragonRoom().getCenter();
        h.getLevel().setBlock(BlockPos.containing(platform.add(0,-1,0)),Blocks.STONE.defaultBlockState(),3);
        var p=player(h,"Platform",platform);var observer=player(h,"Observer",platform.add(0,1,0));observer.setGameMode(GameType.SPECTATOR);
        try{
            tick.invoke(c,h.getLevel());h.assertTrue(since.containsKey(p.getUUID())&&!since.containsKey(observer.getUUID()),"Platform eligibility wrong");
            for(int i=1;i<30;i++)tick.invoke(c,h.getLevel());
            p.setPos(scene.dragonSpawn());for(int i=0;i<99;i++)tick.invoke(c,h.getLevel());
            h.assertTrue(since.containsKey(p.getUUID()),"Short visit below cleared platform accumulation");
            p.setPos(platform);for(int i=0;i<29;i++)tick.invoke(c,h.getLevel());
            h.assertTrue(p.position().equals(platform),"Teleported before 60 accumulated platform ticks");
            p.fallDistance=12;tick.invoke(c,h.getLevel());
            h.assertTrue(p.position().distanceTo(new Vec3(center.x,scene.dragonSpawn().y,center.z))<.01&&p.fallDistance==0,"Accumulated three seconds did not teleport");
            p.setPos(platform);tick.invoke(c,h.getLevel());
            h.assertTrue(!p.position().equals(platform),"Returning immediately bypassed accumulated limit");
            for(int i=0;i<99;i++)tick.invoke(c,h.getLevel());
            h.assertTrue(since.containsKey(p.getUUID()),"Reset before five continuous seconds below");
            tick.invoke(c,h.getLevel());h.assertTrue(!since.containsKey(p.getUUID()),"Five seconds below did not reset");
            p.setPos(platform);for(int i=0;i<30;i++)tick.invoke(c,h.getLevel());
            p.setPos(scene.dragonSpawn());for(int i=0;i<99;i++)tick.invoke(c,h.getLevel());
            p.setPos(platform);tick.invoke(c,h.getLevel());p.setPos(scene.dragonSpawn());tick.invoke(c,h.getLevel());
            h.assertTrue(since.containsKey(p.getUUID()),"Below time accumulated across a return to the platform");
            for(int i=1;i<100;i++)tick.invoke(c,h.getLevel());
            p.setPos(scene.dragonSpawn().add(0,1.2,0));tick.invoke(c,h.getLevel());h.assertTrue(since.isEmpty(),"Ordinary jump counted as platform");
            p.setPos(platform);tick.invoke(c,h.getLevel());field(ShiHuangController.class,"stage").set(c,ShiHuangController.Stage.DYING);tick.invoke(c,h.getLevel());
            h.assertTrue(since.isEmpty()&&p.position().equals(platform),"Platform restriction continued after dragon combat");
            System.out.println("DRAGON_PLATFORM_PASS 60 accumulated ticks, 100 continuous ticks below, immediate reentry, midfield, spectator excluded, combat-only");h.succeed();
        }finally{cleanup(h,p,observer);}
    }

    @GameTest(template="empty",timeoutTicks=40,batch="platform_phase")
    public static void halfHealthPhaseAllowsPlatformShelter(GameTestHelper h)throws Exception {
        for(boolean air:new boolean[]{false,true}){
            var c=controller(h);var scene=c.scene();field(ShiHuangController.class,"stage").set(c,ShiHuangController.Stage.DRAGON);
            var tick=ShiHuangController.class.getDeclaredMethod("tickPlatformLimit",net.minecraft.server.level.ServerLevel.class);tick.setAccessible(true);
            var since=(Map<?,?>)field(ShiHuangController.class,"platformTime").get(c);
            var d=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());d.setPos(h.absoluteVec(new Vec3(0,180,0)));
            if(air)d.beginPhaseEntry("idle_air",80,d.position());else d.beginCombatAnimation("idle_ground");
            field(ShiHuangController.class,"dragon").set(c,d);var combat=combat(d);
            Vec3 platform=scene.dragonSpawn().add(15,2,0),center=scene.dragonRoom().getCenter();
            h.getLevel().setBlock(BlockPos.containing(platform.add(0,-1,0)),Blocks.STONE.defaultBlockState(),3);
            var p=player(h,"PhaseShelter",platform);
            try{
                for(int i=0;i<59;i++)tick.invoke(c,h.getLevel());
                h.assertTrue(since.containsKey(p.getUUID())&&p.position().equals(platform),"Pre-phase platform accumulation missing");
                d.setHealth(d.getMaxHealth()*.5F);tick.invoke(c,h.getLevel());
                h.assertTrue(p.position().equals(platform)&&since.isEmpty(),"Half-health threshold teleported sheltering player before dragon tick");
                d.tick();h.assertTrue(d.animation().equals(air?"phase50_enter_air":"phase50_enter_ground"),"Phase entry did not start");
                for(int i=0;i<120;i++)tick.invoke(c,h.getLevel());
                h.assertTrue(p.position().equals(platform)&&since.isEmpty(),"Platform restriction active during phase entry");
                field(GraveDragonCombat.class,"phaseStarted").setLong(combat,h.getLevel().getGameTime()-10000);
                d.tick();h.assertTrue(d.animation().equals("phase50_firefield"),"Forced firefield did not start");
                int shelterTicks=0;
                for(int i=1;i<=Math.ceil(GraveDragonPose.duration("phase50_firefield")*20)+1;i++){
                    tick.invoke(c,h.getLevel());shelterTicks++;
                    h.assertTrue(p.position().equals(platform)&&since.isEmpty(),"Platform restriction active during firefield tick "+i);
                    time(d,i);combat.tick();if(!d.isForcingHalfHealthPhase())break;
                }
                h.assertTrue(shelterTicks>60&&combat.phaseDone()&&!d.isForcingHalfHealthPhase(),"Forced phase did not finish");
                for(int i=0;i<59;i++)tick.invoke(c,h.getLevel());
                h.assertTrue(p.position().equals(platform),"Pre-phase accumulation leaked into resumed limit");
                tick.invoke(c,h.getLevel());
                h.assertTrue(p.position().distanceTo(new Vec3(center.x,scene.dragonSpawn().y,center.z))<.01,"Platform limit did not resume after completed phase");
            }finally{cleanup(h,p);d.discard();}
        }
        System.out.println("DRAGON_PLATFORM_PHASE_PASS ground/air threshold, entry, complete firefield shelter, fresh 60-tick limit after phase");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void spectatorSwitchReleasesBothBossTargets(GameTestHelper h)throws Exception {
        Vec3 origin=h.absoluteVec(new Vec3(0,180,0));var p=player(h,"SwitchObserver",origin.add(2,0,0));var valid=player(h,"LivingTarget",origin.add(0,0,5));
        var emperor=ModEntities.SHI_HUANG.get().create(h.getLevel());emperor.setPos(origin);emperor.setNoGravity(true);
        var d=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());d.setPos(origin);d.beginPhaseEntry("idle_air",1,origin);var c=combat(d);
        try{
            emperor.setTarget(p);field(ShiHuangEntity.class,"rapidDestination").set(emperor,p.position());field(ShiHuangEntity.class,"rapidStep").setDouble(emperor,1);
            field(GraveDragonCombat.class,"turnTarget").set(c,p);field(GraveDragonCombat.class,"queuedAction").set(c,"air_fireball3");field(GraveDragonCombat.class,"turnUntil").setLong(c,h.getLevel().getGameTime()+200);
            p.setGameMode(GameType.SPECTATOR);emperor.tick();c.tick();
            h.assertTrue(emperor.getTarget()==valid&&field(ShiHuangEntity.class,"rapidDestination").get(emperor)==null,"Emperor retained spectator target/movement");
            h.assertTrue(field(GraveDragonCombat.class,"turnTarget").get(c)==null&&field(GraveDragonCombat.class,"queuedAction").get(c)==null,"Dragon retained spectator turn queue");
            var nearest=GraveDragonCombat.class.getDeclaredMethod("nearestPlayer",double.class);nearest.setAccessible(true);
            h.assertTrue(nearest.invoke(c,100d)==valid,"Dragon preferred nearer spectator");
            var plan=new GraveDragonReposition.Plan("ground_double_bite",p,p.position(),origin,origin,0,0,"turn_ground_left",h.getLevel().getGameTime(),10);
            field(GraveDragonCombat.class,"reposition").set(c,plan);c.tick();h.assertTrue(field(GraveDragonCombat.class,"reposition").get(c)==null,"Ground reposition retained spectator");
            System.out.println("BOSS_SPECTATOR_PASS nearest spectator excluded, emperor retargeted, dragon turn/reposition queues cancelled");h.succeed();
        }finally{cleanup(h,p,valid);emperor.discard();d.discard();}
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void dragonBurnReducedWithoutChangingOrdinaryFire(GameTestHelper h)throws Exception {
        var p=player(h,"DragonBurn",h.absoluteVec(new Vec3(0,180,0)));var normal=player(h,"NormalBurn",p.position());
        try{
            GraveDragonDamage.ignite(p,5);normal.igniteForSeconds(5);
            p.hurt(p.damageSources().onFire(),3);normal.hurt(normal.damageSources().onFire(),3);
            h.assertTrue(Math.abs(p.getHealth()-98)<.001&&Math.abs(normal.getHealth()-97)<.001,"Dragon and ordinary burning not distinguished");
            p.clearFire();p.doTick();h.assertTrue(!p.getPersistentData().contains("GraveDragonBurnUntil"),"Extinguishing retained dragon burn marker");
            System.out.println("DRAGON_BURN_PASS damage reduced once, ordinary fire unchanged, extinguishing clears attribution");h.succeed();
        }finally{cleanup(h,p,normal);}
    }

    private static ShiHuangController controller(GameTestHelper h)throws Exception{var c=new ShiHuangController();field(ShiHuangController.class,"scene").set(c,new ShiHuangScene(h.absolutePos(new BlockPos(0,150,-500))));return c;}
    private static ServerPlayer player(GameTestHelper h,String name,Vec3 position)throws Exception{
        var server=h.getLevel().getServer();var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),name),false);
        var p=new ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}};
        p.setGameMode(GameType.SURVIVAL);field(ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);p.setPos(position);h.getLevel().players().add(p);return p;
    }
    private static void cleanup(GameTestHelper h,ServerPlayer... players){for(var p:players)h.getLevel().players().remove(p);}
    private static java.lang.reflect.Field field(Class<?> cls,String name)throws Exception{var f=cls.getDeclaredField(name);f.setAccessible(true);return f;}
    private static GraveDragonCombat combat(GraveDragonEntity d)throws Exception{return (GraveDragonCombat)field(GraveDragonEntity.class,"combat").get(d);}
    @SuppressWarnings("unchecked") private static void time(GraveDragonEntity d,int ticks)throws Exception{d.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Long>)field(GraveDragonEntity.class,"ANIMATION_START").get(null),d.level().getGameTime()-ticks);}
}
