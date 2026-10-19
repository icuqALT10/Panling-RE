package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.GraveDragonActionPayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonTransitionTest {
    @GameTest(template="empty",timeoutTicks=20)
    public static void actionPacketCarriesCurrentClock(GameTestHelper h) throws Exception {
        var boss=ModEntities.SHI_HUANG.get().create(h.getLevel());
        var dragon=ModEntities.GRAVE_DRAGON.get().create(h.getLevel());
        h.getLevel().addFreshEntity(boss);
        dragon.setEmperorClock(boss);
        dragon.beginEmperorAllyAnimation("ally_combo_coil",boss.actionClock(0)-60);
        double start=boss.actionClock(0)-.5;
        dragon.beginEmperorAllyAnimation("ally_combo_charge",start);
        var combatField=GraveDragonEntity.class.getDeclaredField("combat");combatField.setAccessible(true);
        var combat=(GraveDragonCombat)combatField.get(dragon);
        var activationField=GraveDragonCombat.class.getDeclaredField("activation");activationField.setAccessible(true);
        var packet=(GraveDragonActionPayload)activationField.get(combat);
        h.assertTrue(packet.action().equals("ally_combo_charge")&&packet.emperorId()==boss.getId()
                &&Math.abs(packet.emperorStart()-start)<1e-7,"Activation sent the previous clip's clock");
        h.assertTrue(packet.blendFrom().equals("ally_combo_coil")&&Math.abs(packet.blendFromSeconds()-3)<1e-7,
                "Charge lost the preceding coil pose");
        dragon.beginCombatAnimation("fly");
        var tracking=combat.trackingPayload();
        h.assertTrue(tracking.action().equals("fly")&&tracking.startTick()==dragon.animationStart()
                &&tracking.points().isEmpty(),"Tracking replayed a stale activation");
        boss.discard();dragon.discard();h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=20)
    public static void escapeClipsNeverInvertOrEnlarge(GameTestHelper h) {
        for(String name:new String[]{"escape_enter_coffin","escape_wall_impact"}) {
            double duration=GraveDragonPose.duration(name);
            for(int i=0;i<=Math.ceil(duration*80);i++) {
                var frame=GraveDragonPose.sample(name,Math.min(duration,i/80.0),false);
                for(var entry:frame.bones().entrySet()) {
                    var scale=entry.getValue().scale();
                    h.assertTrue(scale.x>0&&scale.y>0&&scale.z>0&&scale.x<=1.0001&&scale.y<=1.0001&&scale.z<=1.0001,
                            "Invalid escape scale: "+name+" "+entry.getKey()+" frame="+i+" "+scale);
                }
            }
        }
        h.succeed();
    }
}
