package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.*;
import icu.icuqalt10.panlingre.init.*;
import icu.icuqalt10.panlingre.network.TombLightPayload;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class ShiHuangAllyControlTest {
    private static java.lang.reflect.Field field(Class<?> c,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static void clock(ShiHuangEntity b,long time)throws Exception{b.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"FROZEN_AT").get(null),time);}
    private static ServerPlayer player(GameTestHelper h,java.util.List<TombLightPayload> lights)throws Exception{
        var server=h.getLevel().getServer();var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AllyControl"),false);
        var p=new ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload() instanceof TombLightPayload light)lights.add(light);}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setInvulnerable(true);h.getLevel().players().add(p);return p;
    }
    private static ShiHuangEntity boss(GameTestHelper h)throws Exception{
        var b=ModEntities.SHI_HUANG.get().create(h.getLevel());b.moveTo(h.absoluteVec(new Vec3(0,150,0)),0,0);b.setNoGravity(true);b.setEnhanced(true);b.setNoAi(true);h.getLevel().addFreshEntity(b);
        field(ShiHuangEntity.class,"nextAttack").setLong(b,Long.MAX_VALUE);return b;
    }
    private static ShiHuangCombat combat(ShiHuangEntity b)throws Exception{return (ShiHuangCombat)field(ShiHuangEntity.class,"combat").get(b);}
    private static ShiHuangDragonAlly ally(ShiHuangEntity b)throws Exception{return (ShiHuangDragonAlly)field(ShiHuangCombat.class,"ally").get(combat(b));}
    private static GraveDragonEntity dragon(ShiHuangEntity b)throws Exception{return (GraveDragonEntity)field(ShiHuangDragonAlly.class,"dragon").get(ally(b));}
    private static void orbit(ShiHuangEntity b)throws Exception{combat(b).background();field(ShiHuangDragonAlly.class,"spawned").setDouble(ally(b),b.actionClock(0)-500);combat(b).background();}

    @GameTest(template="empty",timeoutTicks=100,batch="ally_freeze")
    public static void cauldronDoesNotRespawnAlly(GameTestHelper h)throws Exception{
        var b=boss(h);orbit(b);var d=dragon(b);var p=player(h,new java.util.ArrayList<>());
        p.setPos(b.position().add(0,0,-2));p.setYRot(0);p.setXRot(0);p.getAttribute(ModAttributes.MAGIC_DAMAGE).setBaseValue(20);
        b.setNoAi(false);
        ((icu.icuqalt10.panlingre.item.warlock.hun_yuan_shen_din)ModItems.hun_yuan_shen_din.get()).skill_use(h.getLevel(),p,new net.minecraft.world.item.ItemStack(ModItems.hun_yuan_shen_din.get()),2);
        h.assertTrue(b.hasEffect(ModEffects.freeze),"Real cauldron skill did not freeze emperor");
        h.assertTrue(!d.isRemoved()&&dragon(b)==d,"Freeze deleted the orbiting ally; next tick will replay entrance");
        double pose=d.animationSeconds(0);Vec3 pos=d.position();
        h.runAfterDelay(8,()->{h.assertTrue(!d.isRemoved()&&Math.abs(d.animationSeconds(0)-pose)<.001&&d.position().distanceTo(pos)<.001,"Frozen ally moved or restarted");});
        h.runAfterDelay(22,()->{try{
            h.assertTrue(!b.hasEffect(ModEffects.freeze)&&!b.isNoAi(),"Cauldron did not expire");
            h.assertTrue(dragon(b)==d&&!d.isRemoved()&&!d.animation().startsWith("ally_emerge"),"Unfreeze recreated entrance");
            h.assertTrue(d.animationSeconds(0)-pose<.3,"Ally clock jumped over frozen ticks");
            b.discard();h.getLevel().players().remove(p);System.out.println("CAULDRON_ALLY_IDENTITY_PASS");h.succeed();
        }catch(Exception e){throw new RuntimeException(e);}});
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ally_patrol")
    public static void orbitFiresAndStrikes(GameTestHelper h)throws Exception{
        var b=boss(h);var lights=new java.util.ArrayList<TombLightPayload>();var p=player(h,lights);p.setPos(b.position().add(0,0,5));
        var weak=player(h,lights);weak.setPos(b.position().add(6,0,0));weak.setHealth(3);
        p.setHealth(3);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(10);
        var healthy=player(h,lights);healthy.setPos(b.position().add(0,0,-5));healthy.setHealth(4);
        long start=h.getLevel().getGameTime();clock(b,start);orbit(b);var d=dragon(b);b.getRandom().setSeed(73);
        int attacks=0,previous=-1;boolean fire=false,thunder=false;
        try{
            for(int tick=0;tick<3200;tick++){
                h.getLevel().getServer().getWorldData().overworldData().setGameTime(start+tick);clock(b,start+tick);combat(b).background();
                if(field(ShiHuangDragonAlly.class,"patrolWindup").getInt(ally(b))==1){
                    if(previous>=0)h.assertTrue(tick-previous>=200&&tick-previous<=400,"Patrol interval outside 10-20 seconds");
                    previous=tick;attacks++;
                    boolean shot=field(ShiHuangDragonAlly.class,"patrolFireball").getBoolean(ally(b));
                    @SuppressWarnings("unchecked") var points=(java.util.List<Vec3>)field(ShiHuangDragonAlly.class,"patrolPoints").get(ally(b));
                    Vec3 expected=shot?weak.getEyePosition():weak.position();
                    h.assertTrue(points.getFirst().distanceTo(expected)<.01,"Patrol ignored lowest health / armor tie-break");
                    h.assertTrue(points.size()==(shot?1:3),"Patrol must choose one fireball or exactly three thunder points");
                    if(shot)fire=true;else thunder=true;System.out.println("PATROL_CHOICE tick="+tick+" fireball="+shot);
                }
            }
            var shots=h.getLevel().getEntitiesOfClass(GraveDragonFireballEntity.class,b.getBoundingBox().inflate(150),e->e.getOwner()==d);
            h.assertTrue(fire&&thunder&&attacks>=8&&!shots.isEmpty(),"Patrol did not execute both choices");
            h.assertTrue(shots.stream().allMatch(s->!s.blue()&&s.getDeltaMovement().y<0),"Patrol ball must be red and aimed down");
            long bolts=lights.stream().filter(l->l.shape()==TombLightPayload.LIGHTNING).count();
            h.assertTrue(bolts>=9,"Patrol lightning never finished its charge: "+bolts);
            shots.forEach(net.minecraft.world.entity.Entity::discard);
            System.out.println("ALLY_PATROL_PASS attacks="+attacks+" shots="+shots.size()+" bolts delivered="+bolts+"; 200-400 ticks, weakest health/armor, red ball, one aimed/two random bolts");h.succeed();
        }finally{h.getLevel().getServer().getWorldData().overworldData().setGameTime(start);b.discard();for(var player:java.util.List.of(p,weak,healthy))h.getLevel().players().remove(player);}

    }

    @GameTest(template="empty",timeoutTicks=100,batch="ally_command_freeze")
    public static void purpleGlowRejectsFreeze(GameTestHelper h)throws Exception{
        for(boolean rage:new boolean[]{false,true})for(String clip:new String[]{"ally_d01_longting","ally_d02_chongfeng","ally_d03_longxi","ultimate_full"}){
            var b=boss(h);orbit(b);var d=dragon(b);b.setNoAi(false);
            if(rage){@SuppressWarnings("unchecked") var key=(net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"RAGE_START").get(null);b.getEntityData().set(key,0L);}
            b.play(clip);b.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.freeze,100,0));
            h.assertTrue(b.hasCombatGlow(0)&&!b.hasEffect(ModEffects.freeze)&&!b.isNoAi()&&b.animation().equals(clip),"Purple glow did not reject freeze: "+clip);
            h.assertTrue(!d.isRemoved()&&dragon(b)==d&&!d.animation().equals("ally_orbit_return"),"Immune command was interrupted");
            b.discard();
        }
        var b=boss(h);b.setNoAi(false);b.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.freeze,100,0));
        h.assertTrue(b.hasEffect(ModEffects.freeze),"Ordinary phase unexpectedly immune");
        b.setHealth(b.getMaxHealth()*.2F);b.tick();
        h.assertTrue(b.enraged()&&!b.hasEffect(ModEffects.freeze)&&!b.isNoAi(),"Entering rage did not clear existing freeze");
        b.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.freeze,100,0));h.assertTrue(!b.hasEffect(ModEffects.freeze),"Enraged idle accepted freeze");b.discard();
        System.out.println("PURPLE_IMMUNITY_PASS three commands, ultimate, enraged idle, existing freeze cleared on rage");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ally_weights")
    public static void equalCategoryAndMoveWeights(GameTestHelper h)throws Exception{
        var b=boss(h);orbit(b);b.addMana(20);var counts=new java.util.HashMap<String,Integer>();int skills=0,commands=0;
        for(int seed=0;seed<40000;seed++){
            long value=seed*7919L;b.getRandom().setSeed(value);var expected=net.minecraft.util.RandomSource.create(value);
            boolean skill=expected.nextInt(100)>=50,command=skill&&expected.nextBoolean();String chosen=combat(b).choose(3);
            h.assertTrue(chosen.startsWith(command?"ally_":skill?"skill_":"combo_"),"Category split not 50/50 then 50/50");
            if(skill){skills++;counts.merge(chosen,1,Integer::sum);}if(command)commands++;
        }
        for(String prefix:new String[]{"skill_","ally_"}){
            var names=ShiHuangCombat.ACTIONS.keySet().stream().filter(n->n.startsWith(prefix)).toList();double mean=(double)(prefix.equals("ally_")?commands:skills-commands)/names.size();
            for(String name:names)h.assertTrue(Math.abs(counts.getOrDefault(name,0)-mean)<mean*.12,"Unequal move weight: "+name+" count="+counts.get(name)+" mean="+mean);
        }
        b.addMana(-20);for(int i=0;i<50;i++)h.assertTrue(combat(b).choose(3).startsWith("combo_"),"Insufficient mana bypassed");b.discard();
        System.out.println("EQUAL_WEIGHTS_PASS 40000 draws, skills="+skills+" commands="+commands+" uniform moves="+counts);h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="ally_damage")
    public static void explicitUltimateAndFireEarthDamage(GameTestHelper h)throws Exception{
        var b=boss(h);var p=player(h,new java.util.ArrayList<>());p.setInvulnerable(false);field(ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setPos(b.position().add(0,0,6));b.setTarget(p);
        b.play("ultimate_full");
        @SuppressWarnings("unchecked") var key=(net.minecraft.network.syncher.EntityDataAccessor<Long>)field(ShiHuangEntity.class,"START").get(null);
        b.getEntityData().set(key,h.getLevel().getGameTime()-130);combat(b).tick();h.assertTrue(p.getHealth()==900,"Ultimate must deal 100, health="+p.getHealth());b.discard();h.getLevel().players().remove(p);
        b=boss(h);p=player(h,new java.util.ArrayList<>());p.setInvulnerable(false);field(ServerPlayer.class,"spawnInvulnerableTime").setInt(p,0);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setPos(b.position().add(0,0,6));b.setTarget(p);b.play("skill_s07_rongyan");
        double release=ShiHuangCombat.ACTIONS.get(b.animation()).windows().getFirst().to();int end=(int)Math.ceil(release*20);
        for(int t=0;t<=end;t++){b.getEntityData().set(key,h.getLevel().getGameTime()-t);combat(b).tick();}
        var effects=(java.util.List<?>)field(ShiHuangCombat.class,"effects").get(combat(b));h.assertTrue(!effects.isEmpty(),"Fire-earth projectile was not released");var effect=effects.getFirst();
        Vec3 from=(Vec3)field(effect.getClass(),"from").get(effect),to=(Vec3)field(effect.getClass(),"to").get(effect);p.setPos(from.lerp(to,.5).add(0,-.9,0));
        for(int t=end+1;t<end+30;t++){b.getEntityData().set(key,h.getLevel().getGameTime()-t);combat(b).tick();}
        h.assertTrue(p.getHealth()==965,"Fire-earth must deal 35, health="+p.getHealth());b.discard();h.getLevel().players().remove(p);
        System.out.println("EXPLICIT_DAMAGE_PASS ultimate=100, moving fire-earth projectile=35");h.succeed();
    }
}
