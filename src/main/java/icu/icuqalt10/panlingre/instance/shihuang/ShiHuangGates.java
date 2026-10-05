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
    private TombDisplayEntity seal;

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
            int[] p=gate.pos();
            displays.put(gate.id(),display(gate.id(),gate.kind(),Vec3.atLowerCornerOf(scene.world(p[0],p[1],p[2]))));
            barriers(gate,true);
        }
        seal=display("seal",TombDisplayEntity.SEAL,scene.worldPoint(-20,72,3803.98));
        for (int[] b : layout.backups())
            for (BlockPos p : BlockPos.betweenClosed(scene.world(b[0],b[1],b[2]),scene.world(b[3],b[4],b[5])))
                level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
    }
    public void setOpen(String id, boolean open) {
        TombDisplayEntity display=displays.get(id);
        var gate=layout.gates().stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
        display.animateTo(open ? 1 : 0, duration(gate));
        // Closing restores collision immediately; opening releases it only at the endpoint.
        if (!open) barriers(gate,true);
    }
    public boolean fullyOpen(String id) { return displays.get(id).progress(0) >= 1; }
    private int duration(ShiHuangLayout.Gate gate) {
        return gate.kind() < 3 ? (gate.size()[1]-2)*10 : gate.kind()==5 ? 60 : 100;
    }
    public void tick() {
        for (var gate : layout.gates()) {
            var display=displays.get(gate.id());
            if (display.target()==1 && display.progress(0)>=1) barriers(gate,false);
        }
    }
    private void barriers(ShiHuangLayout.Gate gate, boolean closed) {
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
        for (BlockPos p : BlockPos.betweenClosed(min,max)) {
            boolean top=gate.kind()<3 && p.getY()>max.getY()-2;
            level.setBlock(p, closed || top ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(),2);
        }
    }
    public void seal(boolean active) { seal.animateTo(active ? 1 : 0,0); }
    public boolean owns(TombDisplayEntity entity) { return displays.containsValue(entity) || entity==seal; }
    public void reset() {
        displays.values().forEach(TombDisplayEntity::reset); seal.reset(); barrierClosed.clear();
        for (var gate : layout.gates()) barriers(gate,true);
    }
}
