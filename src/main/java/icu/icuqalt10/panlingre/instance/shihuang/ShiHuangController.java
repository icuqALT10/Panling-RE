package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.particles.ParticleTypes;
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
import java.util.UUID;

/** One controller per InstanceSession. Never look for the nearest dragon/coffin
 * in the world: different slots can run this sequence simultaneously.
 * Emperor combat consumes emperorEnhanced()/bindEmperor(); its additional
 * attacks and 帝皇歼灭斩 are deliberately not implemented by this scene controller.
 */
public final class ShiHuangController implements InstanceController {
    public enum Stage { INITIALIZING, DORMANT, DRAGON, DYING, TAKEOFF, TO_BARRIER, TO_COFFIN, ENTER_COFFIN, BARRIER_DEATH, EMPEROR_READY, EMPEROR_REVEAL, EMPEROR_COMBAT }
    public static final int SEAL_WINDOW_TICKS = 200;
    public static final int BARRIER_TICKS = 400;
    private static final int TAKEOFF_TICKS = 40, FLIGHT_TICKS = 80, ENTER_TICKS = 40, DEATH_TICKS = 60;
    private InstanceSession session;
    private ShiHuangScene scene;
    private GraveDragonEntity dragon;
    private LivingEntity emperor;
    private Stage stage = Stage.DORMANT;
    private long stageStart, escapeNotBefore;
    private boolean dyingFinished, emperorEnhanced;
    private Vec3 from, to;
    private float fromYaw, travelYaw;
    private final Map<BlockPos, BlockState> changedBlocks = new LinkedHashMap<>();
    private final Set<BlockPos> openedPassage = new LinkedHashSet<>();
    private final Set<Long> forcedChunks = new LinkedHashSet<>();
    private final Set<Long> loadingChunks = new LinkedHashSet<>();
    private ShiHuangGates gates;
    private ShiHuangMechanisms mechanisms;

    public Stage stage() { return stage; }
    public ShiHuangScene scene() { return scene; }
    public boolean emperorEnhanced() { return emperorEnhanced; }
    public UUID dragonId() { return dragon == null ? null : dragon.getUUID(); }
    public boolean barrierActive() { return mechanisms != null && mechanisms.barrierActive(); }

    /** Future emperor entity calls this once after spawning in this session.
     * The saved flag enables joint invulnerable-dragon skills and its 50% special.
     */
    public void bindEmperor(LivingEntity entity) {
        if (entity.level() != session.level() || entity.position().distanceToSqr(scene.coffinMouth()) > 160 * 160)
            throw new IllegalArgumentException("Emperor belongs to a different tomb slot");
        emperor = entity;
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
                if (!session.level().getForcedChunks().contains(key)) {
                    session.level().setChunkForced(x,z,true); forcedChunks.add(key);
                }
                session.level().getChunk(x,z);
            }
    }

    private void initializeLoadedScene() {
        var layout=ShiHuangLayout.load(session.level());
        gates=new ShiHuangGates(session.level(),scene,layout); gates.initialize();
        for (var entity : java.util.List.copyOf(session.level().getEntities(null,scene.bounds())))
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)
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
        openDoor();
        escapeNotBefore = now() + SEAL_WINDOW_TICKS;
        stage = Stage.DYING;
        stageStart = now();
        tell("dragon_retreat");
    }

    public boolean dragonDyingFinished(GraveDragonEntity entity) {
        if (entity != dragon || stage != Stage.DYING) return false;
        dyingFinished = true;
        dragon.beginSceneAnimation("escape_wait");
        return true;
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        if (stage==Stage.INITIALIZING) {
            if (loadingChunks.stream().allMatch(session.level()::areEntitiesLoaded)) initializeLoadedScene();
            return;
        }
        mechanisms.tick();
        if (session.isEnding()) return;
        if (stage==Stage.EMPEROR_READY) {
            if (scene.allInside(session.level(),scene.emperorArena())) {
                restorePassage(); gates.setOpen("bronze_left",false); gates.setOpen("bronze_right",false);
                mechanisms.resetSeals(); openCoffin(); stage=Stage.EMPEROR_REVEAL; stageStart=now();
                session.level().playSound(null,scene.coffinMouth().x,scene.coffinMouth().y,scene.coffinMouth().z,
                        SoundEvents.IRON_DOOR_OPEN,SoundSource.BLOCKS,5,.5F);
                publishEntrance(ShiHuangAwakenEvent.Phase.REVEAL);
            }
            return;
        }
        if (stage==Stage.EMPEROR_REVEAL) {
            if (now()-stageStart>=80) { stage=Stage.EMPEROR_COMBAT; publishEntrance(ShiHuangAwakenEvent.Phase.COMBAT_READY); tell("emperor_ready"); }
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
        if (stage == Stage.DYING) {
            if (dyingFinished && now() >= escapeNotBefore) {
                Vec3 start = dragon.position();
                travelYaw = yawTo(start, scene.barrierCenter());
                begin(Stage.TAKEOFF, "escape_takeoff", start.add(0, 10, 0));
            }
            return;
        }
        int duration = switch (stage) {
            case TAKEOFF -> TAKEOFF_TICKS;
            case TO_BARRIER, TO_COFFIN -> FLIGHT_TICKS;
            case ENTER_COFFIN -> ENTER_TICKS;
            case BARRIER_DEATH -> DEATH_TICKS;
            default -> 0;
        };
        if (duration == 0) return;
        double progress = Math.min(1, (now() - stageStart) / (double) duration);
        double eased = progress * progress * (3 - 2 * progress);
        float yaw = stage == Stage.TAKEOFF || stage == Stage.TO_BARRIER
                ? fromYaw + net.minecraft.util.Mth.wrapDegrees(travelYaw - fromYaw) * (float) eased : travelYaw;
        Vec3 desired = from.lerp(to, stage == Stage.TO_BARRIER || stage == Stage.TO_COFFIN ? progress : eased);
        if (stage == Stage.TO_BARRIER) {
            double firstFront = from.z + dragon.sceneGateOffset(0, fromYaw).z;
            double front = net.minecraft.util.Mth.lerp(progress, firstFront, scene.barrierCenter().z);
            desired = new Vec3(desired.x, desired.y, front - dragon.sceneGateOffset(progress * 4, yaw).z);
        }
        if (stage == Stage.ENTER_COFFIN) {
            // Keep the shrinking body above the lid until it fits the opening.
            // Anchor the head, not the root: root interpolation otherwise drops
            // the still-large feet through the coffin's front rim.
            Vec3 firstHead = from.add(dragon.sceneHeadOffset("escape_enter_coffin", 0, yaw));
            Vec3 head = firstHead.lerp(scene.coffinInterior(), eased);
            double descent = net.minecraft.util.Mth.clamp((progress - .75) / .25, 0, 1);
            descent = descent * descent * (3 - 2 * descent);
            head = new Vec3(head.x, net.minecraft.util.Mth.lerp(descent, firstHead.y, scene.coffinInterior().y), head.z);
            desired = head.subtract(dragon.sceneHeadOffset("escape_enter_coffin", progress * 2, yaw));
        }
        // Check every half-block of root travel against the actual posed OBBs.
        // A modified arena must not turn a fixed-time cinematic into wall clipping.
        if (!dragon.moveSceneTo(desired, yaw, scene.dragonSpawn().y) && stage != Stage.BARRIER_DEATH) {
            crash();
            return;
        }
        if (barrierActive() && (stage == Stage.TO_BARRIER || stage == Stage.TO_COFFIN)) {
            Vec3 p = scene.barrierCenter();
            var plane = new net.minecraft.world.phys.AABB(p.x - 20.5, p.y - 14.5, p.z - .15,
                    p.x + 20.5, p.y + 14.5, p.z + .15);
            for (var part : dragon.getWorldParts()) {
                if (part.getOrientedBox().intersects(plane)) { crash(); return; }
            }
        }
        if (progress < 1) return;
        switch (stage) {
            case TAKEOFF -> {
                travelYaw = 0;
                Vec3 head = dragon.sceneGateOffset(0, travelYaw);
                Vec3 contact = scene.barrierCenter().subtract(head);
                begin(Stage.TO_BARRIER, "escape_flight", contact);
            }
            case TO_BARRIER -> {
                if (barrierActive()) crash();
                else {
                    travelYaw = 0;
                    openPassage();
                    // The lid needs three seconds to open; begin while the dragon is still flying there.
                    openCoffin();
                    Vec3 head = dragon.sceneHeadOffset("escape_flight", 0, travelYaw);
                    // Bring the head above the coffin first. Scaling then pulls the
                    // entire dragon into the cavity, rather than dragging its tail through the lid.
                    begin(Stage.TO_COFFIN, "escape_flight", scene.coffinMouth().add(0, 5, -8).subtract(head));
                }
            }
            case TO_COFFIN -> {
                openCoffin();
                Vec3 lastHead = dragon.sceneHeadOffset("escape_enter_coffin", 2, travelYaw);
                begin(Stage.ENTER_COFFIN, "escape_enter_coffin", scene.coffinInterior().subtract(lastHead));
            }
            case ENTER_COFFIN -> {
                emperorEnhanced = true;
                stampEmperor();
                stage = Stage.EMPEROR_READY;
                InstanceManager.releaseEntity(dragon);
                dragon.discard();
                gates.setOpen("coffin",false);
                tell("dragon_absorbed");
            }
            case BARRIER_DEATH -> {
                stage = Stage.EMPEROR_READY;
                emperorEnhanced = false;
                stampEmperor();
                dragon.kill();
                openPassage();
                tell("dragon_sealed");
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
        dragon.beginSceneAnimation(animation);
    }

    private void crash() {
        Vec3 hit = dragon.getWorldParts()[11].getOrientedBox().center;
        session.level().playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 5, .75F);
        if (session.player() != null)
            session.level().sendParticles(session.player(), ParticleTypes.EXPLOSION, true, hit.x, hit.y, hit.z, 12, 3, 3, 1, .1);
        begin(Stage.BARRIER_DEATH, "escape_barrier_death",
                new Vec3(dragon.getX(), scene.dragonSpawn().y, dragon.getZ() - 3));
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
        if (entity == emperor) { tell("complete"); session.finish(InstanceResult.SUCCESS); return; }
        if (entity == dragon && stage != Stage.EMPEROR_READY) {
            emperorEnhanced = false;
            stampEmperor();
            stage = Stage.EMPEROR_READY;
            openDoor();
            openPassage();
        }
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        if (dragon != null) { InstanceManager.releaseEntity(dragon); dragon.discard(); }
        if (emperor != null) { InstanceManager.releaseEntity(emperor); emperor.discard(); }
        // Restore the actual placed blocks, including the door and coffin lid.
        changedBlocks.forEach((p, state) -> session.level().setBlock(p, state, 3));
        changedBlocks.clear();
        openedPassage.clear();
        resetSeals();
        if (gates!=null) gates.reset();
        for (long key : forcedChunks) session.level().setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);
        forcedChunks.clear(); loadingChunks.clear();
        emperorEnhanced = false;
        dyingFinished = false;
        stage = Stage.DORMANT;
        escapeNotBefore = stageStart = 0;
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
