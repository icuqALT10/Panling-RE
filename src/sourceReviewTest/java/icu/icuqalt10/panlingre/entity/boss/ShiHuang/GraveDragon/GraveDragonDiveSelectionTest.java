package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonDiveSelectionTest {
    @GameTest(template="empty",timeoutTicks=100)
    public static void diveRemainsNaturallySelectableInTomb(GameTestHelper h)throws Exception {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(1024,72,0));var scene=new ShiHuangScene(origin);
        level.getStructureManager().getOrCreate(ResourceLocation.parse("panlingre:instance/shihuang")).placeInWorld(level,origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),level.random,2);
        var p=icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangReportedSkillsTest.player(h,new ArrayList<>());
        var d=ModEntities.GRAVE_DRAGON.get().create(level);d.setPos(scene.dragonSpawn().add(0,12,0));d.beginPhaseEntry("idle_air",80,d.position());
        var c=new GraveDragonCombat(d);
        var pool=GraveDragonCombat.class.getDeclaredMethod("actionPool",net.minecraft.server.level.ServerPlayer.class,boolean.class);pool.setAccessible(true);
        var select=GraveDragonCombat.class.getDeclaredMethod("select",net.minecraft.server.level.ServerPlayer.class);select.setAccessible(true);
        int clear=0,total=0;
        try {
            for(double x:new double[]{-40,0,40})for(double z:new double[]{-25,0,25})for(float yaw:new float[]{0,90,180,270}) {
                d.setPos(scene.dragonSpawn().add(x,12,z));d.setYRot(yaw);d.yBodyRot=yaw;p.setPos(d.position().add(0,-12,8));
                var choices=(List<?>)pool.invoke(c,p,false);boolean found=choices.stream().anyMatch(a->((GraveDragonActions.Action)a).name().equals("air_dive_slam"));
                if(found)clear++;total++;
                System.out.println("DIVE_POOL x="+x+" z="+z+" yaw="+yaw+" available="+found);
                if(x==0&&z==0)h.assertTrue(found,"Dive excluded at room spawn yaw="+yaw);
            }
            d.setPos(scene.dragonSpawn().add(0,12,0));d.setYRot(0);d.yBodyRot=0;p.setPos(scene.dragonSpawn().add(0,0,8));d.getRandom().setSeed(42);
            int selected=0;
            for(int i=0;i<100;i++) {
                var choice=select.invoke(c,p);var action=choice.getClass().getDeclaredMethod("action");action.setAccessible(true);
                if(((GraveDragonActions.Action)action.invoke(choice)).name().equals("air_dive_slam"))selected++;
            }
            System.out.println("DIVE_SELECTION clear="+clear+"/"+total+" actual_weighted_draws="+selected+"/100");
            h.assertTrue(selected>0,"Normal weighted selection never picks dive");h.succeed();
        }finally{d.discard();level.players().remove(p);}
    }
}
