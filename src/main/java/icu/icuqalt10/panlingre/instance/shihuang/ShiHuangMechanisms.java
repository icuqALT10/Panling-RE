package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.entity.TombTrapArrowEntity;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Earlier room puzzles remain independent of the dragon/emperor cinematic. */
public final class ShiHuangMechanisms {
    private final ServerLevel level;
    private final ShiHuangScene scene;
    private final ShiHuangLayout layout;
    private final ShiHuangGates gates;
    private final int[] seasons = new int[4];
    private final boolean[] buttonDown = new boolean[4], seals = new boolean[4];
    private final Map<UUID,Integer> routeProgress = new HashMap<>();
    private final Map<UUID,Integer> lastPlate = new HashMap<>();
    private final int[][] route;
    private long pathUntil, barrierUntil, nextVolley;
    private boolean pathFinished, dragonStarted;
    private static final int[][] EAST = {
        {-21,76,3419},{-21,76,3420},{-21,76,3421},{-21,76,3453},{-21,76,3454},{-21,76,3455},
        {-4,76,3452},{-4,76,3453},{-4,76,3454},{-4,76,3455},{-4,76,3456},
        {-4,76,3417},{-4,76,3418},{-4,76,3419},{13,76,3419},{13,76,3420},{13,76,3421},
        {13,76,3455},{13,76,3456},{13,76,3457}};
    private static final int[][] WEST = {{-13,76,3435},{4,76,3434},{4,76,3435},{4,76,3436},{4,76,3437},
        {21,76,3436},{21,76,3437},{21,76,3438},{21,76,3439}};

    public ShiHuangMechanisms(ServerLevel level, ShiHuangScene scene, ShiHuangLayout layout, ShiHuangGates gates) {
        this.level=level; this.scene=scene; this.layout=layout; this.gates=gates;
        route=layout.routes()[level.random.nextInt(layout.routes().length)];
    }
    private long now() { return level.getGameTime(); }
    private BlockPos pos(int[] p) { return scene.world(p[0],p[1],p[2]); }
    private BlockPos pathBulb() { return scene.world(0,72,3592); }
    public static void setLit(ServerLevel level, BlockPos pos, boolean lit) {
        var state=level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)!=lit)
            level.setBlock(pos,state.setValue(BlockStateProperties.LIT,lit),2);
    }
    private boolean lit(BlockPos p) {
        var s=level.getBlockState(p);
        return s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
    }
    private void clearBulb(BlockPos p) {
        setLit(level,p,false);
        for (BlockPos q : List.of(p,p.above())) {
            var s=level.getBlockState(q);
            if (s.hasProperty(BlockStateProperties.POWERED)) level.setBlock(q,s.setValue(BlockStateProperties.POWERED,false),2);
        }
    }
    public void initialize() {
        for (BlockPos p : List.of(scene.world(-24,72,3327),scene.world(12,72,3345),scene.world(17,74,3474),pathBulb())) clearBulb(p);
        for (int[] p : layout.seasonLamps()) setLit(level,pos(p),false);
        for (var season : layout.seasons()) {
            var p=pos(season.button()); var s=level.getBlockState(p);
            if (s.hasProperty(BlockStateProperties.POWERED)) level.setBlock(p,s.setValue(BlockStateProperties.POWERED,false),2);
        }
        pathLights(false); resetSeals(); nextVolley=now()+100;
    }
    public void tick() {
        var players=scene.players(level);
        gates.setOpen("maze",lit(scene.world(-24,72,3327)) && lit(scene.world(12,72,3345)));
        gates.setOpen("parkour",lit(scene.world(17,74,3474)));
        for (var p : players)
            if (scene.region(-25,71,3404,25,72,3478).contains(p.position()))
                penalty(p,20,scene.worldPoint(-16.5,74,3407.5),"abyss");
        if (now()>=nextVolley) {
            nextVolley=now()+100;
            if (players.stream().anyMatch(p -> scene.region(-26,73,3400,26,92,3479).contains(p.position()))) {
                volley(EAST,1); volley(WEST,-1);
            }
        }
        tickSeasons(); tickPath(players); tickSeals(); gates.tick();
    }
    private void volley(int[][] positions, int dx) {
        for (int[] p : positions) {
            BlockPos dispenser=pos(p);
            TombTrapArrowEntity arrow=ModEntities.TOMB_TRAP_ARROW.get().create(level);
            arrow.setPos(Vec3.atCenterOf(dispenser).add(dx*.55,0,0));
            arrow.shoot(dx,0,0,1.6F,0);
            level.addFreshEntity(arrow);
            level.playSound(null,dispenser,SoundEvents.DISPENSER_DISPENSE,SoundSource.BLOCKS,1,1);
        }
    }
    private void tickSeasons() {
        boolean correct=true;
        for (int i=0;i<4;i++) {
            var season=layout.seasons().get(i); var state=level.getBlockState(pos(season.button()));
            boolean down=state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED);
            if (down && !buttonDown[i]) seasons[i]=seasons[i]%4+1;
            buttonDown[i]=down;
            for (int j=0;j<season.lamps().length;j++) setLit(level,pos(season.lamps()[j]),j<seasons[i]);
            boolean match=seasons[i]==season.target(); correct &= match;
            for (int[] p : season.indicator()) setLit(level,pos(p),match);
        }
        gates.setOpen("seasons",correct);
    }
    private void pathLights(boolean enabled) {
        boolean[] lights=new boolean[49];
        if (enabled) for (int[] cell : route) lights[cell[1]*7+cell[0]]=true;
        for (int i=0;i<49;i++) {
            setLit(level,pos(layout.floor()[i]),lights[i]); setLit(level,pos(layout.ceiling()[i]),lights[i]);
        }
    }
    private void tickPath(List<ServerPlayer> players) {
        if (!pathFinished && pathUntil==0 && lit(pathBulb())) {
            pathUntil=now()+300; routeProgress.clear(); lastPlate.clear();
        }
        if (pathUntil>0 && now()>=pathUntil && !pathFinished) {
            for (var p : players)
                if (scene.region(-26,71,3575,26,92,3655).contains(p.position()))
                    penalty(p,30,scene.worldPoint(.5,72,3587.5),"path_timeout");
            endAttempt();
        }
        if (pathUntil>0 && !pathFinished) for (var player : players) {
            if (!player.onGround()) continue;
            int cell=-1;
            for (int i=0;i<49;i++) {
                BlockPos p=pos(layout.plates()[i]); Vec3 v=player.position();
                if (v.x>=p.getX() && v.x<p.getX()+1 && v.z>=p.getZ() && v.z<p.getZ()+1
                        && v.y>=p.getY() && v.y<p.getY()+.4) { cell=i; break; }
            }
            // Gaps and airborne travel do not count as wrong steps.
            if (cell<0 || lastPlate.getOrDefault(player.getUUID(),-1)==cell) continue;
            lastPlate.put(player.getUUID(),cell);
            int progress=routeProgress.getOrDefault(player.getUUID(),0);
            int[] expected=route[progress];
            if (cell!=expected[1]*7+expected[0]) {
                penalty(player,30,scene.worldPoint(.5,72,3587.5),"path_wrong");
                endAttempt(); break;
            }
            routeProgress.put(player.getUUID(),progress+1);
            if (progress+1==route.length) { pathFinished=true; pathUntil=0; break; }
        }
        boolean active=pathFinished || pathUntil>0;
        // Turning the bulb off cancels an unfinished attempt.
        if (pathUntil>0 && !lit(pathBulb())) { endAttempt(); active=false; }
        pathLights(active); gates.setOpen("path_entry",active);
        gates.setOpen("path_exit",pathFinished); gates.setOpen("dragon_entry",pathFinished && !dragonStarted);
    }
    private void endAttempt() { pathUntil=0; routeProgress.clear(); lastPlate.clear(); clearBulb(pathBulb()); }
    public void dragonStarted() { dragonStarted=true; gates.setOpen("dragon_entry",false); }
    public boolean barrierActive() { return now()<barrierUntil; }
    public long barrierUntil() { return barrierUntil; }
    private void tickSeals() {
        if (barrierUntil!=0 && now()>=barrierUntil) resetSeals();
        var bulbs=scene.sealBulbs(); var beacons=scene.beacons(); int count=0;
        for (int i=0;i<4;i++) {
            if (lit(bulbs[i])) seals[i]=true;
            if (seals[i]) { count++; setLit(level,bulbs[i],true); }
            var base=seals[i] ? net.minecraft.world.level.block.Blocks.GOLD_BLOCK : net.minecraft.world.level.block.Blocks.NETHER_BRICKS;
            if (!level.getBlockState(beacons[i].below()).is(base)) level.setBlock(beacons[i].below(),base.defaultBlockState(),3);
        }
        if (count==4 && barrierUntil==0) {
            barrierUntil=now()+400;
            for (var p : scene.players(level)) p.sendSystemMessage(Component.translatable("instance.panlingre.shihuang.seal_on"));
            var p=scene.barrierCenter();
            level.playSound(null,p.x,p.y,p.z,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,5,.8F);
        }
        gates.seal(barrierActive());
    }
    public void resetSeals() {
        barrierUntil=0; java.util.Arrays.fill(seals,false);
        for (var p : scene.sealBulbs()) clearBulb(p);
        for (var p : scene.beacons()) level.setBlock(p.below(),net.minecraft.world.level.block.Blocks.NETHER_BRICKS.defaultBlockState(),3);
        gates.seal(false);
    }
    private void penalty(ServerPlayer player, float amount, Vec3 target, String message) {
        // Array penalties are direct HP loss, independent of armour and shields.
        float remaining=player.getHealth()-amount;
        if (remaining>0) player.setHealth(remaining);
        else player.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);
        if (player.isAlive()) {
            player.teleportTo(level,target.x,target.y,target.z,player.getYRot(),player.getXRot());
            player.setDeltaMovement(Vec3.ZERO); player.fallDistance=0;
        }
        player.sendSystemMessage(Component.translatable("instance.panlingre.shihuang."+message));
    }
}
