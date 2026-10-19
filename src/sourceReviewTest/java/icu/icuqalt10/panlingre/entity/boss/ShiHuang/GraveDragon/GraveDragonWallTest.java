package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonWallTest {
    @GameTest(template="empty",timeoutTicks=40)
    public static void wholeBodyStaysInsideWhileMovingAndTurning(GameTestHelper h)throws Exception {
        var c=controller(h);var d=dragon(h,c);var room=d.roomMovementBounds();
        for(float yaw:new float[]{0,45,90,135,180,225,270,315}) {
            d.yBodyRot=yaw;d.beginCombatAnimation("idle_ground");
            for(Vec3 corner:new Vec3[]{new Vec3(room.minX,room.minY,room.minZ),new Vec3(room.maxX,room.minY,room.maxZ)}) {
                d.setPos(corner);d.refreshCombatPose();assertInside(h,d);
                d.move(MoverType.SELF,new Vec3(500,0,-500));d.refreshCombatPose();assertInside(h,d);
                d.yBodyRot+=5;d.refreshCombatPose();assertInside(h,d);
            }
        }
        for(String clip:new String[]{"ground_tailsweep","ground_coil","air_dive_slam","phase50_firefield","dying_thrash"}) {
            d.beginCombatAnimation(clip);d.setPos(room.maxX-1,room.minY+12,room.maxZ-1);
            if(clip.equals("air_dive_slam"))d.lockDivePath(d.position(),d.yBodyRot,new Vec3(room.maxX-4,room.minY,room.maxZ-4));
            for(int tick=0;tick<=GraveDragonPose.duration(clip)*20;tick++) {
                time(d,tick);if(clip.equals("air_dive_slam"))d.advanceDiveRoot(tick/20d);
                d.refreshCombatPose();assertInside(h,d);
            }
        }
        d.discard();System.out.println("WALL_PASS full body, corners, movement, turns, attacks, dynamic dive and phase");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void retreatTurnDoesNotRestoreAirborneHeight(GameTestHelper h)throws Exception {
        var c=controller(h);var d=dragon(h,c);Vec3 air=c.scene().dragonSpawn().add(0,20,0);
        d.setPos(air);d.beginPhaseEntry("idle_air",80,air);
        field(ShiHuangController.class,"dragon").set(c,d);
        field(ShiHuangController.class,"retreatPivot").set(c,air.add(d.sceneTurnPivotOffset(0)));
        field(ShiHuangController.class,"retreatFromYaw").setFloat(c,0);
        field(ShiHuangController.class,"retreatToYaw").setFloat(c,90);
        field(ShiHuangController.class,"retreatTurnTicks").setInt(c,60);
        d.beginRetreatLanding();time(d,200);
        var form=GraveDragonEntity.class.getDeclaredMethod("tickForm");form.setAccessible(true);form.invoke(d);
        double floor=c.scene().dragonSpawn().y;
        h.assertTrue(!d.flying()&&Math.abs(d.getY()-floor)<.001,"Retreat did not land");
        d.beginCombatAnimation("dying_thrash");
        for(int tick=0;tick<200;tick++) {
            time(d,tick);c.turnRetreat(tick/20d);d.refreshCombatPose();
            h.assertTrue(Math.abs(d.getY()-floor)<.001,"Struggle restored airborne height at tick "+tick+": "+d.getY());
            assertInside(h,d);
        }
        d.discard();System.out.println("RETREAT_FLOOR_PASS air landing then 200 ticks of grounded struggle/turn");h.succeed();
    }

    static void assertInside(GameTestHelper h,GraveDragonEntity d) {
        var room=d.roomMovementBounds();
        for(var part:d.getWorldParts()) {
            var box=part.getOrientedBox().enclosingAabb();
            h.assertTrue(box.minX>=room.minX-.001&&box.maxX<=room.maxX+.001&&box.minZ>=room.minZ-.001&&box.maxZ<=room.maxZ+.001&&box.maxY<=room.maxY+.001,
                    "Body escaped room: "+d.animation()+" t="+d.animationSeconds(0)+" box="+box+" room="+room);
        }
    }
    private static ShiHuangController controller(GameTestHelper h)throws Exception {var c=new ShiHuangController();field(ShiHuangController.class,"scene").set(c,new ShiHuangScene(h.absolutePos(new BlockPos(0,100,-500))));return c;}
    private static GraveDragonEntity dragon(GameTestHelper h,ShiHuangController c) {return new GraveDragonEntity(ModEntities.GRAVE_DRAGON.get(),h.getLevel()){@Override ShiHuangController tombController(){return c;}};}
    private static java.lang.reflect.Field field(Class<?> type,String name)throws Exception {var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static void time(GraveDragonEntity d,int ticks)throws Exception {d.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Long>)field(GraveDragonEntity.class,"ANIMATION_START").get(null),d.level().getGameTime()-ticks);}
}
