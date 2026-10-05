package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.instance.InstanceSession;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/** The updated 177 x 106 x 763 template starts at (-88,60,3219) in slot zero. */
public record ShiHuangScene(BlockPos origin) {
    public static ShiHuangScene forSession(InstanceSession session) {
        var template = session.level().getStructureManager().getOrCreate(session.definition().template());
        var size = template.getSize();
        Vec3 center = session.center();
        return new ShiHuangScene(BlockPos.containing(center.x - (size.getX() - 1) * .5,
                center.y - (size.getY() - 1) * .5, center.z - (size.getZ() - 1) * .5));
    }

    public BlockPos block(int x, int y, int z) { return origin.offset(x, y, z); }
    public Vec3 point(double x, double y, double z) { return Vec3.atLowerCornerOf(origin).add(x, y, z); }
    public BlockPos world(int x, int y, int z) { return block(x + 88, y - 60, z - 3219); }
    public Vec3 worldPoint(double x, double y, double z) { return point(x + 88, y - 60, z - 3219); }
    public AABB bounds() { return new AABB(Vec3.atLowerCornerOf(origin), point(177,106,763)); }
    public AABB region(int x1,int y1,int z1,int x2,int y2,int z2) {
        return new AABB(worldPoint(x1,y1,z1),worldPoint(x2+1,y2+1,z2+1));
    }
    public List<ServerPlayer> players(ServerLevel level) {
        return level.players().stream().filter(p -> p.isAlive() && !p.isSpectator() && bounds().contains(p.position())).toList();
    }
    public boolean allInside(ServerLevel level, AABB region) {
        var players = players(level);
        return !players.isEmpty() && players.stream().allMatch(p -> region.contains(p.position()));
    }
    public AABB dragonRoom() { return region(-78,72,3661,78,125,3803); }
    public AABB emperorArena() { return region(-48,72,3839,48,115,3935); }
    public Vec3 emperorCombatPosition() { return worldPoint(.5,72,3887.5); }
    public Vec3 dragonSpawn() { return worldPoint(1,72,3721); }
    public Vec3 barrierCenter() { return worldPoint(.5,86.5,3803.98); }
    public Vec3 coffinMouth() { return point(88.5, 26.1, 744.5); }
    // The retained coffin body ends at Y=83. Keep the final shrunken pose above its solid floor.
    public Vec3 coffinInterior() { return point(88.5, 23.3, 744.5); }
    public BlockPos doorMin() { return block(68, 12, 585); }
    public BlockPos doorMax() { return block(108, 40, 589); }
    public BlockPos[] beacons() {
        return new BlockPos[]{block(12, 15, 454), block(164, 15, 453),
                block(12, 15, 569), block(164, 15, 569)};
    }
    public BlockPos[] sealBulbs() {
        return new BlockPos[]{world(-73,75,3673), world(73,75,3672),
                world(-73,75,3788), world(73,75,3788)};
    }
}
