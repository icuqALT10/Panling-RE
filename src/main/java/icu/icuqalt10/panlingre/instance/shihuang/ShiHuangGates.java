package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.entity.TombDisplayEntity;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nine moving assemblies and one seal; no entity per block. */
public final class ShiHuangGates {
    private final ServerLevel level;
    private final ShiHuangScene scene;
    private final ShiHuangLayout layout;
    private final Map<String,TombDisplayEntity> displays = new LinkedHashMap<>();
    private final Map<String,Boolean> barrierClosed = new LinkedHashMap<>();
    private final Map<String,Integer> ironCleared = new LinkedHashMap<>();
    private TombDisplayEntity seal;
    private final java.util.Set<BlockPos> coffinBlocks=new java.util.HashSet<>();
    private int coffinPose=-1;

    public ShiHuangGates(ServerLevel level, ShiHuangScene scene, ShiHuangLayout layout) {
        this.level=level; this.scene=scene; this.layout=layout;
    }
    public static void prepare(ServerLevel level, BlockPos origin) {
        new ShiHuangGates(level,new ShiHuangScene(origin),ShiHuangLayout.load(level)).initialize();
    }
    private String tag(String id) { return "panlingre.tomb."+scene.origin().asLong()+"."+id; }
    private TombDisplayEntity display(String id, int kind, Vec3 position) {
        TombDisplayEntity found=null;
        for (var entity : level.getEntitiesOfClass(TombDisplayEntity.class,scene.bounds(),e -> e.getTags().contains(tag(id)))) {
            if (found == null) found=entity; else entity.discard();
        }
        if (found == null) {
            found=ModEntities.TOMB_DISPLAY.get().create(level);
            found.addTag(tag(id)); found.setKind(kind); found.setPos(position);
            level.addFreshEntity(found);
        }
        found.setPos(position); found.reset();
        return found;
    }
    public void initialize() {
        for (var gate : layout.gates()) {
            if (gate.kind()==5) {
                for (BlockPos b : BlockPos.betweenClosed(scene.world(-4,83,3955),scene.world(4,85,3973)))
                    if (level.getBlockState(b).is(Blocks.BARRIER)) level.setBlock(b,Blocks.AIR.defaultBlockState(),2);
                for (int[] cell : gate.decoration()) {
                    BlockPos b=scene.world(14-cell[0],81-cell[1],3956+cell[2]);
                    if (level.getBlockState(b).is(Blocks.BARRIER)) level.setBlock(b,Blocks.AIR.defaultBlockState(),2);
                    var end=TombDisplayEntity.coffinTransform(1).transformPosition(new org.joml.Vector3f(cell[0]+.5F,cell[1]+.5F,cell[2]+.5F));
                    b=scene.world(-4+net.minecraft.util.Mth.floor(end.x),83+net.minecraft.util.Mth.floor(end.y),3955+net.minecraft.util.Mth.floor(end.z));
                    if (level.getBlockState(b).is(Blocks.BARRIER)) level.setBlock(b,Blocks.AIR.defaultBlockState(),2);
                }
            }
            int[] p=gate.pos();
            displays.put(gate.id(),display(gate.id(),gate.kind(),Vec3.atLowerCornerOf(scene.world(p[0],p[1],p[2]))));
            barriers(gate,true);
        }
        seal=display("seal",TombDisplayEntity.SEAL,scene.barrierCenter().add(-20.5,-14.5,0));
        for (int[] b : layout.backups())
            for (BlockPos p : BlockPos.betweenClosed(scene.world(b[0],b[1],b[2]),scene.world(b[3],b[4],b[5])))
                level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
    }
    public void setOpen(String id, boolean open) {
        TombDisplayEntity display=displays.get(id);
        var gate=layout.gates().stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
        if (gate.kind()==5 && display.target()==(open ? 1 : 0)) return;
        display.animateTo(open ? 1 : 0, duration(gate));
        if (gate.kind()==5) coffinBarriers(gate,-1);
        else if (!open && gate.kind()>=3) barriers(gate,true);
    }
    public boolean fullyOpen(String id) { return displays.get(id).progress(0) >= 1; }
    private int duration(ShiHuangLayout.Gate gate) {
        return gate.kind() < 3 ? (gate.size()[1]-2)*10 : gate.kind()==5 ? icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity.COFFIN_OPEN_TICKS : 100;
    }
    public void tick() {
        for (var gate : layout.gates()) {
            var display=displays.get(gate.id());
            if (gate.kind()<3) ironBarriers(gate,(int)Math.floor(display.progress(0)*(gate.size()[1]-2)+1e-5));
            else if (gate.kind()==5) coffinBarriers(gate,display.progress(0)<=0 ? 0 : display.progress(0)>=1 ? 1 : -1);
            else if (display.target()==1 && display.progress(0)>=1) barriers(gate,false);
        }
    }
    private void ironBarriers(ShiHuangLayout.Gate gate,int cleared) {
        Integer previous=ironCleared.put(gate.id(),cleared);
        if (previous!=null && previous==cleared) return;
        int[] p=gate.pos(),s=gate.size(); BlockPos min=scene.world(p[0],p[1],p[2]);
        for (int y=0;y<s[1];y++) {
            if (previous!=null && (y<previous)==(y<cleared)) continue;
            for (int x=0;x<s[0];x++) for (int z=0;z<s[2];z++)
                level.setBlock(min.offset(x,y,z),y<cleared ? Blocks.AIR.defaultBlockState() : Blocks.BARRIER.defaultBlockState(),2);
        }
    }
    private void coffinBarriers(ShiHuangLayout.Gate gate,int pose) {
        if (pose==coffinPose) return;
        int previous=coffinPose;
        for (BlockPos p : coffinBlocks) if (level.getBlockState(p).is(Blocks.BARRIER))
            level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        coffinBlocks.clear(); coffinPose=pose;
        int[] origin=gate.pos();
        var transform=TombDisplayEntity.coffinTransform(pose==1 ? 1 : 0);
        if (pose>=0) for (int[] b : gate.decoration()) {
            var center=transform.transformPosition(new org.joml.Vector3f(b[0]+.5F,b[1]+.5F,b[2]+.5F));
            BlockPos p=scene.world(origin[0]+net.minecraft.util.Mth.floor(center.x),
                    origin[1]+net.minecraft.util.Mth.floor(center.y),origin[2]+net.minecraft.util.Mth.floor(center.z));
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p,Blocks.BARRIER.defaultBlockState(),2); coffinBlocks.add(p);
            }
        }
        if (pose==1 && previous==-1) level.playSound(null,scene.world(-11,79,3963),
                net.minecraft.sounds.SoundEvents.DEEPSLATE_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,5,.55F);
    }
    private void barriers(ShiHuangLayout.Gate gate, boolean closed) {
        if (gate.kind()==5) { coffinBarriers(gate,closed ? 0 : 1); return; }
        if (gate.kind()<3) { ironBarriers(gate,closed ? 0 : gate.size()[1]-2); return; }
        if (barrierClosed.get(gate.id()) != null && barrierClosed.get(gate.id()) == closed) return;
        barrierClosed.put(gate.id(),closed);
        BlockPos min, max;
        if (gate.kind()==3 || gate.kind()==4) {
            int lo=gate.kind()==3 ? -20 : 1, hi=gate.kind()==3 ? 0 : 20;
            min=scene.world(lo,72,3804); max=scene.world(hi,100,3808);
        } else {
            int[] p=gate.pos(),s=gate.size(); min=scene.world(p[0],p[1],p[2]);
            max=min.offset(s[0]-1,s[1]-1,s[2]-1);
        }
        var decoration=new java.util.HashSet<BlockPos>();
        if (gate.kind()==3 || gate.kind()==4) {
            int[] pivot=gate.pos();
            for (int[] p : gate.decoration()) decoration.add(scene.world(pivot[0]+p[0],pivot[1]+p[1],pivot[2]+p[2]));
        }
        for (BlockPos p : BlockPos.betweenClosed(min,max)) {
            boolean occupied=gate.kind()==5 || p.getZ()>=scene.world(0,72,3806).getZ() || decoration.contains(p);
            level.setBlock(p, closed && occupied ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(),2);
        }
    }
    public void seal(boolean active) { seal.animateTo(active ? 1 : 0,0); }
    public void flashSeal() { seal.animateTo(1,0); seal.animateTo(0,40); }
    public boolean owns(TombDisplayEntity entity) { return displays.containsValue(entity) || entity==seal; }
    public void reset() {
        displays.values().forEach(TombDisplayEntity::reset); seal.reset(); barrierClosed.clear(); ironCleared.clear();
        for (var gate : layout.gates()) barriers(gate,true);
    }
}
