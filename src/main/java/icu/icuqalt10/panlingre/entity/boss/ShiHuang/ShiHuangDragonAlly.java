package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonPose;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** 御龙实体与始皇共享动作时钟；大招在地面蓄力，墓龙在身后悬停。 */
final class ShiHuangDragonAlly {
    private final ShiHuangEntity boss;
    private GraveDragonEntity dragon;
    private double spawned,comboUntil;
    private int patrolDelay=-1,patrolWindup=-1;
    private long patrolTick=Long.MIN_VALUE;
    private boolean patrolFireball;
    private java.util.List<Vec3> patrolPoints=java.util.List.of();
    private Vec3 ground,depart,mountPosition,ultimateFrom,ultimateBehind;
    private float ultimateYaw,approachYaw;
    private float departYaw;
    private String joint="";
    private double jointStart;
    private int lastJointTick=-31;
    private Vec3 jointTarget,jointDirection,jointOrigin,jointFrom,jointNear,chargeRoot,chargeAimRoot;
    private float jointYaw, jointFromYaw,chargeAimYaw;
    private long chargeStopped=-1;
    private double returnStarted=-1,orbitStarted=-1;
    private boolean rising;
    private Vec3 returnHead,returnTarget;
    private float returnYaw;
    private double returnAngle,orbitAngle;
    private java.util.List<Vec3> thunderPoints=java.util.List.of();
    private final java.util.Set<java.util.UUID> jointHits=new java.util.HashSet<>();
    ShiHuangDragonAlly(ShiHuangEntity boss){this.boss=boss;}
    private double now(){return boss.actionClock(0);}
    boolean ready(){return dragon!=null&&returnStarted<0&&now()-spawned>=Math.round((GraveDragonPose.duration("ally_emerge")+GraveDragonPose.duration("ally_emerge_exit"))*20);}
    Vec3 center(){return boss.tomb()==null?ground==null?boss.position():ground:boss.tomb().scene().emperorCombatPosition();}
    private void ensure(){
        if(dragon!=null&&!dragon.isRemoved())return;
        dragon=ModEntities.GRAVE_DRAGON.get().create(boss.level());
        if(dragon==null)throw new IllegalStateException("Cannot create emperor dragon");
        mountPosition=null;
        if(ground==null)ground=boss.position();
        dragon.moveTo(boss.position(),boss.getYRot(),0);dragon.yBodyRot=dragon.yHeadRot=boss.getYRot();
        dragon.setEmperorClock(boss);dragon.beginEmperorAllyAnimation("ally_emerge");boss.level().addFreshEntity(dragon);
        var session=InstanceManager.sessionFor(boss);if(session!=null)InstanceManager.ownEntity(session,dragon);
        spawned=now();
    }
    void tick(){
        ensure();if(boss.animation().equals("ultimate_full"))return;
        if(returnStarted>=0){tickReturn();return;}
        if(boss.animation().startsWith("ally_")&&!joint.isEmpty()){tickCombo();return;}
        if(!joint.isEmpty()){beginReturn(false);tickReturn();return;}
        long age=(long)Math.floor(now()-spawned);
        int emergeTicks=(int)Math.round(GraveDragonPose.duration("ally_emerge")*20);
        if(age<emergeTicks){
            dragon.setYRot(boss.yBodyRot);dragon.yBodyRot=dragon.yHeadRot=boss.yBodyRot;
            dragon.setPos(boss.position());dragon.setDeltaMovement(Vec3.ZERO);
            depart=dragon.position();departYaw=boss.yBodyRot;return;
        }
        long flightAge=age-emergeTicks;
        int exitTicks=(int)Math.round(GraveDragonPose.duration("ally_emerge_exit")*20);
        Vec3 center=center();double angle=orbitStarted>=0?orbitAngle+(now()-orbitStarted)*.013:Math.toRadians(departYaw)+Math.max(0,flightAge-exitTicks)*.013,radius=28;
        Vec3 target=center.add(Math.cos(angle)*radius,36,Math.sin(angle)*radius);
        if(flightAge<exitTicks){
            dragon.beginEmperorAllyAnimation("ally_emerge_exit");
            double u=smooth((double)flightAge/exitTicks);
            float yaw=departYaw;
            dragon.setYRot(yaw);dragon.yBodyRot=dragon.yHeadRot=yaw;
            // The exit clip follows the head's route in world space; its root translation
            // is baked against this fixed final offset, not the moving mount bone.
            Vec3 flightMount=GraveDragonPose.box(GraveDragonPose.sample("ally_orbit",0,false),"shihuang_mount",
                    new float[]{0,0,0,0,155.5F/16,-3F/16},GraveDragonPose.modelToEntity(yaw,0,dragon.getScale()),Vec3.ZERO).center;
            dragon.setPos((depart==null?boss.position():depart).lerp(target.subtract(flightMount),u));
            mountPosition=dragon.position().add(dragon.emperorMountOffset());return;
        }
        if(now()>=comboUntil&&patrolWindup<0)dragon.beginEmperorAllyAnimation("ally_orbit");
        Vec3 current=mountPosition==null?dragon.position().add(dragon.emperorMountOffset()):mountPosition;
        // +X -> +Z is clockwise from above. The tangent faces yaw +angle, not -angle.
        moveMount(orbitStarted>=0?target:current.lerp(target,.15),(float)Math.toDegrees(angle));
        tickPatrolAttack();
    }
    private void tickPatrolAttack(){
        long tick=boss.level().getGameTime();if(patrolTick==tick)return;patrolTick=tick;
        if(patrolDelay<0)patrolDelay=200+boss.getRandom().nextInt(201);
        if(patrolDelay>0)patrolDelay--;
        if(patrolWindup<0&&patrolDelay==0){
            var target=boss.arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())
                    .min(java.util.Comparator.comparingDouble(net.minecraft.server.level.ServerPlayer::getHealth)
                            .thenComparingInt(net.minecraft.server.level.ServerPlayer::getArmorValue)).orElse(null);
            if(target==null)return;
            patrolDelay=200+boss.getRandom().nextInt(201);patrolWindup=0;patrolFireball=boss.getRandom().nextBoolean();
            if(patrolFireball)patrolPoints=java.util.List.of(target.getEyePosition());
            else {
                var points=new java.util.ArrayList<Vec3>();points.add(floor(target.position()));
                double angle=boss.getRandom().nextDouble()*Math.PI*2;
                for(int i=0;i<2;i++){
                    double radius=12+boss.getRandom().nextDouble()*20,theta=angle+i*Math.PI;
                    points.add(floor(center().add(Math.cos(theta)*radius,0,Math.sin(theta)*radius)));
                }
                patrolPoints=java.util.List.copyOf(points);dragon.beginEmperorAllyAnimation("ally_thunder");
                for(Vec3 point:patrolPoints)send(new icu.icuqalt10.panlingre.network.TombLightPayload(1,point.add(0,.06,0),new Vec3(1,0,0),0xBCA2FF,.08F,24));
            }
        }
        if(patrolWindup<0)return;
        Vec3 mouth=dragon.posedPart(11,dragon.animationSeconds(0)).center;
        send(icu.icuqalt10.panlingre.network.TombLightPayload.orb(mouth.add(0,patrolFireball?0:1,0),Vec3.ZERO,patrolFireball?0xFF5825:0xD8BFFF,1.2F,2));
        if(++patrolWindup<24)return;
        if(patrolFireball){
            var fireball=new icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonFireballEntity(
                    boss.level(),dragon,patrolPoints.getFirst().subtract(mouth),10,false);
            fireball.setPos(mouth);boss.level().addFreshEntity(fireball);
            icu.icuqalt10.panlingre.instance.shihuang.ShiHuangBossSounds.play(dragon,boss.level(),mouth.x,mouth.y,mouth.z,
                    SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.HOSTILE,4,.8F);
        }else for(Vec3 point:patrolPoints)lightning(point.add(0,25,0),point.y);
        patrolWindup=-1;patrolPoints=java.util.List.of();dragon.beginEmperorAllyAnimation("ally_orbit");
    }
    private void moveMount(Vec3 mount,float yaw){
        mountPosition=mount;
        dragon.setYRot(yaw);dragon.yBodyRot=dragon.yHeadRot=yaw;
        dragon.setPos(mount.subtract(dragon.emperorMountOffset()));dragon.setDeltaMovement(Vec3.ZERO);
    }
    private void beginReturn(boolean rise){
        returnHead=dragon.posedPart(11,dragon.animationSeconds(0)).center;
        returnYaw=dragon.yBodyRot;returnStarted=now();rising=rise;joint="";
        dragon.beginEmperorAllyAnimation(rise?"ally_charge_rise":"ally_orbit_return");
        if(!rise){
            Vec3 middle=center();returnAngle=Math.atan2(returnHead.z-middle.z,returnHead.x-middle.x)+.35;
            float yaw=(float)Math.toDegrees(returnAngle);
            Vec3 mount=GraveDragonPose.box(GraveDragonPose.sample("ally_orbit",0,false),"shihuang_mount",
                    new float[]{0,0,0,0,155.5F/16,-3F/16},GraveDragonPose.modelToEntity(yaw,0,dragon.getScale()),Vec3.ZERO).center;
            returnTarget=middle.add(Math.cos(returnAngle)*28,36,Math.sin(returnAngle)*28)
                    .add(dragon.sceneHeadOffset("ally_orbit",0,yaw)).subtract(mount);
        }
    }
    private void tickReturn(){
        double u=Math.min(1,(now()-returnStarted)/48.0);
        float yaw=rising?returnYaw:net.minecraft.util.Mth.rotLerp((float)smooth(u),returnYaw,(float)Math.toDegrees(returnAngle));
        dragon.setYRot(yaw);dragon.yBodyRot=dragon.yHeadRot=yaw;
        Vec3 head;
        if(rising)head=returnHead.add(0,36*smooth(u),0);
        else {
            Vec3 tangent=new Vec3(-Math.sin(returnAngle),0,Math.cos(returnAngle)).scale(28*.013*48);
            head=returnHead.lerp(returnTarget,smooth(u)).add(tangent.scale(u*u*(u-1)));
        }
        dragon.setPos(head.subtract(dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(dragon.position())));
        double floor=center().y+(rising?.5:8)-dragon.sceneLowestOffset();
        if(dragon.getY()<floor)dragon.setPos(dragon.getX(),floor,dragon.getZ());
        dragon.setDeltaMovement(Vec3.ZERO);mountPosition=dragon.position().add(dragon.emperorMountOffset());
        if(u>=1){
            if(rising)beginReturn(false);
            else {returnStarted=-1;orbitStarted=now();orbitAngle=returnAngle;comboUntil=orbitStarted;dragon.beginEmperorAllyAnimation("ally_orbit");}
        }
    }
    void beginCombo(String name){
        ensure();patrolWindup=-1;patrolPoints=java.util.List.of();returnStarted=-1;joint=name.substring(5,8);jointStart=now()+30;lastJointTick=-31;jointHits.clear();chargeStopped=-1;
        jointFromYaw=dragon.yBodyRot;
        aimFormation();
        jointFrom=dragon.posedPart(11,dragon.animationSeconds(0)).center;
        chargeRoot=chargeAimRoot=null;dragon.beginEmperorAllyAnimation(joint.equals("d02")?"ally_coil_approach":"ally_command_approach");
        comboUntil=jointStart+(joint.equals("d01")?200:joint.equals("d02")?60+120/GraveDragonEntity.ALLY_CHARGE_SPEED:53);
    }
    private void aimFormation(){
        jointOrigin=boss.position();jointYaw=boss.yBodyRot;
        jointDirection=boss.getLookAngle().multiply(1,0,1).normalize();
        jointTarget=boss.getTarget()==null?jointOrigin.add(jointDirection.scale(8)):boss.getTarget().position().add(0,1,0);
        jointNear=jointOrigin.add(jointDirection.scale(joint.equals("d02")?-7:joint.equals("d03")?-18:-14)).add(0,joint.equals("d02")?5:joint.equals("d03")?28:30,0);
        if(joint.equals("d02")){
            Vec3 side=new Vec3(-jointDirection.z,0,jointDirection.x);
            Vec3 shift=dragon.sceneHeadOffset("ally_combo_coil",3,jointYaw).subtract(dragon.sceneHeadOffset("ally_combo_coil",0,jointYaw));
            jointNear=jointNear.subtract(side.scale(shift.dot(side)));
        }
    }
    private void tickCombo(){
        int t=(int)Math.floor(now()-jointStart);
        if(t<=0)aimFormation();
        if(joint.equals("d03")&&t>=7&&lastJointTick<7){
            var target=boss.arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())
                    .min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
            if(target!=null){boss.setTarget(target);boss.face(target.position(),180);}
            aimFormation();
        }
        // The 30-tick approach plus 53 joint ticks is the emperor's pointing frame (83).
        if(joint.equals("d02")&&t>=53&&lastJointTick<53){
            chargeAimRoot=chargeRoot;chargeAimYaw=dragon.yBodyRot;
            var target=boss.arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())
                    .min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
            if(target!=null){boss.setTarget(target);boss.face(target.position(),180);}
            aimFormation();
            chargeRoot=jointNear.subtract(dragon.sceneHeadOffset("ally_combo_coil",0,jointYaw));
            chargeRoot=chargeRoot.add(0,Math.max(0,jointOrigin.y+.5-(chargeRoot.y+dragon.sceneLowestOffset("ally_combo_coil",0))),0);
        }
        if(t>=0&&lastJointTick<0){
            dragon.beginEmperorAllyAnimation(switch(joint){case "d01"->"ally_combo_thunder";case "d02"->"ally_combo_coil";default->"ally_combo_breath";},jointStart);
            if(joint.equals("d02")){
                Vec3 head=dragon.sceneHeadOffset("ally_combo_coil",0,jointYaw);
                chargeRoot=jointNear.subtract(head);
                chargeRoot=chargeRoot.add(0,Math.max(0,jointOrigin.y+.5-(chargeRoot.y+dragon.sceneLowestOffset("ally_combo_coil",0))),0);
            }
        }
        double u=smooth((t+30)/30.0);float yaw=net.minecraft.util.Mth.rotLerp((float)u,jointFromYaw,jointYaw);
        double chargeAim=smooth((now()-jointStart-53)/7.0);
        if(joint.equals("d02")&&chargeAimRoot!=null)
            yaw=net.minecraft.util.Mth.rotLerp((float)chargeAim,chargeAimYaw,jointYaw);
        dragon.setYRot(yaw);dragon.yBodyRot=dragon.yHeadRot=yaw;
        if(joint.equals("d02")&&t>=0){
            if(t>=60&&lastJointTick<60)dragon.beginEmperorAllyAnimation("ally_combo_charge",jointStart+60);
            Vec3 root=chargeRoot;
            if(chargeAimRoot!=null&&t<60){
                // Resolve the pointing-frame retarget over the remaining coil, around the head.
                double seconds=dragon.animationSeconds(0);String clip=dragon.animation();
                Vec3 from=chargeAimRoot.add(dragon.sceneHeadOffset(clip,seconds,chargeAimYaw));
                Vec3 to=chargeRoot.add(dragon.sceneHeadOffset(clip,seconds,jointYaw));
                root=from.lerp(to,chargeAim).subtract(dragon.sceneHeadOffset(clip,seconds,yaw));
            }
            dragon.setPos(root);
            double floor=jointOrigin.y+.5-dragon.sceneLowestOffset();
            if(dragon.getY()<floor)dragon.setPos(dragon.getX(),floor,dragon.getZ());
            if(t>=60&&chargeStopped<0&&chargeReachesWall()){
                dragon.holdEmperorAllyPoseAt(Math.max(0,dragon.animationSeconds(0)-boss.actionSpeed()*.05*GraveDragonEntity.ALLY_CHARGE_SPEED));
                chargeStopped=t;boss.sound(SoundEvents.GENERIC_EXPLODE.value(),3,.7F);
                beginReturn(true);return;
            }
        }else{
            Vec3 head=dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(dragon.position());
            Vec3 point=jointFrom.lerp(jointNear,u);
            double clearance=joint.equals("d02")?.5:joint.equals("d03")?5:8;
            point=new Vec3(point.x,Math.max(point.y,jointOrigin.y+clearance+head.y-dragon.sceneLowestOffset()),point.z);
            dragon.setPos(point.subtract(head));
        }
        dragon.setDeltaMovement(Vec3.ZERO);mountPosition=dragon.position().add(dragon.emperorMountOffset());
        if(t>=0&&now()<comboUntil)for(int crossed=Math.max(0,lastJointTick+1);crossed<=t;crossed++)tickJoint(crossed);
        lastJointTick=t;
    }
    private boolean chargeReachesWall(){
        var bounds=boss.tomb()==null?new net.minecraft.world.phys.AABB(jointOrigin.add(-48,-10,-48),jointOrigin.add(48,60,48)):boss.tomb().scene().emperorArena();
        var head=dragon.posedPart(11,dragon.animationSeconds(0));
        for(Vec3 corner:head.corners()){
            if(corner.x<bounds.minX+.25||corner.x>bounds.maxX-.25||corner.z<bounds.minZ+.25||corner.z>bounds.maxZ-.25)return true;
            if(corner.y>jointOrigin.y+.5&&boss.level().getBlockState(net.minecraft.core.BlockPos.containing(corner)).isSolidRender(boss.level(),net.minecraft.core.BlockPos.containing(corner)))return true;
        }return false;
    }
    double duration(){return boss.animation().startsWith("ally_")?boss.seconds(0)+Math.max(0,comboUntil-now())/20.0:0;}
    private void tickJoint(long t){
        var level=(net.minecraft.server.level.ServerLevel)boss.level();
        if(joint.equals("d01")){
            for(int round=0;round<3;round++){
                if(t==20+round*64){
                    var target=boss.arenaPlayers().stream().filter(p->!p.isCreative()).min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
                    Vec3 center=target==null?jointTarget:target.position();var points=new java.util.ArrayList<Vec3>();points.add(floor(center));
                    for(int i=0;i<120&&points.size()<11;i++){
                        double angle=boss.getRandom().nextDouble()*Math.PI*2,r=30*Math.sqrt(boss.getRandom().nextDouble());
                        Vec3 point=floor(center.add(Math.cos(angle)*r,0,Math.sin(angle)*r));
                        if(points.stream().anyMatch(p->p.subtract(point).multiply(1,0,1).lengthSqr()<100))continue;
                        if(boss.tomb()!=null&&!boss.tomb().scene().emperorArena().contains(point.add(0,.1,0)))continue;
                        points.add(point);
                    }
                    thunderPoints=points;
                    for(Vec3 point:points)send(new icu.icuqalt10.panlingre.network.TombLightPayload(1,point.add(0,.06,0),new Vec3(5,0,0),0xBCA2FF,.08F,20).onClock(boss,0));
                }
                if(t==40+round*64){
                    jointHits.clear();boss.sound(SoundEvents.LIGHTNING_BOLT_THUNDER,3,1);
                    for(Vec3 point:thunderPoints){
                        send(new icu.icuqalt10.panlingre.network.TombLightPayload(5,point.add(0,25,0),point,0xBCA2FF,.2F,10));
                        for(var player:boss.arenaPlayers())if(!player.isCreative()&&!jointHits.contains(player.getUUID())&&icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonMythic.areaHits(dragon,point,5,player)){
                            if(player.hurt(boss.damageSources().mobAttack(boss),ShiHuangCombat.skillDamage(30)))jointHits.add(player.getUUID());
                        }
                    }
                }
            }
        }else if(joint.equals("d02")){
            if(t>=60&&chargeStopped<0)for(int sample=0;sample<=6;sample++)for(int part=0;part<=21;part++){
                var box=dragon.posedPart(part,Math.max(0,dragon.animationSeconds(0)+(-.05+sample/120.0)*boss.actionSpeed()*GraveDragonEntity.ALLY_CHARGE_SPEED));
                for(var player:boss.arenaPlayers())if(!player.isCreative()&&box.intersects(player.getBoundingBox())){
                    if(player.hurt(boss.damageSources().mobAttack(boss),ShiHuangCombat.skillDamage(35))){
                        player.setDeltaMovement(jointDirection.scale(2.2).add(0,1.1,0));player.hurtMarked=true;
                    }
                }
            }
        }else if(joint.equals("d03")&&t>=10&&t<25){
            var flow=icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath.flowAtPose(dragon,"ground_turn_sweep",t/20.0);
            var jet=flow.geometry();
            if(t==10)icu.icuqalt10.panlingre.instance.shihuang.ShiHuangBossSounds.play(dragon,level,jet.mouth().x,jet.mouth().y,jet.mouth().z,
                    icu.icuqalt10.panlingre.init.ModSounds.GRAVE_DRAGON_BREATHS.get("ground_turn_sweep").get(),net.minecraft.sounds.SoundSource.HOSTILE,5,1);
            for(var player:boss.arenaPlayers())if(!player.isCreative()&&icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath.hits(dragon,flow,player)){
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,10,2));
                player.hurt(boss.damageSources().mobAttack(boss),ShiHuangCombat.skillDamage(20));
            }
        }
    }
    private Vec3 floor(Vec3 point){
        var result=boss.level().clip(new net.minecraft.world.level.ClipContext(point.add(0,.5,0),point.add(0,-16,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,boss));
        return result.getType()==net.minecraft.world.phys.HitResult.Type.MISS?new Vec3(point.x,center().y,point.z):result.getLocation();
    }
    private void send(icu.icuqalt10.panlingre.network.TombLightPayload light){for(var viewer:boss.arenaPlayers())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,light);}
    private void lightning(Vec3 from,double floor){
        send(new icu.icuqalt10.panlingre.network.TombLightPayload(icu.icuqalt10.panlingre.network.TombLightPayload.LIGHTNING,from,new Vec3(from.x,floor,from.z),0xBCA2FF,.13F,10));
        boss.sound(SoundEvents.LIGHTNING_BOLT_IMPACT,3,.9F);
        var box=ShiHuangCombat.lineBox(from,new Vec3(from.x,floor,from.z),1);
        for(var p:boss.arenaPlayers())if(!p.isCreative()&&box.intersects(p.getBoundingBox()))p.hurt(boss.damageSources().mobAttack(boss),ShiHuangCombat.skillDamage(18));
    }
    void beginUltimate(){
        ensure();patrolWindup=-1;patrolPoints=java.util.List.of();ground=boss.position();returnStarted=-1;joint="";
        Vec3 facing=boss.getTarget()==null?ground.add(boss.getLookAngle()):boss.getTarget().position();
        boss.face(facing,180);
        ultimateYaw=boss.yBodyRot;approachYaw=dragon.yBodyRot;
        ultimateFrom=dragon.posedPart(11,dragon.animationSeconds(0)).center;
        ultimateBehind=ultimateHeadPosition();
        dragon.beginEmperorAllyAnimation("ally_ultimate_coil");
    }
    private Vec3 ultimateHeadPosition(){
        // Charge, orbit and ascent are authored around the emperor on the shared action clock.
        return boss.position().add(dragon.sceneHeadOffset("ally_ultimate_coil",dragon.animationSeconds(0),ultimateYaw));
    }
    void tickUltimate(double t){
        if(dragon==null||dragon.isRemoved()){ensure();dragon.beginEmperorAllyAnimation("ally_ultimate_coil");}
        double u=smooth(t/1.8);
        float yaw=net.minecraft.util.Mth.rotLerp((float)u,approachYaw,ultimateYaw);
        dragon.setYRot(yaw);dragon.yBodyRot=dragon.yHeadRot=yaw;
        ultimateBehind=ultimateHeadPosition();
        Vec3 head=ultimateFrom.lerp(ultimateBehind,u);
        Vec3 offset=dragon.posedPart(11,dragon.animationSeconds(0)).center.subtract(dragon.position());
        Vec3 root=head.subtract(offset);
        dragon.setPos(root.x,Math.max(root.y,ground.y+.5-dragon.sceneLowestOffset()),root.z);
        dragon.setDeltaMovement(Vec3.ZERO);mountPosition=dragon.position().add(dragon.emperorMountOffset());
    }
    void roarUltimate(){
        Vec3 head=dragon.posedPart(11,dragon.animationSeconds(0)).center;
        boss.level().playSound(null,head.x,head.y,head.z,SoundEvents.ENDER_DRAGON_GROWL,net.minecraft.sounds.SoundSource.HOSTILE,5,.65F);
    }
    void finishUltimate(){boss.stopRapidMovement();if(dragon!=null&&!dragon.isRemoved())beginReturn(false);}
    void interrupt(){
        if(patrolWindup>=0&&dragon!=null&&!dragon.isRemoved())dragon.beginEmperorAllyAnimation("ally_orbit");
        patrolWindup=-1;patrolPoints=java.util.List.of();
        if(dragon!=null&&!dragon.isRemoved()&&returnStarted<0
                &&(!joint.isEmpty()||boss.animation().equals("ultimate_full")))beginReturn(false);
    }
    void resumeClock(double ticks){
        if(dragon==null||dragon.isRemoved())return;
        spawned+=ticks;comboUntil+=ticks;jointStart+=ticks;
        if(returnStarted>=0)returnStarted+=ticks;
        if(orbitStarted>=0)orbitStarted+=ticks;
        dragon.shiftEmperorAnimationStart(ticks);
    }
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    void stop(){joint="";returnStarted=orbitStarted=-1;if(dragon!=null){InstanceManager.releaseEntity(dragon);dragon.discard();dragon=null;}}
}
