package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonPhaseTest {
    private static java.lang.reflect.Field field(Class<?> type,String name)throws ReflectiveOperationException {
        var f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    @GameTest(template="empty",timeoutTicks=680)
    public static void phaseFinishesFromGroundAndAirAcrossRoom(GameTestHelper h)throws Exception {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(1024,72,0));
        var scene=new ShiHuangScene(origin);
        var room=scene.dragonRoom();
        for(int x=((int)room.minX>>4)-1;x<=((int)room.maxX>>4)+1;x++)
            for(int z=((int)room.minZ>>4)-1;z<=((int)room.maxZ>>4)+1;z++)level.setChunkForced(x,z,true);
        var template=level.getStructureManager().getOrCreate(ResourceLocation.parse("panlingre:instance/shihuang"));
        template.placeInWorld(level,origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),level.random,2);
        var controller=new ShiHuangController();field(ShiHuangController.class,"scene").set(controller,scene);
        var dragons=new ArrayList<GraveDragonEntity>();
        var combats=new ArrayList<GraveDragonCombat>();
        for(double x:new double[]{-65,0,65})for(double y:new double[]{0,20})
        for(double z:new double[]{-45,0,55})for(float yaw:new float[]{0,90,180,270}) {
            var d=new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(),level) {
                @Override ShiHuangController tombController(){return controller;}
                @Override public boolean moveSceneTo(Vec3 destination,float facing,double floor) {
                    Vec3 before=position();
                    boolean moved=super.moveSceneTo(destination,facing,floor);
                    h.assertTrue(before.distanceTo(position())<2,"Phase entry teleported from "+new Vec3(x,y,z));
                    h.assertTrue(moved,"Phase entry blocked from "+new Vec3(x,y,z)+" yaw="+yaw+" seconds="+animationSeconds(0));
                    return moved;
                }
            };
            d.moveTo(scene.dragonSpawn().add(x,y,z),yaw,0);d.yBodyRot=d.yBodyRotO=d.yHeadRot=yaw;
            if(y>0)d.beginPhaseEntry("idle_air",80,d.position());
            else d.beginCombatAnimation("idle_ground");
            d.refreshCombatPose();
            if(!d.combatPoseFitsAt(d.animation(),0,yaw,0,d.position(),scene.dragonSpawn().y))continue;
            System.out.println("PHASE_START case="+dragons.size()+" offset="+d.position().subtract(scene.dragonSpawn())+" yaw="+yaw);
            field(GraveDragonEntity.class,"introComplete").setBoolean(d,true);
            d.setNoAi(false);d.setHealth(d.getMaxHealth()*.51F);
            level.addFreshEntity(d);dragons.add(d);
            h.assertTrue(d.hurtPart(0,level.damageSources().generic(),1000000),"Half health hit rejected");
            combats.add((GraveDragonCombat)field(GraveDragonEntity.class,"combat").get(d));
        }
        h.assertTrue(dragons.size()==72,"Room fixture no longer covers the 72 room-bounded start poses");
        long began=level.getGameTime();boolean[] spraying=new boolean[dragons.size()],fields=new boolean[dragons.size()];
        var fireField=field(GraveDragonCombat.class,"fireField");
        h.onEachTick(()->{
            try {
                int age=(int)(level.getGameTime()-began);boolean done=true;
                for(int i=0;i<dragons.size();i++) {
                    var d=dragons.get(i);var combat=combats.get(i);
                    var bounds=d.roomMovementBounds();
                    for(var part:d.getWorldParts()) {
                        var box=part.getOrientedBox().enclosingAabb();
                        h.assertTrue(box.minX>=bounds.minX-.001&&box.maxX<=bounds.maxX+.001
                                &&box.minZ>=bounds.minZ-.001&&box.maxZ<=bounds.maxZ+.001&&box.maxY<=bounds.maxY+.001,
                                "Phase body escaped room: "+d.animation()+" case="+i);
                    }
                    if(d.animation().equals("phase50_firefield"))spraying[i]=true;
                    fields[i]|=fireField.get(combat)!=null;
                    done&=spraying[i]&&fields[i]&&!d.isInvulnerable();
                }
                if(done) {
                    for(var d:dragons)d.discard();
                    for(int x=((int)room.minX>>4)-1;x<=((int)room.maxX>>4)+1;x++)
                        for(int z=((int)room.minZ>>4)-1;z<=((int)room.maxZ>>4)+1;z++)level.setChunkForced(x,z,false);
                    System.out.println("PHASE_PASS cases="+dragons.size()+" elapsed="+age+" fire_fields=true continuous_motion=true");h.succeed();
                }
                if(age==650) {
                    for(int i=0;i<dragons.size();i++) {
                        var d=dragons.get(i);
                        h.assertTrue(spraying[i]&&fields[i]&&!d.isInvulnerable(),"Half-health incomplete case "+i+": "+d.animation()+" fireField="+fields[i]);
                    }
                }
            }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        });
    }
}
