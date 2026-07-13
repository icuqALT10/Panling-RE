package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShockwaveUpdatePayload(
        Vec3 center,
        int age,
        long blockedMask1,
        long blockedMask2
) implements CustomPacketPayload {

    public static final Type<ShockwaveUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "shockwave_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShockwaveUpdatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    p -> p.center().x,
                    ByteBufCodecs.DOUBLE,
                    p -> p.center().y,
                    ByteBufCodecs.DOUBLE,
                    p -> p.center().z,
                    ByteBufCodecs.VAR_INT,
                    ShockwaveUpdatePayload::age,
                    ByteBufCodecs.VAR_LONG,
                    ShockwaveUpdatePayload::blockedMask1,
                    ByteBufCodecs.VAR_LONG,
                    ShockwaveUpdatePayload::blockedMask2,
                    (x,y,z,age,m1,m2)->
                            new ShockwaveUpdatePayload(
                                    new Vec3(x,y,z), age, m1, m2)

            );
    public static void handle(ShockwaveUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(()->{
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if(level==null)
                return;
            Vec3 center =
                    payload.center();
            double radius =
                    payload.age()*1.2;
            for(int i=0;i<72;i++){
                boolean blocked;
                if(i<64){
                    blocked = (payload.blockedMask1() &(1L<<i))!=0;
                }else{
                    blocked = (payload.blockedMask2() &(1L<<(i-64)))!=0;
                }
                if(blocked)
                    continue;
                double rad = Math.toRadians(i*5);
                double x = center.x+ Math.cos(rad)*radius;
                double z = center.z+ Math.sin(rad)*radius;

                level.addParticle(
                        ParticleTypes.GUST,
                        true,
                        x,
                        center.y,
                        z,
                        0,
                        1,
                        0
                );
            }
        });
    }


    @Override
    public Type<? extends CustomPacketPayload> type(){

        return TYPE;

    }
}