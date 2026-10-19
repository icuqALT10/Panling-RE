package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.animation.WorldTimeAnimationController;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangBossSounds;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;

/** 始皇的唯一动作时钟；移动、转向、事件和 OBB 由服务端控制。 */
public final class ShiHuangEntity extends MultipartEntity implements GeoEntity, PanLingEntities {
    private static final EntityDataAccessor<String> ANIMATION=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> FROZEN_AT=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DYING=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> RAGE_START=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> MANA=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.INT);
    public static final int MAX_MANA=20, SKILL_MANA=7;
    private static final EntityDataAccessor<Boolean> ENHANCED=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> AIM_PITCH=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STOP_PHASE=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GUARD_FROM=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INTERVAL_GAIT=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INTERVAL_WEIGHT=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> INTERVAL_DIRECTION=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> BLINK_START=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<org.joml.Vector3f> BLINK_FROM=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<org.joml.Vector3f> BLINK_TO=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> BLINK_YAW=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> BLINK_CLIP=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> BLINK_POSE=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.FLOAT);
    public static final int BLINK_LIGHT_TICKS=10, BLINK_GHOST_TICKS=16;
    public static final int ATTACK_COOLDOWN_TICKS=60;
    public static final double HEALTH_PER_PLAYER=10000;
    private static final EntityDataAccessor<Long> ENTRANCE_START=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<org.joml.Vector3f> ENTRANCE_ORIGIN=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<net.minecraft.core.BlockPos> ENTRANCE_BLOCK=SynchedEntityData.defineId(ShiHuangEntity.class,EntityDataSerializers.BLOCK_POS);
    // Reveal at 4s, draw after the flare clears, then point as the dragon finishes appearing at 6.675s.
    public static final int COFFIN_OPEN_TICKS=20, ENTRANCE_DESCEND_TICK=24, ENTRANCE_GHOST_TICK=ENTRANCE_DESCEND_TICK, ENTRANCE_LAND_TICK=80, ENTRANCE_REVEAL_TICK=80,
            ENTRANCE_DESCENT_TICKS=12, ENTRANCE_GHOST_FADE_TICKS=2, ENTRANCE_DRAW_TICK=ENTRANCE_REVEAL_TICK+12,
            ENTRANCE_END_TICK=180;
    public static final float ENTRANCE_POINT_TICK=ENTRANCE_REVEAL_TICK+53.5F;
    public static final int PLANT_TICK=19, FLOOR_TICK=100, VANISH_TICK=110, REWARD_TICK=VANISH_TICK+60;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar=new ServerBossEvent(Component.translatable("entity.panlingre.shihuang"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS);
    private final ShiHuangPartEntity[] parts=new ShiHuangPartEntity[1];
    private final ShiHuangCombat combat=new ShiHuangCombat(this);
    private final ShiHuangPressure pressure=new ShiHuangPressure(this);
    private boolean smokeSent, settled;
    private String locomotion="idle";
    private long nextAttack;
    private int retreatSide;
    private int guardHits;
    private Vec3 rapidDestination;
    private double rapidStep;

    public ShiHuangEntity(EntityType<? extends ShiHuangEntity> type,Level level) {
        super(type,level);
        for(int i=0;i<parts.length;i++) parts[i]=new ShiHuangPartEntity(this,i+1);
        setId(ENTITY_COUNTER.getAndAdd(parts.length+1)+1);
        setPersistenceRequired();
        if(!level.isClientSide){entityData.set(START,level.getGameTime());nextAttack=level.getGameTime()+ATTACK_COOLDOWN_TICKS;}
        updateParts(pose(0),yBodyRot,position());
    }
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,HEALTH_PER_PLAYER)
                .add(Attributes.ATTACK_DAMAGE,16)
                .add(Attributes.ARMOR,100)
                .add(Attributes.KNOCKBACK_RESISTANCE,1)
                .add(Attributes.MOVEMENT_SPEED,0.28)
                .add(Attributes.FOLLOW_RANGE,128);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);b.define(ANIMATION,"idle");b.define(START,0L);b.define(FROZEN_AT,-1L);b.define(DYING,false);b.define(ENHANCED,false);
        b.define(RAGE_START,-1L);b.define(MANA,0);b.define(AIM_PITCH,0F);b.define(STOP_PHASE,0F);
        b.define(INTERVAL_GAIT,0F);b.define(INTERVAL_WEIGHT,0F);b.define(INTERVAL_DIRECTION,1);b.define(GUARD_FROM,-1F);
        b.define(BLINK_START,-1L);b.define(BLINK_FROM,new org.joml.Vector3f());b.define(BLINK_TO,new org.joml.Vector3f());
        b.define(BLINK_YAW,0F);b.define(BLINK_CLIP,"idle");b.define(BLINK_POSE,0F);
        b.define(ENTRANCE_START,-1L);b.define(ENTRANCE_ORIGIN,new org.joml.Vector3f());b.define(ENTRANCE_BLOCK,net.minecraft.core.BlockPos.ZERO);
    }
    public double entranceTicks(float partial){long start=entityData.get(ENTRANCE_START);return start<0?-1:Math.max(0,(level().getGameTime()-start)+(double)partial);}
    public Vec3 entranceOrigin(){return Vec3.atLowerCornerOf(entityData.get(ENTRANCE_BLOCK)).add(new Vec3(entityData.get(ENTRANCE_ORIGIN)));}
    private void setEntranceOrigin(Vec3 point){
        var block=net.minecraft.core.BlockPos.containing(point);entityData.set(ENTRANCE_BLOCK,block);
        var offset=point.subtract(Vec3.atLowerCornerOf(block));entityData.set(ENTRANCE_ORIGIN,offset.toVector3f());
    }
    public void beginEntrance(){
        entityData.set(ENTRANCE_START,level().getGameTime());
        bossBar.setVisible(false);
        setEntranceOrigin(position());
        setInvisible(true);setInvulnerable(true);setNoAi(true);setNoGravity(true);noPhysics=true;
    }
    /** The controller owns this timeline; clients derive the same effects from the synchronized start. */
    public boolean tickEntrance(Vec3 landing){
        double t=entranceTicks(0);if(t<0)return false;
        setInvisible(t<ENTRANCE_REVEAL_TICK);
        setDeltaMovement(Vec3.ZERO);
        // Transfer while hidden; the two columns convey the journey before the apparition fades in.
        setPos(landing);
        if(t>=ENTRANCE_REVEAL_TICK){setInvisible(false);play("emerge");combat.background();}
        if(t<ENTRANCE_END_TICK)return false;
        entityData.set(ENTRANCE_START,-1L);setInvisible(false);noPhysics=false;setNoGravity(false);
        setNoAi(false);setInvulnerable(false);play("idle");beginAttackCooldown();return true;
    }
    public String animation(){return entityData.get(ANIMATION);}
    public boolean dying(){return entityData.get(DYING);}
    public boolean enhanced(){return entityData.get(ENHANCED);}
    public void setEnhanced(boolean value){entityData.set(ENHANCED,value);}
    public int elapsedTicks(){return (int)Math.floor(seconds(0)*20+.0001);}
    public boolean enraged(){return entityData.get(RAGE_START)>=0&&!dying();}
    public double commandGlowEnd(){return switch(animation()){case "ally_d01_longting"->9.9;case "ally_d02_chongfeng"->4.5;case "ally_d03_longxi"->2.75;case "ultimate_full"->7.6;default->0;};}
    public boolean hasCombatGlow(float partial){return !dying()&&(enraged()||commandGlowEnd()>0&&seconds(partial)<=commandGlowEnd()+.15);}
    public double actionSpeed(){return enraged()&&ShiHuangCombat.ACTIONS.containsKey(animation())?1.5:1;}
    public int mana(){return entityData.get(MANA);}
    void addMana(int amount){entityData.set(MANA,Mth.clamp(mana()+amount,0,MAX_MANA));}
    void spendMana(){addMana(-SKILL_MANA);}
    private double clockAt(double tick){long rage=entityData.get(RAGE_START);return tick+(rage>=0&&!dying()?Math.max(0,tick-rage)*.5:0);}
    public double actionClock(float partial){long freeze=entityData.get(FROZEN_AT);return clockAt(freeze>=0?freeze:level().getGameTime()+(double)partial);}
    public double seconds(float partial){
        long freeze=entityData.get(FROZEN_AT);
        double tick=freeze>=0?freeze:level().getGameTime()+(double)partial;
        double elapsed=ShiHuangCombat.ACTIONS.containsKey(animation())
                ?actionClock(partial)-clockAt(entityData.get(START)):tick-entityData.get(START);
        double time=Math.max(0,elapsed)/20.0;
        // Stretch only the charging pose; the release stroke retains its original speed.
        if(animation().equals("ultimate_full")&&time>1.95)time=time<6.4?1.95+(time-1.95)*2.45/4.45:time-2;
        return time*(animation().equals("run")?2:animation().startsWith("interval_side_hop_")?1.25:1);
    }
    private void updateRage(){
        if(enraged()||dying()||entranceTicks(0)>=0||getHealth()>getMaxHealth()*.25)return;
        entityData.set(RAGE_START,level().getGameTime());
        removeEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze);
        bossBar.setColor(BossEvent.BossBarColor.RED);
        sound(SoundEvents.ENDER_DRAGON_GROWL,3,.8F);
    }
    public double blinkTicks(float partial){long start=entityData.get(BLINK_START),freeze=entityData.get(FROZEN_AT);return start<0?-1:(freeze>=0?freeze:level().getGameTime()+(double)partial)-start;}
    public Vec3 blinkFrom(){return new Vec3(entityData.get(BLINK_FROM));}
    public Vec3 blinkTo(){return new Vec3(entityData.get(BLINK_TO));}
    public float blinkYaw(){return entityData.get(BLINK_YAW);}
    public ShiHuangPose.Frame blinkPose(){String clip=entityData.get(BLINK_CLIP);return ShiHuangPose.sample(clip,entityData.get(BLINK_POSE),Set.of("idle","walk","run","walk_back").contains(clip));}
    public boolean looping(){return Set.of("idle","walk","run","walk_back").contains(animation());}
    public void play(String name){
        if(name.equals(animation()))return;
        entityData.set(GUARD_FROM,name.equals("counter_guard_sweep")&&animation().equals("interval_guard")?(float)seconds(0):-1F);
        guardHits=0;
        if(!name.startsWith("interval_"))pressure.stop();
        entityData.set(ANIMATION,name);entityData.set(START,level().getGameTime());combat.begin(name);
    }
    public ShiHuangPose.Frame pose(float partial){
        double entrance=entranceTicks(partial);
        if(entrance>=0&&entrance<ENTRANCE_REVEAL_TICK)return ShiHuangPose.sample("emerge",0,false);
        var frame=ShiHuangPose.sample(animation(),seconds(partial),looping());
        var sweep=frame;
        boolean guardBlend=animation().equals("counter_guard_sweep")&&entityData.get(GUARD_FROM)>=0&&seconds(partial)<.1;
        if(guardBlend)frame=ShiHuangPose.sample("interval_guard",entityData.get(GUARD_FROM),false);
        boolean flashStart=animation().startsWith("interval_flash_")&&seconds(partial)<.15;
        if(guardBlend||flashStart||Set.of("interval_walk","interval_press","interval_feint","interval_guard","interval_point","interval_watch").contains(animation())){
            if(animation().equals("interval_walk"))frame=ShiHuangPose.sample("idle",0,true);
            double weight=entityData.get(INTERVAL_WEIGHT);
            if(flashStart){double u=seconds(partial)/.15;weight*=1-u*u*(3-2*u);}
            int direction=entityData.get(INTERVAL_DIRECTION);
            double phase=entityData.get(INTERVAL_GAIT)+(!guardBlend&&entityData.get(FROZEN_AT)<0?partial*weight/20:0);
            var gait=ShiHuangPose.sample(direction==2?"walk":"walk_back",phase,true);
            var poses=new LinkedHashMap<>(frame.bones());
            // Turn the hips into the diagonal step; counter-turn the waist to keep the sword on target.
            double turn=direction==2?0:direction*Math.PI/12;
            var hip=poses.get("pelvis");
            for(var entry:gait.bones().entrySet()){
                String bone=entry.getKey();
                if(!bone.equals("pelvis")&&!bone.startsWith("leg_")&&!bone.startsWith("foot_")
                        &&!bone.startsWith("robe_")&&!bone.startsWith("tasset_")&&!bone.equals("scabbard"))continue;
                var a=poses.get(bone);var b=entry.getValue();
                Vec3 rotation=b.rotation().add(0,bone.equals("pelvis")?turn:0,0);
                Vec3 position=b.position();
                if(bone.equals("leg_r_upper")||bone.equals("leg_l_upper")){
                    // Keep the diagonal foot path while the pelvis turns only 15 degrees.
                    Vec3 offset=ShiHuangCombat.PIVOTS.get(bone).subtract(ShiHuangCombat.PIVOTS.get("pelvis"));
                    position=offset.add(position.multiply(-1.0/16,1.0/16,1.0/16)).yRot((float)(turn*2)).subtract(offset).multiply(-16,16,16);
                    var angles=new org.joml.Matrix3f().rotationY((float)(turn*2))
                            .rotateZYX((float)rotation.z,(float)rotation.y,(float)rotation.x)
                            .getEulerAnglesZYX(new org.joml.Vector3f());
                    rotation=new Vec3(angles.x,angles.y,angles.z);
                }
                poses.put(bone,new ShiHuangPose.BonePose(a.rotation().lerp(rotation,weight),a.position().lerp(position,weight),a.scale().lerp(b.scale(),weight)));
            }
            var waist=poses.get("waist");
            poses.put("waist",new ShiHuangPose.BonePose(waist.rotation().add(hip.rotation().subtract(poses.get("pelvis").rotation())),waist.position(),waist.scale()));
            frame=ShiHuangPose.rebuild(poses);
        }
        if(guardBlend){
            double u=seconds(partial)/.1;u=u*u*(3-2*u);
            var poses=new LinkedHashMap<String,ShiHuangPose.BonePose>();
            for(var entry:frame.bones().entrySet()){
                var a=entry.getValue();var b=sweep.bones().get(entry.getKey());
                poses.put(entry.getKey(),new ShiHuangPose.BonePose(a.rotation().lerp(b.rotation(),u),a.position().lerp(b.position(),u),a.scale().lerp(b.scale(),u)));
            }
            frame=ShiHuangPose.rebuild(poses);
        }
        if(animation().endsWith("_to_idle")){
            String source=animation().substring(0,animation().length()-8);
            var contact=ShiHuangPose.sample(source,entityData.get(STOP_PHASE),true);
            var rest=ShiHuangPose.sample("idle",0,true);
            double u=Mth.clamp(seconds(partial)/.4,0,1);u=u*u*(3-2*u);
            var poses=new LinkedHashMap<String,ShiHuangPose.BonePose>();
            for(var entry:contact.bones().entrySet()){
                var a=entry.getValue();var b=rest.bones().get(entry.getKey());
                poses.put(entry.getKey(),new ShiHuangPose.BonePose(a.rotation().lerp(b.rotation(),u),a.position().lerp(b.position(),u),a.scale().lerp(b.scale(),u)));
            }
            frame=ShiHuangPose.rebuild(poses);
        }
        if(ShiHuangCombat.ACTIONS.containsKey(animation())&&seconds(partial)>ShiHuangPose.duration(animation()))
            frame=ShiHuangPose.sample("idle",seconds(partial)-ShiHuangPose.duration(animation()),true);
        if(!dying()&&!isNoAi()){
            var poses=new LinkedHashMap<>(frame.bones());var head=poses.get("head");
            double parentPitch=0,parentYaw=0;
            for(String n:List.of("pelvis","waist","chest","neck")){parentPitch+=poses.get(n).rotation().x;parentYaw+=poses.get(n).rotation().y;}
            poses.put("head",new ShiHuangPose.BonePose(new Vec3(Mth.clamp(entityData.get(AIM_PITCH)-parentPitch,-.6,.6),Mth.clamp(-parentYaw,-.8,.8),head.rotation().z),head.position(),head.scale()));
            frame=ShiHuangPose.rebuild(poses);
        }
        return frame;
    }
    public Vec3 bonePoint(ShiHuangPose.Frame frame,String bone,Vec3 model){return ShiHuangPose.point(frame,bone,model,yBodyRot,position());}
    public Vec3 visualPosition(float partial){return getPosition(partial);}
    public Vec3 swordTrailPoint(ShiHuangPose.Frame frame,boolean tip,float partial){
        String bone=tip?"sword_trail_end":"sword_trail_start";
        return ShiHuangPose.point(frame,bone,ShiHuangCombat.PIVOTS.get(bone),Mth.rotLerp(partial,yBodyRotO,yBodyRot),visualPosition(partial));
    }
    public static Vec3 swordTrailLocalPoint(ShiHuangPose.Frame frame,boolean tip){
        String bone=tip?"sword_trail_end":"sword_trail_start";
        return ShiHuangPose.point(frame,bone,ShiHuangCombat.PIVOTS.get(bone),180,Vec3.ZERO);
    }
    public List<String> swordElements(double time){
        var action=ShiHuangCombat.ACTIONS.get(animation());
        if(action==null||time<Math.max(.1,action.gather()))return List.of();
        if(animation().equals("skill_s08_wuxing")){
            var window=action.windows().getLast();
            for(var next:action.windows())if(time<=next.to()+.08){window=next;break;}
            return List.of(window.element());
        }
        return action.cards();
    }
    public List<String> cardElements(){var a=ShiHuangCombat.ACTIONS.get(animation());return a!=null&&a.cardLead()>0?a.cards():List.of();}
    public double cardDissolve(){var a=ShiHuangCombat.ACTIONS.get(animation());return a==null?0:a.dissolve();}
    public double cardGatherEnd(){var a=ShiHuangCombat.ACTIONS.get(animation());return a==null?0:a.gather();}
    public Vec3 cardPoint(ShiHuangPose.Frame frame,String element,float partial){
        String bone="card_"+element;
        return ShiHuangPose.point(frame,bone,ShiHuangCombat.PIVOTS.get(bone),Mth.rotLerp(partial,yBodyRotO,yBodyRot),getPosition(partial));
    }
    public ShiHuangController tomb(){var s=InstanceManager.sessionFor(this);return s!=null&&s.controller() instanceof ShiHuangController t?t:null;}
    public List<ServerPlayer> arenaPlayers(){
        if(!(level() instanceof ServerLevel server))return List.of();
        var t=tomb();return t==null?server.players().stream().filter(p->p.distanceToSqr(this)<128*128&&p.isAlive()&&!p.isSpectator()).toList():t.scene().players(server);
    }
    public void sound(SoundEvent sound,float volume,float pitch){ShiHuangBossSounds.play(this,level(),getX(),getY(),getZ(),sound,SoundSource.HOSTILE,volume,pitch);}
    public void face(Vec3 target,float step){
        Vec3 d=target.subtract(position());float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90);
        setYRot(Mth.approachDegrees(getYRot(),yaw,step));yBodyRot=yHeadRot=getYRot();
    }
    @Override public void tick(){
        if(!level().isClientSide&&getTarget() instanceof net.minecraft.world.entity.player.Player p
                &&(!p.isAlive()||p.isCreative()||p.isSpectator())){stopRapidMovement();setTarget(null);}
        if(!level().isClientSide&&rapidDestination!=null){
            Vec3 step=rapidDestination.subtract(position());
            // Arrive over five windup ticks, including a thrust's longer approach.
            double speed=rapidStep*actionSpeed();
            if(step.length()>speed)step=step.normalize().scale(speed);
            var body=getBoundingBox().deflate(0,.01,0);
            if(dying()||isNoAi()||entityData.get(FROZEN_AT)>=0||!level().noCollision(this,body.expandTowards(step)))stopRapidMovement();
            else {setDeltaMovement(step);hasImpulse=true;}
        }
        super.tick();
        if(!level().isClientSide&&rapidDestination!=null&&position().distanceToSqr(rapidDestination)<.0025)stopRapidMovement();
        if(!level().isClientSide){
            if(getPersistentData().hasUUID("ShiHuangOwner")&&InstanceManager.sessionFor(this)==null&&!settled){discard();return;}
            updateRage();
            bossBar.setProgress(Math.max(0,getHealth()/getMaxHealth()));
            bossBar.setVisible(!dying()&&(entranceTicks(0)<0||entranceTicks(0)>=ENTRANCE_REVEAL_TICK));
            var audience=new HashSet<>(arenaPlayers());
            for(var p:List.copyOf(bossBar.getPlayers()))if(!audience.contains(p))bossBar.removePlayer(p);
            if(!dying())for(var p:audience){bossBar.addPlayer(p);}
            if(dying())tickFinale();
            else if(!isNoAi()&&entityData.get(FROZEN_AT)<0)tickCombat();
        }
        updateParts(pose(0),yBodyRot,level().isClientSide?visualPosition(1):position());
    }
    private void tickCombat(){
        var target=arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(target==null&&tomb()==null&&getTarget() instanceof ServerPlayer previous
                &&previous.level()==level()&&previous.isAlive()&&!previous.isRemoved()&&!previous.isCreative()&&!previous.isSpectator())target=previous;
        setTarget(target);
        boolean action=ShiHuangCombat.ACTIONS.containsKey(animation());
        boolean tracks=!action||combat.tracksTarget();
        if(target!=null&&tracks){Vec3 d=target.getEyePosition().subtract(position().add(0,3.5*ShiHuangPose.MODEL_SCALE,0));entityData.set(AIM_PITCH,(float)Math.atan2(d.y,d.horizontalDistance()));}
        else if(!tracks)entityData.set(AIM_PITCH,0F);
        if(action){
            if(target!=null&&tracks)face(target.position(),(float)((animation().startsWith("combo_")?(combat.inSwing()?3:9):3)*actionSpeed()));
            combat.adjustPosition();
            combat.background();
            combat.tick();
            if(seconds(0)>=combat.duration()){
                boolean intervalCounter=animation().equals("counter_watch_liaotian")||animation().equals("counter_guard_sweep");
                combat.finish();
                play("idle");
                if(intervalCounter)pressure.finishCounter();else beginAttackCooldown();
            }
            return;
        }
        combat.background();
        if(combat.tryWatchInterrupt())return;
        if(target!=null&&!pressure.busy()&&level().getGameTime()>=nextAttack){
            if(target.position().subtract(position()).horizontalDistance()>6&&!blinkBeside(target)){
                pressure.stop();pauseIntervalGait();face(target.position(),6);play("walk");
                stepDuringInterval(target.position().subtract(position()).multiply(1,0,1).normalize().scale(.15));return;
            }
            locomotion="idle";
            play(enhanced()&&!combat.ultimateUsed()&&getHealth()<=getMaxHealth()*.5?"ultimate_full":combat.choose(distanceTo(target)));
            return;
        }
        if(rapidDestination!=null)return;
        if(pressure.tick(target))return;
        if(target!=null)face(target.position(),(float)(6*actionSpeed()));
        if(animation().contains("_to_")){
            if(seconds(0)>=.4)play(locomotion);
            return;
        }
        if(target==null){stopWalking();return;}
        double distance=distanceTo(target);
        String wanted=distance<=12?"walk_back":"walk";
        // A small boundary margin prevents alternating start/stop clips when a target stands on a threshold.
        if(animation().equals("walk_back")&&distance<12.3)wanted="walk_back";
        if(!animation().equals(wanted)){
            if(!animation().equals("idle")){stopWalking();return;}
            locomotion=wanted;play("idle_to_"+wanted);return;
        }
        Vec3 forward=target.position().subtract(position()).multiply(1,0,1).normalize();
        Vec3 step;
        if(wanted.equals("walk_back")){
            if(retreatSide==0)retreatSide=random.nextBoolean()?1:-1;
            Vec3 side=new Vec3(-forward.z,0,forward.x).scale(retreatSide);
            step=side.subtract(forward).normalize().scale(3.5/16/.6/20*2*ShiHuangPose.MODEL_SCALE);
        }else step=forward.scale(3.0/20);
        var controller=tomb();
        if(controller==null||controller.scene().emperorArena().deflate(2,0,2).contains(position().add(step)))move(MoverType.SELF,step);
    }
    private void stopWalking(){
        if(Set.of("walk","run","walk_back").contains(animation())){entityData.set(STOP_PHASE,(float)seconds(0));locomotion="idle";play(animation()+"_to_idle");}
    }
    void stepDuringInterval(Vec3 step){
        if(step.lengthSqr()<1e-8)return;
        var destination=standingPosition(position().add(step));
        if(destination==null)return;
        var controller=tomb();
        if(controller!=null&&!controller.scene().emperorArena().deflate(2,0,2).contains(destination))return;
        Vec3 travel=destination.subtract(position());
        if(level().noCollision(this,getBoundingBox().deflate(0,.01,0).expandTowards(travel)))move(MoverType.SELF,travel);
    }
    void pauseIntervalGait(){entityData.set(INTERVAL_WEIGHT,Math.max(0,entityData.get(INTERVAL_WEIGHT)-.25F));}
    void walkDuringCooldown(LivingEntity target,boolean moving){
        Vec3 forward=target.position().subtract(position()).multiply(1,0,1).normalize();
        double distance=target.position().subtract(position()).horizontalDistance();
        boolean retreat=distance<=12||entityData.get(INTERVAL_DIRECTION)!=2&&entityData.get(INTERVAL_WEIGHT)>0&&distance<12.3;
        if(retreatSide==0)retreatSide=random.nextBoolean()?1:-1;
        int direction=retreat?retreatSide:2;
        if(direction!=entityData.get(INTERVAL_DIRECTION)&&entityData.get(INTERVAL_WEIGHT)>0){pauseIntervalGait();return;}
        entityData.set(INTERVAL_DIRECTION,direction);
        float weight=Mth.clamp(entityData.get(INTERVAL_WEIGHT)+(moving?.25F:-.25F),0,1);
        Vec3 step=retreat?new Vec3(-forward.z,0,forward.x).scale(retreatSide).subtract(forward).normalize()
                .scale(3.5/16/.6/20*2*ShiHuangPose.MODEL_SCALE):forward.scale(.15);
        Vec3 before=position();stepDuringInterval(step.scale(weight));
        if(position().distanceToSqr(before)<1e-8){pauseIntervalGait();return;}
        entityData.set(INTERVAL_WEIGHT,weight);
        entityData.set(INTERVAL_GAIT,entityData.get(INTERVAL_GAIT)+weight/20);
    }
    double stepDuringSkill(double distance){
        if(distance==0)return 0;
        Vec3 step=getLookAngle().multiply(1,0,1).normalize().scale(distance),destination=position().add(step);
        var controller=tomb();
        if(controller!=null&&!controller.scene().emperorArena().deflate(2,0,2).contains(destination))return 0;
        var body=getBoundingBox().deflate(0,.01,0);
        if(!level().noCollision(this,body.expandTowards(step)))return 0;
        var floor=net.minecraft.core.BlockPos.containing(destination.add(0,-.1,0));
        if(!level().getBlockState(floor).isFaceSturdy(level(),floor,net.minecraft.core.Direction.UP))return 0;
        Vec3 before=position();move(MoverType.SELF,step);return position().distanceTo(before);
    }
    boolean approachTarget(LivingEntity target,double stopDistance,int remainingTicks){
        if(target instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()||!p.isAlive()))return false;
        Vec3 from=position(),delta=target.position().subtract(from).multiply(1,0,1);
        double distance=delta.length();if(distance<=stopDistance||distance>6||!hasLineOfSight(target))return false;
        Vec3 horizontal=from.add(delta.normalize().scale(distance-stopDistance));
        Vec3 destination=standingPosition(horizontal);
        if(destination==null)return false;
        var controller=tomb();
        if(controller!=null&&!controller.scene().emperorArena().deflate(1,0,1).contains(destination))return false;
        // Sweep the same standing AABB used for body collision.
        int samples=(int)Math.ceil(from.distanceTo(destination)*4);
        for(int i=1;i<=samples;i++){
            Vec3 point=from.lerp(destination,i/(double)samples);
            var body=getBoundingBox().move(point.subtract(position())).deflate(0,.01,0);
            if(!level().noCollision(this,body))return false;
        }
        rapidStep=from.distanceTo(destination)/Math.max(1,remainingTicks);
        reposition(destination,target.position(),false);
        return true;
    }
    boolean blinkBeside(ServerPlayer target){
        Vec3 toward=position().subtract(target.position()).multiply(1,0,1).normalize();
        if(toward.lengthSqr()<1e-8)toward=new Vec3(0,0,-1);
        for(int turn:new int[]{0,1,-1,2,-2,3,-3,4}){
            Vec3 point=standingPosition(target.position().add(toward.yRot((float)(turn*Math.PI/4)).scale(1.8)));
            if(point==null)continue;
            var controller=tomb();
            if(controller!=null&&!controller.scene().emperorArena().deflate(1,0,1).contains(point))continue;
            if(level().noCollision(this,getBoundingBox().move(point.subtract(position())).deflate(0,.01,0))){
                reposition(point,target.position(),true);return true;
            }
        }
        return false;
    }
    Vec3 standingPosition(Vec3 point){
        var hit=level().clip(new net.minecraft.world.level.ClipContext(point.add(0,.6,0),point.add(0,-1.1,0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getDirection()==net.minecraft.core.Direction.UP?hit.getLocation():null;
    }
    void reposition(Vec3 destination,Vec3 facing,boolean ghost){
        if(ghost){
            entityData.set(BLINK_FROM,position().toVector3f());entityData.set(BLINK_TO,destination.toVector3f());
            entityData.set(BLINK_YAW,yBodyRot);entityData.set(BLINK_CLIP,animation());entityData.set(BLINK_POSE,(float)seconds(0));
            entityData.set(BLINK_START,level().getGameTime());
        }
        if(ghost){stopRapidMovement();teleportTo(destination.x,destination.y,destination.z);}
        else rapidDestination=destination;
        face(facing,180);getNavigation().stop();
    }
    @Override public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> values){
        super.onSyncedDataUpdated(values);
        if(level().isClientSide&&values.stream().anyMatch(v->v.id()==BLINK_START.id())&&blinkTicks(0)>=0&&blinkTicks(0)<BLINK_GHOST_TICKS){
            Vec3 to=blinkTo();moveTo(to.x,to.y,to.z,getYRot(),getXRot());
            lerpSteps=0;
        }
    }
    @Override public void travel(Vec3 input){
        if(!level().isClientSide&&pressure.airborne()){setDeltaMovement(Vec3.ZERO);return;}
        if(rapidDestination!=null){move(MoverType.SELF,getDeltaMovement());return;}
        super.travel(input);
    }
    void stopRapidMovement(){rapidDestination=null;setDeltaMovement(Vec3.ZERO);hasImpulse=true;}
    int attackCooldownTicks(){return (int)Math.max(0,nextAttack-level().getGameTime());}
    private void beginAttackCooldown(){
        nextAttack=level().getGameTime()+ATTACK_COOLDOWN_TICKS;pressure.beginCooldown();
        retreatSide=random.nextBoolean()?1:-1;entityData.set(INTERVAL_GAIT,0F);entityData.set(INTERVAL_WEIGHT,0F);
    }
    @Override protected boolean hurtSelectedPart(int part,DamageSource source,float amount){
        if(dying())return false;
        if(!level().isClientSide&&!isNoAi()&&entityData.get(FROZEN_AT)<0&&amount>0&&!isInvulnerableTo(source)
                &&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))combat.tryFeintCounter(source);
        // Raised sword hold; the opening lift and recovery remain vulnerable.
        if(animation().equals("interval_guard")
                &&!level().isClientSide&&!isNoAi()&&entityData.get(FROZEN_AT)<0&&amount>0
                &&source.getDirectEntity()!=null&&!isInvulnerableTo(source)
                &&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
            boolean blocking=seconds(0)>=.25&&seconds(0)<=.8;
            if(++guardHits>=3){
                if(source.getEntity()!=null)face(source.getEntity().position(),180);
                play("counter_guard_sweep");
            }
            if(blocking){sound(SoundEvents.ANVIL_LAND,2,1.6F);return false;}
        }
        float guarded=combat.guard(source,amount);
        if(hasCombatGlow(0)&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))guarded*=.5F;
        if(guarded>0&&!level().isClientSide&&!isNoAi()&&entityData.get(FROZEN_AT)<0&&!isInvulnerableTo(source)
                &&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                &&(animation().equals("idle")||animation().startsWith("interval_"))
                &&(pressure.active()||level().getGameTime()<nextAttack)&&source.getEntity() instanceof LivingEntity attacker
                &&pressure.onHurt(attacker))return false;
        return guarded>0&&super.hurtSelectedPart(part,source,guarded);
    }
    @Override public void die(DamageSource source){
        if(level().isClientSide||dying())return;
        entityData.set(ENTRANCE_START,-1L);setInvisible(false);noPhysics=false;
        combat.stop();entityData.set(DYING,true);entityData.set(FROZEN_AT,-1L);setHealth(0);setNoAi(true);setNoGravity(true);
        getNavigation().stop();setDeltaMovement(Vec3.ZERO);play("death");
        bossBar.removeAllPlayers();
    }
    @Override protected void tickDeath(){/* The authored finale, not vanilla's 20-tick removal. */}
    private void tickFinale(){
        setDeltaMovement(Vec3.ZERO);hurtTime=0;
        int t=elapsedTicks();
        if(t==FLOOR_TICK)sound(SoundEvents.PLAYER_BIG_FALL,3,.8F);
        if(t>=VANISH_TICK&&!smokeSent){smokeSent=true;var frame=ShiHuangPose.sample("death",5,false);
            for(String bone:List.of("head","chest","pelvis","leg_l_lower","leg_r_lower")){
                Vec3 p=bonePoint(frame,bone,ShiHuangCombat.PIVOTS.get(bone));
                var glow=icu.icuqalt10.panlingre.network.TombLightPayload.orb(p.add(0,.3,0),new Vec3(0,.03,0),0xFFF0C2,1.7F,24);
                for(var player:arenaPlayers())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,glow);
            }
        }
        if(t>=REWARD_TICK&&!settled){settled=true;var controller=tomb();if(controller!=null)controller.completeEmperor(this);}
    }
    public boolean rewardReady(){return dying()&&elapsedTicks()>=REWARD_TICK;}
    public void releaseRemnant(){settled=true;bossBar.removeAllPlayers();combat.stop();}
    @Override public boolean isAttackable(){return !dying();}
    @Override public boolean isPickable(){return isAlive()&&!dying();}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isMultipartEntity(){return true;}
    @Override public PartEntity<?>[] getParts(){return parts;}
    @Override protected Entity[] multipartParts(){return new Entity[]{this};}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return hurtSelectedPart(0,source,amount);}
    @Override public void setId(int id){super.setId(id);if(parts!=null)for(int i=0;i<parts.length;i++)if(parts[i]!=null)parts[i].setId(id+i+1);}
    public void updateParts(ShiHuangPose.Frame frame,float yaw,Vec3 origin){
        parts[0].setOrientedBox(ShiHuangCombat.swordBox(frame,yaw,origin));
    }
    @Override public void whenFroozen(){if(!level().isClientSide&&!dying()&&entityData.get(FROZEN_AT)<0){combat.interrupt();play("idle");entityData.set(FROZEN_AT,level().getGameTime());setDeltaMovement(Vec3.ZERO);}}
    @Override public void whenUnFroozen(){if(!level().isClientSide&&entityData.get(FROZEN_AT)>=0){double paused=actionClock(0);entityData.set(START,entityData.get(START)+level().getGameTime()-entityData.get(FROZEN_AT));entityData.set(FROZEN_AT,-1L);combat.resumeClock(actionClock(0)-paused);}}
    @Override public void remove(RemovalReason reason){combat.stop();bossBar.removeAllPlayers();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putString("EmperorAnimation",animation());tag.putLong("EmperorStart",entityData.get(START));tag.putLong("EmperorFrozen",entityData.get(FROZEN_AT));
        tag.putBoolean("EmperorDying",dying());tag.putBoolean("EmperorEnhanced",enhanced());tag.putBoolean("EmperorSmoke",smokeSent);tag.putBoolean("EmperorSettled",settled);tag.putBoolean("EmperorUltimate",combat.ultimateUsed());
        tag.putLong("EmperorRageStart",entityData.get(RAGE_START));tag.putInt("EmperorMana",mana());
        tag.putFloat("EmperorStopPhase",entityData.get(STOP_PHASE));
        tag.putLong("EmperorEntrance",entityData.get(ENTRANCE_START));var origin=entranceOrigin();
        tag.putDouble("EmperorEntranceX",origin.x);tag.putDouble("EmperorEntranceY",origin.y);tag.putDouble("EmperorEntranceZ",origin.z);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);String name=tag.getString("EmperorAnimation");if(ShiHuangPose.animationNames().contains(name))entityData.set(ANIMATION,name);
        entityData.set(START,tag.getLong("EmperorStart"));entityData.set(FROZEN_AT,tag.contains("EmperorFrozen")?tag.getLong("EmperorFrozen"):-1L);
        entityData.set(STOP_PHASE,tag.getFloat("EmperorStopPhase"));
        entityData.set(RAGE_START,tag.contains("EmperorRageStart")?tag.getLong("EmperorRageStart"):-1L);
        entityData.set(MANA,Mth.clamp(tag.getInt("EmperorMana"),0,MAX_MANA));
        if(enraged())bossBar.setColor(BossEvent.BossBarColor.RED);
        locomotion=name.startsWith("idle_to_")?name.substring(8):"idle";
        entityData.set(DYING,tag.getBoolean("EmperorDying"));setEnhanced(tag.getBoolean("EmperorEnhanced"));smokeSent=tag.getBoolean("EmperorSmoke");settled=tag.getBoolean("EmperorSettled");combat.restoreUltimate(tag.getBoolean("EmperorUltimate"));
        entityData.set(ENTRANCE_START,tag.contains("EmperorEntrance")?tag.getLong("EmperorEntrance"):-1L);
        setEntranceOrigin(new Vec3(tag.getDouble("EmperorEntranceX"),tag.getDouble("EmperorEntranceY"),tag.getDouble("EmperorEntranceZ")));
        // Command paths are transient; loading a saved command must not resume an uninitialised joint attack.
        if(ShiHuangCombat.ACTIONS.containsKey(name)&&!dying()){entityData.set(ANIMATION,"idle");entityData.set(START,level().getGameTime());}
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new WorldTimeAnimationController<>(this,"emperor",()->new WorldTimeAnimationController.Playback(animation(),entityData.get(START),true,1),state->entityData.get(START)+seconds(state.getPartialTick())*20));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
