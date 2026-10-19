package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class GraveDragonLookTest {
    @GameTest(template="empty",timeoutTicks=80)
    public static void uprightHeadAcrossAnimatedNecks(GameTestHelper h) {
        int checked=0;
        for(String clip:List.of("idle_air","idle_ground","ally_orbit","fly","takeoff")) {
            for(double time:new double[]{0,.25,.8,1.5}) {
                var poses=new LinkedHashMap<>(GraveDragonPose.sample(clip,time,false).bones());
                var head=poses.get("head_ctrl");
                poses.put("head_ctrl",new GraveDragonPose.BonePose(head.rotation().add(0,0,.5),head.position(),head.scale()));
                var base=GraveDragonPose.rebuild(poses);
                for(float bodyYaw:new float[]{0,75,179})for(float yaw:new float[]{-70,0,70}) {
                    var transform=GraveDragonPose.modelToEntity(bodyYaw,12,1.5F);
                    var before=new Matrix4f(transform).mul(base.matrices().get("head"));
                    var forward=before.transformDirection(new Vector3f(0,0,-1)).normalize();
                    double heading=Math.atan2(-forward.x,-forward.z)+Math.toRadians(yaw);
                    double elevation=Math.atan2(forward.y,Math.hypot(forward.x,forward.z))+Math.toRadians(-20);
                    var expected=new Vector3f((float)(-Math.sin(heading)*Math.cos(elevation)),(float)Math.sin(elevation),
                            (float)(-Math.cos(heading)*Math.cos(elevation)));
                    var looked=GraveDragonPose.look(base,transform,yaw,-20,1);
                    var after=new Matrix4f(transform).mul(looked.matrices().get("head"));
                    var actual=after.transformDirection(new Vector3f(0,0,-1)).normalize();
                    var right=after.transformDirection(new Vector3f(1,0,0)).normalize();
                    h.assertTrue(actual.dot(expected)>.99999F,"Head misses target direction: "+clip+" at "+time);
                    h.assertTrue(Math.abs(right.y)<.0001F,"Head rolls sideways while tracking: "+clip);
                    h.assertTrue(looked.bones().get("tail_tip").equals(base.bones().get("tail_tip")),"Look changed unrelated tail");
                    h.assertTrue(GraveDragonPose.look(base,transform,yaw,-20,0)==base,"Disabled look changed animation");
                    checked++;
                }
            }
        }
        System.out.println("DRAGON_LOOK upright world-space poses="+checked);
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void breathOwnsHeadButTakeoffTracks(GameTestHelper h) {
        for(String clip:List.of("takeoff","fly","idle_air","idle_ground","ally_orbit"))
            h.assertTrue(GraveDragonEntity.tracksTarget(clip),"Tracking incorrectly disabled: "+clip);
        for(String clip:List.of("air_fireball3","ground_firezone","air_open_fire_ring","ground_fan_breath",
                "ground_sweep_breath","ground_turn_breath","phase50_firefield","ground_rift_fan","ally_combo_charge",
                "ally_combo_breath","ally_combo_coil","ground_charge","ground_coil"))
            h.assertTrue(!GraveDragonEntity.tracksTarget(clip),"Skill head overridden by look: "+clip);
        h.succeed();
    }
}
