package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonActions;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonPose;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonMythic;
import icu.icuqalt10.panlingre.network.GraveDragonActionPayload;
import icu.icuqalt10.panlingre.network.GraveDragonFireFieldPayload;
import icu.icuqalt10.panlingre.client.sound.GraveDragonBreathSound;
import icu.icuqalt10.panlingre.client.sound.GraveDragonDeathCrySound;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;

/** Decorative effects driven by the same activation tick as the dragon's animation. */
public final class GraveDragonEffects {
    private static final int WARNING=0xFFDA36;
    private static final int HOT_WARNING=0xFF593A;
    private static final int[] SEAL_COLORS={0xFFE19A,0x60F595,0x66CFFF,0xFF6045,0xC997FF};
    private static final int[] SEAL_BODY={11,21,10,9,8,7,6,5,4,3,2,1,0,13,14,15,16,17,18,19,20};
    private static final Map<Integer, State> ACTIVE = new HashMap<>();
    private static final Map<Integer, Long> LAST_SEQUENCE = new HashMap<>();
    private static final Map<Integer, GraveDragonFireFieldPayload> FIRE_FIELDS = new HashMap<>();
    private static ClientLevel world;
    private static GraveDragonEntity poseDragon;
    private static String poseAction;
    private static long poseAge;
    private static GraveDragonPose.Frame poseFrame;
    private static Matrix4f poseToEntity;
    private record Warning(Vec3 center,double radius,int color) {}
    private record WarningPath(Vec3 from,Vec3 to) {}
    private static final java.util.List<Warning> warnings=new ArrayList<>();
    private static final java.util.List<WarningPath> warningPaths=new ArrayList<>();

    private GraveDragonEffects() {}

    private static final class State {
        GraveDragonActionPayload activation;
        final long receivedAt;
        long lastAge;
        boolean foundEntity;
        boolean foundAction;
        Vec3 previousTailTip;
        Vec3 previousTailRoot;
        long observedLanding = Long.MIN_VALUE;
        boolean cryPlayed;

        State(GraveDragonActionPayload activation, long receivedAt) {
            this.activation = activation;
            this.receivedAt = receivedAt;
            this.lastAge = Math.max(-1, receivedAt - activation.startTick() - 1);
        }
    }

    /** The payload handler calls this once for each activation, including tracking replays. */
    public static void activate(GraveDragonActionPayload activation) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        resetFor(level);
        if (!GraveDragonPose.animationNames().contains(activation.action())) return;
        Long latest = LAST_SEQUENCE.get(activation.entityId());
        if (latest != null && activation.sequence() <= latest) return;
        LAST_SEQUENCE.put(activation.entityId(), activation.sequence());
        poseDragon = null;
        if (level.getEntity(activation.entityId()) instanceof GraveDragonEntity dragon)
            dragon.applyClientAction(activation);
        State previous = ACTIVE.get(activation.entityId());
        if (previous != null && previous.activation.action().equals(activation.action())
                && previous.activation.startTick() == activation.startTick()) previous.activation = activation;
        else ACTIVE.put(activation.entityId(), new State(activation, level.getGameTime()));
    }

    public static void acceptFireField(GraveDragonFireFieldPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        resetFor(level);
        FIRE_FIELDS.put(payload.entityId(), payload);
    }

    /** Called after the client world advances. No per-frame network traffic is needed. */
    public static void tick() {
        warnings.clear();warningPaths.clear();
        ClientLevel level = Minecraft.getInstance().level;
        GraveDragonBreathSound.update(level);
        if (level == null) {
            world = null;
            poseDragon = null;
            ACTIVE.clear();
            LAST_SEQUENCE.clear();
            FIRE_FIELDS.clear();
            return;
        }
        resetFor(level);
        for(var entity:level.entitiesForRendering())if(entity instanceof GraveDragonEntity dragon
                &&dragon.animation().equals("ally_combo_breath")){
            double seconds=dragon.animationSeconds(0);
            if(seconds>=.5&&seconds<1.25)breathFire(level,dragon,(long)Math.round(seconds*20),level.random);
        }
        FIRE_FIELDS.entrySet().removeIf(entry -> !(level.getEntity(entry.getKey()) instanceof GraveDragonEntity dragon)
                || !dragon.animation().equals("phase50_firefield") || dragon.animationStart() != entry.getValue().startTick());
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, State>> entries = ACTIVE.entrySet().iterator();
        while (entries.hasNext()) {
            State state = entries.next().getValue();
            GraveDragonActionPayload activation = state.activation;
            long age = now - activation.startTick();
            boolean idle = activation.action().equals("idle_air") || activation.action().equals("idle_ground");
            if (idle ? now - state.receivedAt > 40
                    : age > Math.ceil(GraveDragonPose.duration(activation.action()) * 20)
                            + (activation.action().equals("escape_wall_impact") ? 40 : 0)) {
                entries.remove();
                continue;
            }
            if (!(level.getEntity(activation.entityId()) instanceof GraveDragonEntity dragon)) {
                if (state.foundEntity || now - state.receivedAt > 40) entries.remove();
                continue;
            }
            boolean newlyFound = !state.foundEntity;
            state.foundEntity = true;
            if (newlyFound || now - state.receivedAt <= 5
                    && (!activation.action().equals(dragon.animation()) || dragon.animationStart() != activation.startTick()))
                dragon.applyClientAction(activation);
            if (!activation.action().equals(dragon.animation()) || dragon.animationStart() != activation.startTick()) {
                if (state.foundAction || now - state.receivedAt > 5) entries.remove();
                continue;
            }
            state.foundAction = true;
            if (idle) continue;
            if (age < 0 || age <= state.lastAge) continue;
            long previous = state.lastAge;
            state.lastAge = age;
            renderTick(level, dragon, state, previous, age);
        }
        LAST_SEQUENCE.keySet().removeIf(id -> !ACTIVE.containsKey(id) && level.getEntity(id) == null);
    }

    private static void resetFor(ClientLevel level) {
        if (world == level) return;
        world = level;
        poseDragon = null;
        ACTIVE.clear();
        LAST_SEQUENCE.clear();
        FIRE_FIELDS.clear();
        warnings.clear();warningPaths.clear();
    }

    public static void renderLights(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                                    com.mojang.blaze3d.vertex.PoseStack pose,float partial) {
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        resetFor(level);
        for(var warning:warnings)
            TombLightEffects.warningRing(vertices,pose,warning.center,warning.radius,warning.color,1);
        for(var path:warningPaths)
            TombLightEffects.warningPath(vertices,pose,path.from,path.to,WARNING);
        for(var state:ACTIVE.values()) {
            String action=state.activation.action();
            if(!action.equals("roar")&&!action.equals("roar_air")&&!action.equals("escape_wall_impact"))continue;
            if(!(level.getEntity(state.activation.entityId()) instanceof GraveDragonEntity dragon)
                    ||!dragon.animation().equals(action)||dragon.animationStart()!=state.activation.startTick())continue;
            double age=(level.getGameTime()-state.activation.startTick())+(double)partial;
            if(action.equals("escape_wall_impact")){sealExecution(vertices,pose,dragon,state.activation,age);continue;}
            if(age>=18&&age<59) {
                Vec3 center=dragon.getWorldParts()[11].getOrientedBox().center;
                TombLightEffects.warningRing(vertices,pose,center,8+(age-18)*1.8,WARNING,
                        (float)Math.min(1,(59-age)/4));
            }
        }
    }

    private static void sealExecution(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                                      com.mojang.blaze3d.vertex.PoseStack pose,GraveDragonEntity dragon,
                                      GraveDragonActionPayload activation,double age) {
        if(age<0||age>=56)return;
        float fade=(float)Math.min(1,(56-age)/16);
        var parts=dragon.getWorldParts();
        var boxes=new icu.icuqalt10.panlingre.entity.OrientedBoundingBox[SEAL_BODY.length];
        for(int i=0;i<boxes.length;i++)boxes[i]=parts[SEAL_BODY[i]].getOrientedBox();
        for(int color=0;color<5;color++){
            double phase=color*Math.PI*2/5-age*.18;
            Vec3 previous=null;
            for(int step=0;step<=(boxes.length-1)*4;step++){
                double along=step/4.0;
                if(along>age*3+1)break;
                int index=Math.min((int)along,boxes.length-2);double blend=along-index;
                var a=boxes[index];var b=boxes[index+1];double angle=phase+along*.85;
                Vec3 radialA=a.axisX.scale(Math.cos(angle)*(a.halfExtents.x+.7)).add(a.axisY.scale(Math.sin(angle)*(a.halfExtents.y+.7)));
                Vec3 radialB=b.axisX.scale(Math.cos(angle)*(b.halfExtents.x+.7)).add(b.axisY.scale(Math.sin(angle)*(b.halfExtents.y+.7)));
                Vec3 point=a.center.lerp(b.center,blend).add(radialA.lerp(radialB,blend));
                if(previous!=null)TombLightEffects.beam(vertices,pose,previous,point,SEAL_COLORS[color],SEAL_COLORS[color],.06F,fade*.8F);
                if(step==0&&!activation.points().isEmpty()&&age<40){
                    double sourceAngle=color*Math.PI*2/5;
                    Vec3 source=activation.points().getFirst().add(Math.cos(sourceAngle)*15,Math.sin(sourceAngle)*11,0);
                    TombLightEffects.beam(vertices,pose,source,point,0xFFF8E9,SEAL_COLORS[color],.16F,(float)Math.min(1,(40-age)/6));
                }
                if(step%8==0&&previous!=null){
                    Vec3 surface=a.center.lerp(b.center,blend),radial=radialA.lerp(radialB,blend);
                    Vec3 start=surface.add(radial.scale(1.2)).add(a.axisZ.scale(-2));
                    Vec3 end=surface.add(radial.scale(1.6)).add(a.axisZ.scale(3));
                    var bolt=new icu.icuqalt10.panlingre.network.TombLightPayload(5,start,end,SEAL_COLORS[color],.13F*fade,4);
                    TombLightEffects.lightning(vertices,pose,bolt,age%4,activation.seed()+color*97L+step+(long)(age/4)*341873128712L);
                }
                previous=point;
            }
        }
    }

    private static void renderTick(ClientLevel level, GraveDragonEntity dragon, State state, long previous, long age) {
        String action = state.activation.action();
        RandomSource random = RandomSource.create(state.activation.seed() ^ (age * 0x9E3779B97F4A7C15L));
        switch (action) {
            case "escape_enter_coffin" -> {
                if (age < 40) coffinEntryGlow(level, dragon, random);
            }
            case "escape_wall_impact" -> {
                if(age<52&&age%2==0)for(int i=0;i<SEAL_BODY.length;i+=2){
                    var box=dragon.getWorldParts()[SEAL_BODY[i]].getOrientedBox();
                    double angle=age*.5+i;
                    Vec3 point=box.center.add(box.axisX.scale(Math.cos(angle)*(box.halfExtents.x+.4)))
                            .add(box.axisY.scale(Math.sin(angle)*(box.halfExtents.y+.4)));
                    emit(level,ParticleTypes.ELECTRIC_SPARK,point,jitter(random,.18));
                }
                if (!state.cryPlayed && age>=5) {
                    state.cryPlayed = true;
                    // Late tracking must not replay the cry over the smoke phase.
                    if (!dragon.arenaAudio() && age <= 9 && dragon.sceneLandingTick() == Long.MIN_VALUE)
                        Minecraft.getInstance().getSoundManager().play(new GraveDragonDeathCrySound(dragon));
                }
                long landed=dragon.sceneLandingTick();
                if(landed!=Long.MIN_VALUE) {
                    long landingAge=landed-state.activation.startTick();
                    boolean impact=state.observedLanding!=landed && age>=landingAge && age-landingAge<=10;
                    state.observedLanding=landed;
                    if(impact) {
                        double floor=dragon.getY()+dragon.sceneLowestOffset();
                        for(int index : new int[]{0,2,4,6,8,11,13,15,17,19}) {
                            Vec3 p=dragon.getWorldParts()[index].getOrientedBox().center;
                            p=new Vec3(p.x,floor+.15,p.z);
                            impact(level,p,3.5,random);
                        }
                    }
                }
            }

            case "ground_double_bite" -> {
                if (inWindow(action, 0, age) || inWindow(action, 1, age))
                    trail(level, dragon, action, age, "head", ParticleTypes.CLOUD, random);
            }
            case "ground_tail_stab" -> {
                Vec3 point = point(state, 0, dragon.position());
                if (between(age, 15, 29)) ring(level, point, GraveDragonMythic.TAIL_SLAM_RADIUS, age, HOT_WARNING, 48);
                if (at(previous, age, 30)) impact(level, point, GraveDragonMythic.TAIL_SLAM_RADIUS, random);
            }
            case "ground_flank_press_l", "ground_flank_press_r" -> {
                if (at(previous, age, 43)) {
                    var flank = GraveDragonMythic.flank(dragon, action, dragon.yBodyRot, dragon.position());
                    for (int i = 0; i < 512; i++) emit(level, ParticleTypes.CLOUD,
                            flank.surface().add(flank.box().axisZ.scale((random.nextDouble() * 2 - 1) * flank.box().halfExtents.z))
                                    .add(0, random.nextDouble() * 3 - 1.5, 0),
                            flank.direction().scale(2 + random.nextDouble()).add(jitter(random, .15)));
                }
            }
            case "ground_rift_fan" -> rift(level, dragon, state, age, random);
            case "ground_fan_breath" -> {
                if (between(age, 22, 34)) mouthBurst(level, dragon, age, random);
                if (inWindow(action, 0, age)) fanFire(level, dragon, age, random);
            }
            case "air_tail_pierce" -> tailPierceCloud(level, dragon, state, age, random);
            case "air_open_fire_ring" -> {
                Vec3 point = point(state, 0, dragon.position());
                if (between(age, 30, 34)) ring(level, point, GraveDragonMythic.FIRE_RING_RADIUS, age, HOT_WARNING, 72);
                  if (inWindow(action, 0, age)) expandingFire(level, dragon, point, GraveDragonMythic.fireRingRadius(age / 20.0), random);
                if (between(age, 35, 43)) mouthFire(level, dragon, age, random);
            }
            case "air_thunder_trail" -> thunderTrail(level, dragon, state, previous, age, random);
            case "ground_claw_front_l", "ground_claw_front_r" -> {
                if (inWindow(action, 0, age)) clawWind(level, dragon, action, age, action.endsWith("_l") ? "front_l_hand" : "front_r_hand", random);
            }
            case "ground_claw_back_l", "ground_claw_back_r" -> {
                if (inWindow(action, 0, age)) clawWind(level, dragon, action, age, action.endsWith("_l") ? "hind_l_hand" : "hind_r_hand", random);
            }
            case "ground_tailsweep" -> {
                if (inWindow(action, 0, age)) tailWind(level, dragon, action, age, random);
            }
            case "ground_tailroll" -> {
                if (inWindow(action, 0, age)) tailWind(level, dragon, action, age, random);
                if (between(age, 28, 38))
                    ring(level, dragon.position(), 10, age, HOT_WARNING, 48);
                if (at(previous, age, eventTick(action, 3))) impact(level, dragon.position(), 10, random);
            }
            case "ground_slam" -> {
                if (between(age, 20, 40))
                    ring(level, dragon.position(), 10, age, age < 35 ? WARNING : HOT_WARNING, 48);
                if (at(previous, age, eventTick(action, 2))) impact(level, dragon.position(), 10, random);
            }
            case "ground_charge" -> {
                if (age < eventTick(action, 0)) pathWarning(level, dragon.position(), point(state, 0), age);
                if (inWindow(action, 0, age) && age % 2 == 0) chargeCloud(level, dragon, random);
            }
            case "ground_charge_wall" -> {
                if (at(previous, age, eventTick(action, 0))) impact(level, point(state, 0, dragon.position()), 5, random);
            }
            case "air_dive_slam" -> {
                Vec3 landing = state.activation.points().size() == 3 ? point(state, 0) : groundPoint(level, dragon);
                if (between(age, 50, 65))
                    ring(level, landing, 20, age, age < 61 ? WARNING : HOT_WARNING, 48);
                if (at(previous, age, eventTick(action, 2))) {
                    impact(level, landing, 20, random);
                    diveImpactCloud(level, landing, random);
                    GroundSmashRenderer.triggerFracture(landing, 10, 36);
                }
            }
            case "air_coil_lightning" -> lightning(level, dragon, state, previous, age, random);
            case "ground_turn_sweep" -> {
                if (inWindow(action, 0, age)) breathFire(level, dragon, age, random);
            }
            case "ground_turn_breath" -> {
                if (inWindow(action, 0, age)) breathFire(level, dragon, age, random);
            }
            case "ground_sweep_breath" -> {
                if (at(previous, age, eventTick(action, 0))) mouthBurst(level, dragon, age, random);
                if (inWindow(action, 0, age)) breathFire(level, dragon, age, random);
            }
            case "phase50_firefield" -> {
                if (between(age, 40, 73)) mouthBurst(level, dragon, age, random);
                if (inWindow(action, 0, age)) breathFire(level, dragon, age, random);
            }
            case "ground_firezone" -> {
                for (int i = 0; i < 5; i++) {
                    int firedAt = eventTick(action, i);
                    if (at(previous, age, firedAt)) mouthBurst(level, dragon, age, random);
                }
            }
            case "air_fireball3" -> {
                for (int i = 0; i < 3; i++) {
                    int firedAt = eventTick(action, i * 2 + 1);
                    if (at(previous, age, firedAt)) mouthBurst(level, dragon, age, random);
                }
            }
            case "ground_coil" -> {
                if (inWindow(action, 0, age))
                    for (int part : new int[]{11, 8, 9, 10, 13, 14, 15, 16, 17, 18, 19, 20}) {
                        Vec3 center = dragon.getWorldParts()[part].getOrientedBox().center;
                        for (int i = 0; i < 8; i++) emit(level, ParticleTypes.CLOUD,
                                center.add(jitter(random, 2.5)), jitter(random, .18));
                    }
            }
            case "roar", "roar_air" -> { /* The moving ring is sampled each render frame. */ }
            default -> { /* Movement, recovery and roars have no persistent particle emitter. */ }
        }
    }

    private static void coffinEntryGlow(ClientLevel level, GraveDragonEntity dragon, RandomSource random) {
        // The half-entry pose defines the bulb face, shared with coffinEntryOrigin().
        Vec3 plane = dragon.position().add(dragon.sceneHeadOffset("escape_enter_coffin", .2, dragon.yBodyRot));
        Vec3 normal = GraveDragonMythic.forward(dragon.yBodyRot);
        Vec3 side = new Vec3(0, 1, 0).cross(normal);
        dragon.updateClientPartPose(0);
        int remaining = 24;
        for (var part : dragon.getWorldParts()) {
            Vec3[] corners = part.getOrientedBox().corners();
            var rim = new ArrayList<Vec3>(6);
            // Intersect the twelve OBB edges with the lip plane, not its world AABB.
            for (int i = 0; i < 8; i++) for (int bit : new int[]{1, 2, 4}) {
                int j = i ^ bit;
                if (j <= i) continue;
                double a = corners[i].subtract(plane).dot(normal);
                double b = corners[j].subtract(plane).dot(normal);
                if ((a >= 0) == (b >= 0)) continue;
                Vec3 p = corners[i].lerp(corners[j], a / (a - b));
                if (rim.stream().noneMatch(v -> v.distanceToSqr(p) < 1e-8)) rim.add(p);
            }
            if (rim.size() < 3) continue;
            Vec3 center = Vec3.ZERO;
            for (Vec3 p : rim) center = center.add(p);
            Vec3 middle = center.scale(1.0 / rim.size());
            rim.sort(Comparator.comparingDouble(p -> Math.atan2(p.y - middle.y, p.subtract(middle).dot(side))));
            double perimeter = 0;
            for (int i = 0; i < rim.size(); i++) perimeter += rim.get(i).distanceTo(rim.get((i + 1) % rim.size()));
            if (perimeter < .25) continue; // A swallowed, nearly invisible segment should not keep glowing.
            int count = Math.min(remaining, Math.min(10, (int)Math.ceil(perimeter * 2)));
            for (int i = 0; i < count; i++) {
                double along = (i + random.nextDouble()) * perimeter / count;
                int edge = 0;
                while (edge < rim.size() - 1 && along > rim.get(edge).distanceTo(rim.get(edge + 1))) {
                    along -= rim.get(edge).distanceTo(rim.get(edge + 1));
                    edge++;
                }
                Vec3 from = rim.get(edge), to = rim.get((edge + 1) % rim.size());
                Vec3 p = from.lerp(to, along / from.distanceTo(to));
                Vec3 outward = p.subtract(middle).normalize();
                TombLightEffects.beam(p,p.add(outward.scale(.35)),0xFFF2CD,.035F,2);
            }
            remaining -= count;
            if (remaining == 0) break;
        }
    }

    private static void rift(ClientLevel level, GraveDragonEntity dragon, State state, long age, RandomSource random) {
        if (between(age, 25, 41)) {
            var mud = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL));
            Vec3 left = anchor(dragon, dragon.animation(), age, "front_l_hand", Vec3.ZERO);
            Vec3 right = anchor(dragon, dragon.animation(), age, "front_r_hand", Vec3.ZERO);
            Vec3 middle = left.add(right).scale(.5);
            for (Vec3 hand : new Vec3[]{left, right}) {
                Vec3 outward = hand.subtract(middle).multiply(1, 0, 1).normalize();
                for (int i = 0; i < 96; i++) {
                    Particle particle = emit(level, mud, hand.add(jitter(random, .8)),
                            outward.scale(.25 + random.nextDouble() * .25).add(jitter(random, .13))
                                    .add(0, .15 + random.nextDouble() * .25, 0));
                    if (particle != null) {
                        particle.scale(2.5F);
                        particle.setLifetime(Math.max(particle.getLifetime(), 18));
                    }
                }
            }
        }
        if (age != 42) return;
        var dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        for (int ray = 0; ray < 3; ray++) {
            Vec3 origin = point(state, ray, dragon.position());
            for (int i = 0; i < 32; i++) emit(level, dirt, origin.add(jitter(random, 2)).add(0, .4, 0), jitter(random, .4).add(0, .3, 0));
        }
    }

    private static void fanFire(ClientLevel level, GraveDragonEntity dragon, long age, RandomSource random) {
        var fan = GraveDragonMythic.fan(dragon, age / 20.0, dragon.yBodyRot, dragon.position());
        ParticleOptions flame = dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        mouthFire(level, dragon, age, random);
        Vec3 side = fan.forward().cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(fan.forward()).normalize();
        for (int i = 0; i < 320; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double cosine = 1 - random.nextDouble() * (1 - Math.cos(GraveDragonMythic.FAN_HALF_ANGLE));
            double sine = Math.sqrt(1 - cosine * cosine);
            Vec3 direction = fan.forward().scale(cosine).add(side.scale(Math.cos(angle) * sine)).add(up.scale(Math.sin(angle) * sine));
            double distance = fan.range() * Math.sqrt(random.nextDouble());
            Vec3 point = fan.origin().add(direction.scale(distance));
            if (!GraveDragonMythic.clear(dragon, fan.origin(), point)) continue;
            double speed = .8 + random.nextDouble();
            Particle particle = emit(level, flame, point, direction.scale(speed));
            if (particle != null) particle.setLifetime(Math.min(particle.getLifetime(), Math.max(1, (int) Math.ceil((fan.range() - distance) / speed))));
        }
    }

    private static void expandingFire(ClientLevel level, GraveDragonEntity dragon, Vec3 center, double radius, RandomSource random) {
        ParticleOptions flame = dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        for (int i = 0; i < 192; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 point = GraveDragonMythic.ground(dragon, center.add(outward.scale(Math.max(GraveDragonMythic.FIRE_RING_START_RADIUS,
                    radius + (random.nextDouble() * 2 - 1) * 1.5))));
            if (!GraveDragonMythic.clear(dragon, center.add(0, .8, 0), point.add(0, .8, 0))) continue;
            emit(level, flame, point.add(0, random.nextDouble() * 1.5, 0), outward.scale(.6).add(jitter(random, .1)).add(0, .05, 0));
        }
    }

    private static void thunderTrail(ClientLevel level, GraveDragonEntity dragon, State state, long previous, long age, RandomSource random) {
        for (int wave = 0; wave < 3; wave++) {
            int charge = wave == 0 ? 20 : wave == 1 ? 84 : 148;
            int strike = wave == 0 ? 40 : wave == 1 ? 104 : 168;
            if (between(age, charge, strike - 1) && age % 3 == 0) chargeLightning(dragon, random);
            for (Vec3 point : state.activation.points()) {
                if (between(age, charge, strike - 1)) ring(level, point, 5, age, age < strike - 4 ? WARNING : HOT_WARNING, 24);
                if (at(previous, age, strike)) {
                    strikeLightning(point);
                }
            }
        }
    }

    private static void lightning(ClientLevel level, GraveDragonEntity dragon, State state,
                                  long previous, long age, RandomSource random) {
        String action = state.activation.action();
        if (between(age, 20, eventTick(action, 2) - 1) && age % 3 == 0) chargeLightning(dragon, random);
        for (int i = 0; i < state.activation.points().size(); i++) {
            Vec3 strike = state.activation.points().get(i);
            int strikeAt = eventTick(action, 2);
            if (between(age, eventTick(action, 0), strikeAt - 1))
                ring(level, strike, 5, age + i * 7L,
                        age < eventTick(action, 1) ? WARNING : HOT_WARNING, 24);
            if (at(previous, age, strikeAt)) {
                strikeLightning(strike);
            }
        }
    }

    private static void chargeLightning(GraveDragonEntity dragon, RandomSource random) {
        for (int part = 74; part <= 77; part++) {
            var horn = dragon.getWorldParts()[part].getOrientedBox();
            Vec3 from = horn.center.add(horn.axisY.scale(-horn.halfExtents.y));
            Vec3 to = horn.center.add(horn.axisY.scale(horn.halfExtents.y)).add(jitter(random, .6));
            TombLightEffects.lightning(from, to, .055F, 3);
        }
    }

    private static void strikeLightning(Vec3 point) {
        TombLightEffects.lightning(point.add(0, 30, 0), point, .16F, 10);
        TombLightEffects.burst(point.add(0, .12, 0), 0xBCA2FF, 5, 8);
    }

    private static void clawWind(ClientLevel level, GraveDragonEntity dragon, String action, long age,
                                 String bone, RandomSource random) {
        Vec3 hand = anchor(dragon, action, age, bone, Vec3.ZERO);
        for (int i = 0; i < 40; i++)
            emit(level, ParticleTypes.CLOUD, hand.add(jitter(random, 2.5)), jitter(random, 0.12));
    }

    private static void tailWind(ClientLevel level, GraveDragonEntity dragon, String action, long age, RandomSource random) {
        trail(level, dragon, action, age, dragon.partBroken("tail") ? "tail_05" : "tail_tip",
                ParticleTypes.CLOUD, random);
    }

    private static void frontImpact(ClientLevel level, GraveDragonEntity dragon, String action, long age, RandomSource random) {
        if (!dragon.partBroken("front_l"))
            impact(level, anchor(dragon, action, age, "front_l_hand", Vec3.ZERO), 5, random);
        if (!dragon.partBroken("front_r"))
            impact(level, anchor(dragon, action, age, "front_r_hand", Vec3.ZERO), 5, random);
    }

    private static void chargeCloud(ClientLevel level, GraveDragonEntity dragon, RandomSource random) {
        var box = dragon.chargeBounds();
        Vec3 backward = GraveDragonMythic.forward(dragon.yBodyRot).scale(-1);
        for (int i = 0; i < 128; i++) {
            Vec3 position = new Vec3(box.minX + random.nextDouble() * box.getXsize(),
                    box.minY + random.nextDouble() * box.getYsize(), box.minZ + random.nextDouble() * box.getZsize());
            emit(level, ParticleTypes.CLOUD, position, backward.add(jitter(random, .08)).add(0, .05, 0));
        }
    }

    private static void tailPierceCloud(ClientLevel level, GraveDragonEntity dragon, State state, long age, RandomSource random) {
        Vec3 tip = dragon.getWorldParts()[20].getOrientedBox().center;
        Vec3 from = state.previousTailTip == null ? tip : state.previousTailTip;
        Vec3 motion = state.previousTailRoot == null ? Vec3.ZERO : dragon.position().subtract(state.previousTailRoot);
        if (motion.lengthSqr() < .0001) motion = tip.subtract(from);
        if (motion.lengthSqr() < .0001) motion = GraveDragonMythic.forward(dragon.yBodyRot);
        if (between(age, 37, 40) || between(age, 67, 70)) {
            Vec3 backward = motion.normalize().scale(-1);
            for (int i = 0; i < 128; i++) emit(level, ParticleTypes.CLOUD,
                    from.lerp(tip, random.nextDouble()).add(jitter(random, .7)),
                    backward.scale(.8 + random.nextDouble() * .4));
        }
        state.previousTailTip = tip;
        state.previousTailRoot = dragon.position();
    }

    private static void breathFire(ClientLevel level, GraveDragonEntity dragon, long age, RandomSource random) {
        GraveDragonBreath.Flow flow = dragon.animation().equals("ally_combo_breath")
                ? GraveDragonBreath.flowAtPose(dragon,"ground_turn_sweep",age/20.0)
                : GraveDragonBreath.flow(dragon, dragon.animation(), age / 20.0);
        GraveDragonBreath.Geometry geometry = flow.geometry();
        double front = geometry.range();
        mouthFire(level, dragon, age, random);
        Vec3 direction = geometry.direction();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.01) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        Vec3 up = side.cross(direction).normalize();
        ParticleOptions flame = dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        for (int i = 0; i < 240; i++) {
            double along = front * random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            double radial = geometry.radiusAt(along) * Math.sqrt(random.nextDouble());
            Vec3 position = geometry.pointAt(along)
                    .add(side.scale(Math.cos(angle) * radial)).add(up.scale(Math.sin(angle) * radial))
                    .add(jitter(random, .35));
            if (level.clip(new ClipContext(geometry.mouth(), position, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS) continue;
            double speed = .8 + random.nextDouble() * 1.2;
            Vec3 outward = position.subtract(geometry.pointAt(along)).scale(1 / Math.max(4, along));
            Particle particle = emit(level, flame, position,
                    direction.add(outward).scale(speed).add(jitter(random, .12)));
            if (particle != null)
                particle.setLifetime(Math.min(particle.getLifetime(), Math.max(1, (int) Math.ceil((front - along) / speed))));
        }
        if (dragon.animation().equals("phase50_firefield")) {
            var cached = FIRE_FIELDS.get(dragon.getId());
            if (cached != null && cached.startTick() == dragon.animationStart())
                fieldFire(level, dragon, cached, age, random);
        } else if (flow.ground() != null)
            groundFire(level, dragon, flow.ground(), flow.spreadRadius(), age, random);
    }

    private static void mouthFire(ClientLevel level, GraveDragonEntity dragon, long age, RandomSource random) {
        GraveDragonBreath.Geometry geometry = GraveDragonBreath.geometry(dragon);
        Vec3 mouth = geometry.mouth();
        Vec3 velocity = geometry.direction().scale(.45).add(jitter(random, .08));
        ParticleOptions flame = dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        for (int i = 0; i < 64; i++) emit(level, flame,
                mouth.add(jitter(random, 1.5)), velocity.add(jitter(random, .06)));
        if (age % 4 == 0)
            for (int i = 0; i < 8; i++) emit(level, ParticleTypes.SMOKE,
                    mouth.add(jitter(random, 1.5)), velocity.scale(0.45));
    }

    private static void mouthBurst(ClientLevel level, GraveDragonEntity dragon, long age, RandomSource random) {
        mouthFire(level, dragon, age, random);
        Vec3 mouth = GraveDragonBreath.geometry(dragon).mouth();
        for (int i = 0; i < 160; i++)
            emit(level, dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                    mouth.add(jitter(random, 2)), jitter(random, 0.2));
    }

    private static void groundFire(ClientLevel level, GraveDragonEntity dragon, Vec3 center,
                                   double radius, long age, RandomSource random) {
        fireRing(level, dragon, center, radius, age, 80);
        int count = Math.max(100, (int) (1000 * radius * radius / (80 * 80)));
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.pow(random.nextDouble(), 1.2) * radius;
            Vec3 position = center.add(Math.cos(angle) * distance,
                    0.12 + random.nextDouble() * 2, Math.sin(angle) * distance);
            emit(level, dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                    position, new Vec3(Math.cos(angle), 0, Math.sin(angle))
                            .scale(.1 + .35 * (1 - distance / Math.max(1, radius)))
                            .add(jitter(random, .08)).add(0, .08 + random.nextDouble() * .12, 0));
            if (random.nextInt(12) == 0) emit(level, ParticleTypes.SMOKE, position, jitter(random, .06).add(0, .12, 0));
        }
    }

    private static void fireRing(ClientLevel level, GraveDragonEntity dragon, Vec3 center,
                                 double radius, long age, int count) {
        if (age % 3 != 0) return;
        for (int i = 0; i < count; i++) {
            double angle = age * .2 + i * (Math.PI * 2 / count);
            Vec3 position = center.add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
            if (clearLine(level, dragon, center.add(0, 1, 0), position.add(0, 1, 0)))
                emit(level, dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                        position, new Vec3(Math.cos(angle) * .2, .08, Math.sin(angle) * .2));
        }
    }

    private static void trail(ClientLevel level, GraveDragonEntity dragon, String action, long age,
                              String bone, ParticleOptions type, RandomSource random) {
        Vec3 center = anchor(dragon, action, age, bone, Vec3.ZERO);
        for (int i = 0; i < 64; i++) emit(level, type,
                center.add(jitter(random, 2)), jitter(random, 0.1));
    }

    private static void pathWarning(ClientLevel level, Vec3 origin, Vec3 destination, long age) {
        if(destination==null)return;
        warningPaths.add(new WarningPath(origin.add(0,.12,0),destination.add(0,.12,0)));
        warnings.add(new Warning(destination.add(0,.12,0),5,WARNING));
    }

    private static void ring(ClientLevel level, Vec3 center, double radius, long age,
                             int type, int count) {
        warnings.add(new Warning(center.add(0,.12,0),radius,type));
    }

    private static void diveImpactCloud(ClientLevel level, Vec3 center, RandomSource random) {
        for (int i = 0; i < 768; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 position = center.add(outward.scale(Math.sqrt(random.nextDouble()) * 7)).add(0, .2 + random.nextDouble() * .7, 0);
            Particle cloud = emit(level, ParticleTypes.CLOUD, position,
                    outward.scale(.2 + random.nextDouble() * .45).add(0, .12 + random.nextDouble() * .3, 0));
            if (cloud != null) { cloud.scale(6 + random.nextFloat() * 4); cloud.setLifetime(24 + random.nextInt(16)); }
        }
    }

    private static void impact(ClientLevel level, Vec3 center, double radius, RandomSource random) {
        for (int i = 0; i < 320; i++) {
            double angle = i * Math.PI * 2 / 320;
            double distance = radius * (0.2 + random.nextDouble() * 0.8);
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 position = center.add(outward.scale(distance)).add(0, 0.15, 0);
            emit(level, i % 4 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD,
                    position, outward.scale(0.04 + random.nextDouble() * 0.04).add(0, 0.025, 0));
        }
    }

    private static Vec3 point(State state, int index) {
        return index < state.activation.points().size() ? state.activation.points().get(index) : null;
    }

    private static Vec3 point(State state, int index, Vec3 fallback) {
        Vec3 point = point(state, index);
        return point == null ? fallback : point;
    }

    private static Vec3 groundPoint(ClientLevel level, GraveDragonEntity dragon) {
        Vec3 origin = dragon.position();
        HitResult hit = level.clip(new ClipContext(origin.add(0, .25, 0),
                new Vec3(origin.x, level.getMinBuildHeight(), origin.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : origin;
    }

    private static Vec3 anchor(GraveDragonEntity dragon, String action, long age, String bone, Vec3 offset) {
        Matrix4f matrix = boneMatrix(dragon, action, age, bone);
        return transformed(dragon, matrix, GraveDragonPose.bonePivot(bone).add(offset));
    }

    private static Matrix4f boneMatrix(GraveDragonEntity dragon, String action, long age, String bone) {
        if (poseDragon != dragon || poseAge != age || !action.equals(poseAction)) {
            poseDragon = dragon;
            poseAction = action;
            poseAge = age;
            poseFrame = dragon.poseAt(age / 20.0);
            poseToEntity = GraveDragonPose.modelToEntity(dragon.yBodyRot, dragon.bodyPitch(), dragon.getScale());
        }
        return new Matrix4f(poseToEntity).mul(poseFrame.matrices().get(bone));
    }

    private static Vec3 transformed(GraveDragonEntity dragon, Matrix4f matrix, Vec3 local) {
        Vector3f p = matrix.transformPosition(new Vector3f((float)local.x, (float)local.y, (float)local.z));
        return dragon.position().add(p.x, p.y, p.z);
    }

    private static boolean between(long age, int first, int last) {
        return age >= first && age <= last;
    }

    private static boolean inWindow(String action, int index, long age) {
        GraveDragonActions.Action definition = GraveDragonActions.get(action);
        if (definition == null || index >= definition.windows().size()) return false;
        GraveDragonActions.Window window = definition.windows().get(index);
        double seconds = age / 20.0;
        return seconds >= window.from() && seconds < window.to();
    }

    private static int eventTick(String action, int index) {
        return (int)Math.round(GraveDragonActions.event(action, index) * 20);
    }

    private static boolean at(long previous, long age, int tick) {
        return previous < tick && age >= tick && age - tick <= 1;
    }







    private static void fieldFire(ClientLevel level, GraveDragonEntity dragon,
                                  GraveDragonFireFieldPayload payload, long age, RandomSource random) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var cells = payload.field().cells();
        ParticleOptions flame = dragon.headBroken() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        for (int i = (int)(age % 3); i < cells.size(); i += 3) {
            Vec3 floor = cells.get(i);
            if (floor.distanceToSqr(camera) > 80 * 80) continue;
            if (floor.distanceToSqr(camera) > 40 * 40 && random.nextInt(4) != 0) continue;
            Particle particle = emit(level, flame,
                    floor.add((random.nextDouble() - .5) * .8, .1 + random.nextDouble() * .7,
                            (random.nextDouble() - .5) * .8),
                    new Vec3((random.nextDouble() - .5) * .05, .08 + random.nextDouble() * .04,
                            (random.nextDouble() - .5) * .05));
            if (particle != null) particle.setLifetime(6);
        }
    }
    private static Vec3 jitter(RandomSource random, double scale) {
        return new Vec3((random.nextDouble() - 0.5) * 2 * scale,
                (random.nextDouble() - 0.5) * 2 * scale,
                (random.nextDouble() - 0.5) * 2 * scale);
    }
    private static Particle emit(ClientLevel level, ParticleOptions type, Vec3 position, Vec3 velocity) {
        if (type == ParticleTypes.CLOUD && level.getRandom().nextInt(4) == 0) return null;
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(type,
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        // Dust/cloud constructors normalise and shrink the supplied velocity.
        // Set it on the created particle so the local effect really flows.
        if (particle != null) particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
        return particle;
    }
    private static boolean clearLine(ClientLevel level, GraveDragonEntity dragon, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon)).getType() == HitResult.Type.MISS;
    }
}
