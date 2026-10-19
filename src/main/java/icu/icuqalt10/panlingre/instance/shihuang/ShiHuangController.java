package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonPose;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/** One controller per InstanceSession. Never look for the nearest dragon/coffin
 * in the world: different slots can run this sequence simultaneously.
 * ShiHuangEntity owns combat and the death timeline; this controller owns
 * the entrance and instance cleanup.
 */
public final class ShiHuangController implements InstanceController {
    public enum Stage { INITIALIZING, DORMANT, DRAGON, DYING, TAKEOFF, FLIGHT_PREP, TO_BARRIER, TO_COFFIN, ENTER_COFFIN, BARRIER_DEATH, BARRIER_SMOKE, EMPEROR_READY, EMPEROR_REVEAL, EMPEROR_COMBAT }
    public static final int BARRIER_TICKS = 60;
    private static final double ESCAPE_SPEED = 2; // 40 blocks/second at 20 TPS.
    private static final int ENTER_TICKS = 40, SMOKE_TICKS = 40;
    private int flightTicks;
    private Vec3 flightFromHead;
    private InstanceSession session;
    private ShiHuangScene scene;
    private GraveDragonEntity dragon;
    private ShiHuangEntity emperor;
    private Stage stage = Stage.DORMANT;
    private long stageStart;
    private boolean emperorEnhanced;
    private boolean foreheadHit;
    private Vec3 from, to;
    private float fromYaw, travelYaw;
    private float retreatFromYaw, retreatToYaw;
    private int retreatTurnTicks;
    private Vec3 retreatPivot;
    private java.util.List<Vec3> deathSmokePoints=java.util.List.of();
    private long deathSmokeAt;
    private final Map<BlockPos, BlockState> changedBlocks = new LinkedHashMap<>();
    private final Set<BlockPos> openedPassage = new LinkedHashSet<>();
    private final Set<Long> forcedChunks = new LinkedHashSet<>();
    private final Set<Long> loadingChunks = new LinkedHashSet<>();
    private ShiHuangGates gates;
    private ShiHuangMechanisms mechanisms;
    private final ShiHuangBossSounds bossSounds = new ShiHuangBossSounds();
    private static final class PlatformTime { int above, below; }
    private final Map<java.util.UUID, PlatformTime> platformTime = new LinkedHashMap<>();

    public ShiHuangScene scene() { return scene; }
    void tickPlatformLimit(net.minecraft.server.level.ServerLevel level) {
        if (stage != Stage.DRAGON) { platformTime.clear(); return; }
        if (dragon != null && dragon.isForcingHalfHealthPhase()) { platformTime.clear(); return; }
        var present = new java.util.HashSet<java.util.UUID>();
        for (var player : scene.players(level)) {
            if (player.isCreative() || !scene.dragonRoom().contains(player.position())) continue;
            present.add(player.getUUID());
            var support = level.clip(new net.minecraft.world.level.ClipContext(player.position().add(0, .1, 0),
                    new Vec3(player.getX(), scene.dragonSpawn().y - .1, player.getZ()),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (support.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                    || support.getLocation().y < scene.dragonSpawn().y + 1.5) {
                var time = platformTime.get(player.getUUID());
                if (time != null && ++time.below >= 100) platformTime.remove(player.getUUID());
                continue;
            }
            var time = platformTime.computeIfAbsent(player.getUUID(), ignored -> new PlatformTime());
            time.below = 0;
            time.above = Math.min(60, time.above + 1);
            if (time.above >= 60) {
                Vec3 center = scene.dragonRoom().getCenter();
                player.teleportTo(level, center.x, scene.dragonSpawn().y, center.z, player.getYRot(), player.getXRot());
                player.sendSystemMessage(Component.literal("§7你被高台上的阵法丢回了场地中央……"));
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0;
            }
        }
        platformTime.keySet().retainAll(present);
    }
    public boolean barrierActive() { return mechanisms != null && mechanisms.barrierActive(); }
    public void playArenaSound(net.minecraft.sounds.SoundEvent sound, SoundSource source, float volume, float pitch) {
        bossSounds.playInArena(scene,session.level(),sound,source,volume,pitch);
    }

    /** Bind the emperor spawned by this session.
     * The saved flag enables joint invulnerable-dragon skills and its 50% special.
     */
    public void bindEmperor(ShiHuangEntity entity) {
        if (entity.level() != session.level() || entity.position().distanceToSqr(scene.coffinMouth()) > 160 * 160)
            throw new IllegalArgumentException("Emperor belongs to a different tomb slot");
        emperor = entity;
        emperor.setEnhanced(emperorEnhanced);
        InstanceManager.ownEntity(session, entity);
        stampEmperor();
    }

    private void stampEmperor() {
        if (emperor == null) return;
        CompoundTag data = emperor.getPersistentData();
        data.putBoolean("ShiHuangDragonEnhanced", emperorEnhanced);
        data.putString("ShiHuangInstance", session.instanceId().toString());
        data.putInt("ShiHuangSlot", session.slot());
        data.putUUID("ShiHuangOwner", session.playerId());
    }

    @Override
    public void start(InstanceSession session) {
        this.session = session;
        scene = ShiHuangScene.forSession(session);
        stage=Stage.INITIALIZING;
        var bounds=scene.bounds();
        for (int x=(int)Math.floor(bounds.minX)>>4;x<=((int)Math.floor(bounds.maxX-1)>>4);x++)
            for (int z=(int)Math.floor(bounds.minZ)>>4;z<=((int)Math.floor(bounds.maxZ-1)>>4);z++) {
                long key=ChunkPos.asLong(x,z); loadingChunks.add(key);

            }
    }

    private void initializeLoadedScene() {
        var layout=ShiHuangLayout.load(session.level());
        gates=new ShiHuangGates(session.level(),scene,layout); gates.initialize();
        for (var entity : java.util.List.copyOf(session.level().getEntities(null,scene.bounds())))
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)
                    && !(entity instanceof net.minecraft.world.entity.npc.Villager && entity.getTags().contains("shi_huang_ling"))
                    && (!(entity instanceof icu.icuqalt10.panlingre.entity.TombDisplayEntity display) || !gates.owns(display)))
                entity.discard();
        mechanisms=new ShiHuangMechanisms(session.level(),scene,layout,gates); mechanisms.initialize();
        // Keep remote mechanisms and both boss rooms active, release temporary whole-template tickets.
        Set<Long> keep=new LinkedHashSet<>();
        for (var gate : layout.gates()) keep.add(scene.world(gate.pos()[0],gate.pos()[1],gate.pos()[2]).asLong());
        for (int[] p : layout.seasonLamps()) keep.add(scene.world(p[0],p[1],p[2]).asLong());
        for (var s : layout.seasons()) keep.add(scene.world(s.button()[0],s.button()[1],s.button()[2]).asLong());
        for (int[] p : layout.floor()) keep.add(scene.world(p[0],p[1],p[2]).asLong());
        for (BlockPos p : java.util.List.of(scene.world(-24,72,3327),scene.world(12,72,3345),scene.world(17,74,3474),scene.world(0,72,3592))) keep.add(p.asLong());
        Set<Long> retained=new LinkedHashSet<>();
        for (long p : keep) retained.add(new ChunkPos(BlockPos.of(p)).toLong());
        for (var box : java.util.List.of(scene.region(-26,71,3400,26,94,3483),scene.region(-80,60,3657,80,165,3980)))
            for (int x=(int)Math.floor(box.minX)>>4;x<=((int)Math.floor(box.maxX-1)>>4);x++)
                for (int z=(int)Math.floor(box.minZ)>>4;z<=((int)Math.floor(box.maxZ-1)>>4);z++) retained.add(ChunkPos.asLong(x,z));
        for (long key : java.util.List.copyOf(forcedChunks)) if (!retained.contains(key)) {
            session.level().setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false); forcedChunks.remove(key);
        }
        loadingChunks.clear();
        dragon = ModEntities.GRAVE_DRAGON.get().create(session.level());
        if (dragon == null) { session.finish(InstanceResult.FAILURE); return; }
        dragon.moveTo(scene.dragonSpawn(), 180, 0);
        dragon.setPersistenceRequired();
        dragon.setArenaAudio();
        dragon.setNoAi(true);
        dragon.setInvulnerable(true);
        dragon.getPersistentData().putInt("ShiHuangSlot", session.slot());
        dragon.getPersistentData().putUUID("ShiHuangOwner", session.playerId());
        session.level().addFreshEntity(dragon);
        InstanceManager.ownEntity(session, dragon);
        stage=Stage.DORMANT;
    }

    /** Called exactly once when this dragon reaches its 5% lock. */
    public void dragonRetreating(GraveDragonEntity entity) {
        if (entity != dragon || stage != Stage.DRAGON) return;
        retreatFromYaw=dragon.yBodyRot;
        retreatPivot=dragon.position().add(dragon.sceneTurnPivotOffset(retreatFromYaw));
        retreatToYaw=yawTo(retreatPivot,scene.barrierCenter());
        retreatTurnTicks=GraveDragonEntity.turnTicks(dragon.position().subtract(scene.barrierCenter()).horizontalDistance());
        openDoor();
        stage = Stage.DYING;
        stageStart = now();
    }

    public boolean dragonDyingFinished(GraveDragonEntity entity) {
        if (entity != dragon || stage != Stage.DYING) return false;
        Vec3 start = dragon.position();
        travelYaw = 0;
        // Leave room for the extended tail before unfolding into the gate-facing fly pose.
        Vec3 min=scene.worldPoint(-28,72,3710),max=scene.worldPoint(28,72,3760);
        Vec3 launch=new Vec3(net.minecraft.util.Mth.clamp(start.x,min.x,max.x),start.y+12,
                net.minecraft.util.Mth.clamp(start.z,min.z,max.z));
        begin(Stage.TAKEOFF, "takeoff", launch);
        return true;
    }

    public float retreatYaw(double seconds) {
        double progress=net.minecraft.util.Mth.clamp(seconds*20/retreatTurnTicks,0,1);
        progress=progress*progress*(3-2*progress);
        return retreatFromYaw+net.minecraft.util.Mth.wrapDegrees(retreatToYaw-retreatFromYaw)*(float)progress;
    }

    /** Turn around the rear trunk, so the long tail does not sweep through the room's outer wall. */
    public float turnRetreat(double seconds) {
        float yaw=retreatYaw(seconds);
        Vec3 root=retreatPivot.subtract(dragon.sceneTurnPivotOffset(yaw));
        // The retreat pivot was captured before landing; it must not restore that airborne height.
        dragon.moveSceneTo(new Vec3(root.x,scene.dragonSpawn().y,root.z),yaw,scene.dragonSpawn().y);
        return dragon.getYRot();
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        if (stage==Stage.INITIALIZING) {
            // Admit four chunks per tick instead of synchronously loading the entire 763-block tomb.
            int budget=4;
            for(long key:loadingChunks) {
                if(!session.level().getForcedChunks().contains(key)) {
                    session.level().setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),true);
                    forcedChunks.add(key);
                    if(--budget==0)break;
                }
            }
            if (loadingChunks.stream().allMatch(session.level()::areEntitiesLoaded)) initializeLoadedScene();
            return;
        }
        mechanisms.tick();
        if(!deathSmokePoints.isEmpty() && now()>=deathSmokeAt) {
            if(now()-deathSmokeAt>=40) deathSmokePoints=java.util.List.of();
            else if((now()-deathSmokeAt)%4==0) for(var player:session.level().players()) {
                if(!scene.bounds().contains(player.position())) continue;
                for(Vec3 p:deathSmokePoints)
                    session.level().sendParticles(player,net.minecraft.core.particles.ParticleTypes.CLOUD,true,
                            p.x,p.y,p.z,16,1.2,.8,1.2,.06);
            }
        }
        if (dragon != null) bossSounds.tick(dragon,scene);
        if (session.isEnding()) return;
        if (stage==Stage.EMPEROR_READY) {
            if (scene.allInsideEmperorArena(session.level())) {
                restorePassage(); gates.setOpen("bronze_left",false); gates.setOpen("bronze_right",false);
                for(BlockPos p:BlockPos.betweenClosed(scene.world(-4,72,3838),scene.world(4,78,3838))) {
                    changedBlocks.putIfAbsent(p.immutable(),session.level().getBlockState(p));
                    session.level().setBlock(p,Blocks.BARRIER.defaultBlockState(),2);
                }
                mechanisms.resetSeals(); openCoffin(); stage=Stage.EMPEROR_REVEAL; stageStart=now();
                playArenaSound(SoundEvents.IRON_DOOR_OPEN,SoundSource.BLOCKS,5,.5F);
                emperor=ModEntities.SHI_HUANG.get().create(session.level());
                if(emperor==null){session.finish(InstanceResult.FAILURE);return;}
                emperor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(ShiHuangEntity.HEALTH_PER_PLAYER*Math.max(1,scene.players(session.level()).size()));
                emperor.setHealth(emperor.getMaxHealth());
                emperor.moveTo(scene.coffinInterior(),180,0);emperor.yBodyRot=emperor.yHeadRot=180;
                emperor.beginEntrance();
                session.level().addFreshEntity(emperor);bindEmperor(emperor);
                publishEntrance(ShiHuangAwakenEvent.Phase.REVEAL);
            }
            return;
        }
        if (stage==Stage.EMPEROR_REVEAL) {
            if(emperor==null||emperor.isRemoved()||emperor.dying())return;
            if(emperor.tickEntrance(scene.emperorCombatPosition())){
                stage=Stage.EMPEROR_COMBAT;publishEntrance(ShiHuangAwakenEvent.Phase.COMBAT_READY);}
            return;
        }
        if (dragon == null || dragon.isRemoved() || dragon.isDeadOrDying()) return;
        if (stage == Stage.DORMANT) {
            if (scene.allInside(session.level(),scene.dragonRoom())) {
                stage = Stage.DRAGON;
                mechanisms.dragonStarted();
                dragon.setInvulnerable(false);
                dragon.setNoAi(false);
            }
            return;
        }
        tickPlatformLimit(session.level());
        if (stage == Stage.DYING) return;
        if (stage==Stage.BARRIER_DEATH || stage==Stage.BARRIER_SMOKE) { tickBarrierFall(); return; }
        int duration = switch (stage) {
            case TAKEOFF -> Math.max(retreatTurnTicks,(int)Math.round(GraveDragonPose.duration("takeoff")*20));
            case FLIGHT_PREP -> (int)Math.round(GraveDragonPose.duration("idle_air_to_fly")*10);
            case TO_BARRIER, TO_COFFIN -> flightTicks;
            case ENTER_COFFIN -> ENTER_TICKS;
            default -> 0;
        };
        if (duration == 0) return;
        double progress = Math.min(1, (now() - stageStart) / (double) duration);
        double eased = progress * progress * (3 - 2 * progress);
        double turnProgress=stage==Stage.TAKEOFF
                ? Math.min(1,(now()-stageStart)/(double)retreatTurnTicks) : progress;
        double turnEase=turnProgress*turnProgress*(3-2*turnProgress);
        float yaw = stage == Stage.TAKEOFF || stage == Stage.FLIGHT_PREP || stage == Stage.TO_BARRIER
                ? fromYaw + net.minecraft.util.Mth.wrapDegrees(travelYaw - fromYaw) * (float) turnEase : travelYaw;
        Vec3 desired = from.lerp(to, stage == Stage.TO_BARRIER || stage == Stage.TO_COFFIN ? progress : eased);
        if (stage==Stage.TO_BARRIER || stage==Stage.TO_COFFIN) {
            double seconds=dragon.animationSeconds(0);
            Vec3 headOffset=dragon.sceneHeadOffset("fly",seconds,yaw);
            Vec3 targetHead=to;
            if(stage==Stage.TO_BARRIER) targetHead=to.add(0,0,-(dragon.sceneGateOffset(seconds,yaw).z-headOffset.z));
            desired=flightFromHead.lerp(targetHead,progress).subtract(headOffset);
        }
        if (stage==Stage.ENTER_COFFIN) desired=to;
        // Check every half-block of root travel against the actual posed OBBs.
        // A modified arena must not turn a fixed-time cinematic into wall clipping.
        var absorption=stage==Stage.ENTER_COFFIN || stage==Stage.TO_COFFIN
                ? scene.region(-7,79,3953,7,87,3974) : null;
        if (!dragon.moveSceneTo(desired, yaw, scene.dragonSpawn().y,absorption)) return;
        if (barrierActive() && (stage == Stage.TO_BARRIER || stage == Stage.TO_COFFIN)) {
            Vec3 p = scene.barrierCenter();
            var plane = new net.minecraft.world.phys.AABB(p.x - 20.5, p.y - 14.5, p.z - .15,
                    p.x + 20.5, p.y + 14.5, p.z + .15);
            for (int index : new int[]{11,12,78}) {
                var part=dragon.getWorldParts()[index];
                if (part.getOrientedBox().intersects(plane)) { crash(); return; }
            }
        }
        if (progress < 1) return;
        switch (stage) {
            case TAKEOFF -> {
                travelYaw=0;
                Vec3 head=dragon.sceneHeadOffset("fly",0,travelYaw);
                begin(Stage.FLIGHT_PREP,"idle_air_to_fly",new Vec3(dragon.getX(),scene.barrierCenter().y-head.y,dragon.getZ()));
            }
            case FLIGHT_PREP -> beginFlight(Stage.TO_BARRIER,scene.barrierCenter());
            case TO_BARRIER -> {
                if (barrierActive()) crash();
                else {
                    openPassage();
                    Vec3 entry=coffinEntryOrigin();
                    beginFlight(Stage.TO_COFFIN,entry.add(dragon.sceneHeadOffset("escape_enter_coffin",0,travelYaw)));
                }
            }
            case TO_COFFIN -> {
                begin(Stage.ENTER_COFFIN,"escape_enter_coffin",coffinEntryOrigin());
                var bulb=session.level().getBlockState(scene.coffinBulb());
                change(scene.coffinBulb(),bulb.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true));
            }
            case ENTER_COFFIN -> {
                emperorEnhanced = true;
                stampEmperor();
                stage = Stage.EMPEROR_READY;
                InstanceManager.releaseEntity(dragon);
                dragon.discard();
            }
            default -> { }
        }
    }

    private void begin(Stage next, String animation, Vec3 destination) {
        stage = next;
        stageStart = now();
        from = dragon.position();
        to = destination;
        fromYaw = dragon.getYRot();
        if(next==Stage.TAKEOFF) dragon.beginSceneTakeoff(Math.max(retreatTurnTicks,(int)Math.round(GraveDragonPose.duration(animation)*20)));
        else dragon.beginSceneAnimation(animation);
    }

    private Vec3 coffinEntryOrigin() {
        // At 0.2 s half of the head has crossed the copper bulb's front face,
        // still at scale 1. Later segments use that same absorption plane.
        return scene.coffinEntry()
                .subtract(dragon.sceneHeadOffset("escape_enter_coffin",.2,travelYaw));
    }

    private void beginFlight(Stage next,Vec3 targetHead) {
        Vec3 start=dragon.getWorldParts()[11].getOrientedBox().center;
        if (!dragon.animation().equals("fly")) dragon.beginSceneAnimation("fly");
        stage=next; stageStart=now(); from=dragon.position(); fromYaw=dragon.getYRot();
        flightFromHead=start; to=targetHead;
        Vec3 arrival=targetHead;
        if (next==Stage.TO_BARRIER) arrival=arrival.add(0,0,-(dragon.sceneGateOffset(0,travelYaw).z-dragon.sceneHeadOffset("fly",0,travelYaw).z));
        flightTicks=Math.max(1,(int)Math.ceil(start.distanceTo(arrival)/ESCAPE_SPEED));
    }

    private void crash() {
        mechanisms.hitSeal();
        foreheadHit=false;
        Vec3 head=dragon.getWorldParts()[11].getOrientedBox().center;
        icu.icuqalt10.panlingre.network.TombLightPayload.burst(head,0xFFF0CF,2.5F,10).send(session.level(),128);
        double angle=Math.toRadians(travelYaw);
        begin(Stage.BARRIER_DEATH,"escape_wall_impact",dragon.position().add(10*Math.sin(angle),0,-10*Math.cos(angle)));
    }

    private void tickBarrierFall() {
        if(stage==Stage.BARRIER_SMOKE) {
            if(now()-stageStart<SMOKE_TICKS) return;
            stage=Stage.EMPEROR_READY; emperorEnhanced=false; stampEmperor();
            deathSmokePoints=java.util.stream.IntStream.of(0,2,4,6,8,11,13,15,17,19)
                    .mapToObj(i->dragon.getWorldParts()[i].getOrientedBox().center).toList();
            deathSmokeAt=now()+2; // Entity removal reaches clients before the first light packet.
            InstanceManager.releaseEntity(dragon); dragon.discard();
            openPassage();
            return;
        }
        double seconds=dragon.animationSeconds(0);
        double floor=scene.dragonSpawn().y;
        double lowest=dragon.sceneLowestOffset();
        // Keep the mouth/skull against the seal during the short downward slide.
        if (seconds<=.25) {
            double front=dragon.sceneImpactFrontOffset();
            if (!dragon.moveSceneTo(new Vec3(from.x,from.y-.7*(seconds/.25),scene.barrierCenter().z-front),travelYaw,floor)) return;
            if (seconds<.25) return;
        }
        if (!foreheadHit) {
            foreheadHit=true;
            from=dragon.position();
            double angle=Math.toRadians(travelYaw);
            to=from.add(10*Math.sin(angle),0,-10*Math.cos(angle));
            Vec3 head=dragon.getWorldParts()[11].getOrientedBox().center;
            icu.icuqalt10.panlingre.network.TombLightPayload.burst(new Vec3(head.x,head.y,scene.barrierCenter().z),0xFFF0CF,3,10).send(session.level(),128);
        }
        // Two-second cry, twice-speed fall, then the original weakening attempts.
        double drop=net.minecraft.util.Mth.clamp((seconds-2.25)/.325,0,1);
        double ground=floor-lowest;
        double y=Math.max(ground,from.y+(ground-from.y)*drop*drop);
        double recoil=net.minecraft.util.Mth.clamp((seconds-.25)/.5,0,1);
        Vec3 retreat=from.lerp(to,1-(1-recoil)*(1-recoil));
        if (!dragon.moveSceneTo(new Vec3(retreat.x,y,retreat.z),travelYaw,floor)) return;
        if(drop>=1 && dragon.getY()+lowest<=floor+.06 && dragon.sceneLandingTick()==Long.MIN_VALUE) {
            dragon.landScene();
            playArenaSound(SoundEvents.GENERIC_EXPLODE.value(),SoundSource.HOSTILE,5,.65F);
        }
        if(seconds>=GraveDragonPose.duration("escape_wall_impact")) {
            stage=Stage.BARRIER_SMOKE; stageStart=now();
        }
    }

    public void resetSeals() {
        if (mechanisms!=null) mechanisms.resetSeals();
    }

    private void openDoor() {
        gates.setOpen("bronze_left",true); gates.setOpen("bronze_right",true);
    }

    private void openCoffin() {
        gates.setOpen("coffin",true);
    }

    /** The template also encloses the emperor arena in invisible arena walls.
     * These are not the timed four-beacon seal. Open only its two flight portals.
     */
    private void openPassage() {
        for (int z : new int[]{619, 717}) {
            for (BlockPos p : BlockPos.betweenClosed(scene.block(68, 12, z), scene.block(108, 44, z))) {
                if (!session.level().getBlockState(p).is(Blocks.BARRIER)) continue;
                openedPassage.add(p.immutable());
                change(p, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private void restorePassage() {
        for (BlockPos p : openedPassage) session.level().setBlock(p, changedBlocks.get(p), 3);
        openedPassage.clear();
    }

    private void change(BlockPos pos, BlockState state) {
        var level = session.level();
        if (level.getBlockState(pos) == state) return;
        changedBlocks.putIfAbsent(pos.immutable(), level.getBlockState(pos));
        level.setBlock(pos, state, 3);
    }

    @Override
    public void onEntityDeath(InstanceSession session, LivingEntity entity) {
        if (entity == emperor) return; // ShiHuangEntity owns the complete death timeline.
        if (entity == dragon && stage != Stage.EMPEROR_READY) {
            emperorEnhanced = false;
            stampEmperor();
            stage = Stage.EMPEROR_READY;
            openDoor();
            openPassage();
        }
    }

    public void completeEmperor(ShiHuangEntity entity) {
        if(entity!=emperor||session.isEnding()||!entity.rewardReady())return;
        entity.releaseRemnant();
        session.finish(InstanceResult.SUCCESS);
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        bossSounds.stop();
        if (dragon != null) { InstanceManager.releaseEntity(dragon); dragon.discard(); }
        if (emperor != null) { InstanceManager.releaseEntity(emperor); emperor.discard(); }
        // Restore the actual placed blocks, including the door and coffin lid.
        changedBlocks.forEach((p, state) -> session.level().setBlock(p, state, 3));
        changedBlocks.clear();
        openedPassage.clear();
        resetSeals();
        if (mechanisms!=null) mechanisms.stop();
        if (gates!=null) gates.reset();
        for (long key : forcedChunks) session.level().setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);
        forcedChunks.clear(); loadingChunks.clear();
        emperorEnhanced = false;
        stage = Stage.DORMANT;
        stageStart = 0;
    }

    private long now() { return session.level().getGameTime(); }
    private void tell(String key) {
        for (var p : scene.players(session.level())) p.sendSystemMessage(Component.translatable("instance.panlingre.shihuang."+key));
    }
    private void publishEntrance(ShiHuangAwakenEvent.Phase phase) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ShiHuangAwakenEvent(session,phase,emperorEnhanced,
                scene.coffinInterior(),scene.emperorCombatPosition()));
        stampEmperor();
    }
    private static float yawTo(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }
}
