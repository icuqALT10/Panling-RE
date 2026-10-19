package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Short lived visual only; damage stays on the server's existing attack timeline. */
public record TombLightPayload(int shape, Vec3 from, Vec3 to, int color, float width, int ticks, long startTick, Vec3 axis, int clockOwner, double clockStart)
        implements CustomPacketPayload {
    public TombLightPayload(int shape,Vec3 from,Vec3 to,int color,float width,int ticks){this(shape,from,to,color,width,ticks,Long.MIN_VALUE);}
    public TombLightPayload(int shape,Vec3 from,Vec3 to,int color,float width,int ticks,long startTick){this(shape,from,to,color,width,ticks,startTick,new Vec3(0,1,0));}
    public TombLightPayload(int shape,Vec3 from,Vec3 to,int color,float width,int ticks,long startTick,Vec3 axis){this(shape,from,to,color,width,ticks,startTick,axis,-1,0);}
    public TombLightPayload onClock(icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity owner,double delay){
        return new TombLightPayload(shape,from,to,color,width,ticks,startTick,axis,owner.getId(),owner.actionClock(0)+delay);
    }
    public static final int BEAM=0, RING=1, BURST=2, ORB=3, ARENA_SHAKE=4, LIGHTNING=5,
            FLOW_EDGE=6, FIRE_FAN=7, CRESCENT=8, FALLING_SWORD=9, WATER_COLUMN=10,
            ELEMENT_CRESCENT=11, LAVA_BOULDER=12, HIT_SPARKS=13, WOOD_SURGE=14;
    public static final Type<TombLightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "tomb_light"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TombLightPayload> STREAM_CODEC = StreamCodec.of((b,p) -> {
        b.writeVarInt(p.shape); b.writeVec3(p.from); b.writeVec3(p.to);
        b.writeInt(p.color); b.writeFloat(p.width); b.writeVarInt(p.ticks); b.writeLong(p.startTick); b.writeVec3(p.axis); b.writeVarInt(p.clockOwner); b.writeDouble(p.clockStart);
    }, b -> new TombLightPayload(b.readVarInt(), b.readVec3(), b.readVec3(), b.readInt(), b.readFloat(), b.readVarInt(), b.readLong(), b.readVec3(), b.readVarInt(), b.readDouble()));
    public static TombLightPayload burst(Vec3 center,int color,float radius,int ticks){return new TombLightPayload(BURST,center,Vec3.ZERO,color,radius,ticks);}
    public static TombLightPayload orb(Vec3 center,Vec3 velocity,int color,float radius,int ticks){return new TombLightPayload(ORB,center,velocity,color,radius,ticks);}
    public void send(net.minecraft.server.level.ServerLevel level,double range){
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,from.x,from.y,from.z,range,this);
    }
    public static void shakeArena(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.AABB bounds,int ticks){
        var effect=new TombLightPayload(ARENA_SHAKE,new Vec3(bounds.minX,bounds.minY,bounds.minZ),
                new Vec3(bounds.maxX,bounds.maxY,bounds.maxZ),0,2,ticks);
        for(var player:level.players())if(bounds.contains(player.position()))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,effect);
    }
    public static void handle(TombLightPayload p, IPayloadContext context) {
        context.enqueueWork(() -> icu.icuqalt10.panlingre.client.TombLightEffects.add(p));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
