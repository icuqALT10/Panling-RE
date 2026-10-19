package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import com.google.gson.*;
import icu.icuqalt10.panlingre.skill.SkillCastManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import icu.icuqalt10.panlingre.network.TombLightPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 动作事件表驱动判伤。每个剑窗、每个释放后的范围分别去重。 */
final class ShiHuangCombat {
    static final double MELEE_REACH=3;
    static float skillDamage(float damage){return damage*2;}
    record Window(String element,double from,double to,boolean thrust){}
    record Action(String name,List<Window> windows,List<String> cards,double cardLead,double dissolve,double gather){}
    static final Map<String,Action> ACTIONS=new LinkedHashMap<>();
    static final Map<String,float[]> BOUNDS=new LinkedHashMap<>();
    static final Map<String,Vec3> PIVOTS=new HashMap<>();
    static {
        var geometry=read("shihuang/shihuang_bounds.json").getAsJsonObject();
        geometry.getAsJsonObject("bounds").entrySet().forEach(e->{float[] v=new float[6];for(int i=0;i<6;i++)v[i]=e.getValue().getAsJsonArray().get(i).getAsFloat();BOUNDS.put(e.getKey(),v);});
        geometry.getAsJsonObject("pivots").entrySet().forEach(e->PIVOTS.put(e.getKey(),vector(e.getValue())));
        for(var value:read("shihuang/shihuang_actions.json").getAsJsonArray()){
            var a=value.getAsJsonObject();List<Window> windows=new ArrayList<>();List<String> cards=new ArrayList<>();
            for(var v:a.getAsJsonArray("sword_windows")){var w=v.getAsJsonObject();windows.add(new Window(w.get("element").getAsString(),w.get("from").getAsDouble(),w.get("to").getAsDouble(),w.has("stroke")&&w.get("stroke").getAsString().equals("W")));}
            a.getAsJsonArray("card_elements").forEach(v->cards.add(v.getAsString()));
            double lead=a.has("card_lead_seconds")?a.get("card_lead_seconds").getAsDouble():0;
            var cardEvents=a.has("card_events")?a.getAsJsonObject("card_events"):null;
            String name=a.get("clip").getAsString();ACTIONS.put(name,new Action(name,List.copyOf(windows),List.copyOf(cards),lead,
                    cardEvents==null?0:cardEvents.get("dissolve").getAsDouble(),cardEvents==null?0:cardEvents.get("gather_complete").getAsDouble()));
        }
    }
    private static JsonElement read(String path){
        var stream=ShiHuangCombat.class.getClassLoader().getResourceAsStream("assets/panlingre/"+path);
        if(stream==null)throw new IllegalStateException("Missing emperor resource: "+path);
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader);}catch(java.io.IOException e){throw new IllegalStateException(path,e);}
    }
    private static Vec3 vector(JsonElement v){var a=v.getAsJsonArray();return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());}
    private final ShiHuangEntity boss;
    private Action action;
    private ServerPlayer watchTarget;
    private boolean counterStuns;
    private boolean watchUsed;
    private final Set<String> events=new HashSet<>();
    private final Map<Integer,Set<UUID>> hits=new HashMap<>();
    private final List<Effect> effects=new ArrayList<>();
    private Vec3 skillOrigin, skillTarget, skillDirection;
    private boolean ultimateUsed;
    private double positioningTravel,lastTime;
    private final Map<String,String> previous=new HashMap<>();
    private final ShiHuangDragonAlly ally;
    private record Effect(Vec3 from,Vec3 to,String element,long start,int duration,double radius,float damage,int shape,Vec3 axis,Set<UUID> hit){}
    ShiHuangCombat(ShiHuangEntity boss){this.boss=boss;ally=new ShiHuangDragonAlly(boss);}
    boolean ultimateUsed(){return ultimateUsed;}
    void finish(){if(boss.animation().equals("ultimate_full"))ally.finishUltimate();}
    double duration(){return Math.max(ally.duration(),Math.max(ShiHuangPose.duration(boss.animation()),effects.stream()
            .filter(e->e.shape!=TombLightPayload.WOOD_SURGE).mapToDouble(e->(e.start+(e.shape==TombLightPayload.FALLING_SWORD?ShiHuangSkillShape.SWORD_DROP_END:e.duration)+1)/20.0).max().orElse(0)));}
    void restoreUltimate(boolean used){ultimateUsed=used;}
    void begin(String name){boss.stopRapidMovement();watchTarget=null;action=ACTIONS.get(name);if(action!=null&&!name.startsWith("counter_"))watchUsed=false;events.clear();hits.clear();effects.removeIf(e->e.shape!=TombLightPayload.WOOD_SURGE);positioningTravel=0;lastTime=0;if((name.startsWith("skill_")||name.startsWith("ally_"))&&boss.mana()>=ShiHuangEntity.SKILL_MANA)boss.spendMana();skillOrigin=skillTarget=skillDirection=null;if(name.equals("ultimate_full")){ultimateUsed=true;ally.beginUltimate();}else if(name.startsWith("ally_"))ally.beginCombo(name);}
    void stop(){watchTarget=null;effects.clear();boss.stopRapidMovement();ally.stop();}
    void interrupt(){watchTarget=null;effects.clear();boss.stopRapidMovement();ally.interrupt();}
    void resumeClock(double ticks){ally.resumeClock(ticks);}
    boolean inSwing(){return action!=null&&action.windows.stream().anyMatch(w->boss.seconds(0)>=w.from&&boss.seconds(0)<=w.to);}
    boolean tracksTarget(){
        if(action!=null&&action.name.equals("counter_guard_sweep"))return false;
        if(action!=null&&action.name.equals("counter_watch_liaotian"))return false;
        if(action==null||action.name.startsWith("combo_"))return true;
        double time=boss.seconds(0);
        if(action.name.equals("ultimate_full"))return false;
        if(action.name.startsWith("ally_"))return time>=.15&&time<1.4;
        if(action.name.startsWith("counter_"))return time<.15;
        if(time<.15)return false;
        if(action.name.equals("skill_s02_jianzhen"))return time<action.cardLead+.1;
        if(action.name.equals("skill_s06_fanzhao"))return time<action.cardLead+.1;
        for(int i=0;i<action.windows.size();i++){
            Window w=action.windows.get(i);
            if(time<w.from-.05)return i==0||time>action.windows.get(i-1).to+.05;
            if(time<=w.to+.05)return false;
        }
        return false;
    }
    void adjustPosition(){
        if(action==null||boss.getTarget()==null)return;
        if(tracksTarget()&&action.name.equals("skill_s04_chixiao")){
            double limit=1;
            if(positioningTravel>=limit)return;
            double distance=boss.getTarget().position().subtract(boss.position()).horizontalDistance();
            double step=(distance<1?-.05:distance>2&&!inMeleeRange(boss.getTarget())?.05:0)*boss.actionSpeed()*ShiHuangPose.MODEL_SCALE;
            positioningTravel+=boss.stepDuringSkill(Math.copySign(Math.min(Math.abs(step),limit-positioningTravel),step));
        }
    }
    String choose(double distance){
        if(boss.mana()<ShiHuangEntity.SKILL_MANA||boss.getRandom().nextInt(100)<50)return choosePrefix("combo_");
        return boss.enhanced()&&ally.ready()&&boss.getRandom().nextBoolean()?choosePrefix("ally_"):chooseSkill();
    }
    String chooseSkill(){return choosePrefix("skill_");}
    private String choosePrefix(String prefix){
        var choices=ACTIONS.values().stream().filter(a->a.name.startsWith(prefix)&&!a.name.equals(previous.get(prefix))).toList();
        var choice=choices.get(boss.getRandom().nextInt(choices.size()));
        previous.put(prefix,choice.name);return choice.name;
    }
    float guard(DamageSource source,float amount){
        double time=boss.seconds(0),lead=ShiHuangSkillShape.cardLead(boss.animation());
        if(!boss.animation().equals("skill_s06_fanzhao")||time<lead+.1||time>lead+.55||source.getSourcePosition()==null)return amount;
        Vec3 toward=source.getSourcePosition().subtract(boss.position()).normalize();
        if(toward.dot(boss.getLookAngle())>.5){
            if(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,200,1));
            boss.sound(SoundEvents.SHIELD_BLOCK,3,.8F);
            send(TombLightPayload.burst(boss.position().add(boss.getLookAngle().scale(1.1*ShiHuangPose.MODEL_SCALE)).add(0,2.65*ShiHuangPose.MODEL_SCALE,0),0xA5DFFF,.6F,5));
            boss.play("counter_s06_fanzhao");return 0;
        }return amount;
    }
    void background(){tickWoodEffects();if(boss.enhanced())ally.tick();}
    void faceNearest(){
        var target=boss.arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())
                .min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if(target!=null){boss.setTarget(target);boss.face(target.position(),180);}
    }
    boolean tryWatchInterrupt(){
        if(watchUsed||!Set.of("interval_watch","interval_point").contains(boss.animation()))return false;
        var target=boss.arenaPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())
                .filter(p->SkillCastManager.isCasting(p)||p.isUsingItem())
                .min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if(target==null)return false;
        if(!beginInterruptCounter(target,false))return false;
        watchUsed=true;
        SkillCastManager.interruptWithCooldown(target);
        if(target.isUsingItem()){
            var item=target.getUseItem().getItem();
            // stopUsingItem cancels eating/charging; releaseUsingItem would fire a charged weapon.
            target.stopUsingItem();
            target.getCooldowns().addCooldown(item,100);
        }
        return true;
    }
    boolean tryFeintCounter(DamageSource source){
        return boss.animation().equals("interval_feint")&&source.getEntity() instanceof ServerPlayer player
                &&player.isAlive()&&!player.isCreative()&&!player.isSpectator()
                &&!player.hasEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze)
                &&beginInterruptCounter(player,boss.elapsedTicks()>=14&&boss.elapsedTicks()<=17);
    }
    private boolean beginInterruptCounter(ServerPlayer target,boolean stun){
        if(!boss.blinkBeside(target))return false;
        boss.play("counter_watch_liaotian");watchTarget=target;counterStuns=stun;boss.setTarget(target);
        return true;
    }
    private void aimBeforeRelease(int tick){
        if(action.name.startsWith("combo_")||action.name.equals("ally_d02_chongfeng")||action.name.equals("ally_d03_longxi"))return;
        if(action.name.equals("skill_s02_jianzhen")||action.name.equals("skill_s06_fanzhao")){
            if(tick>=Math.round((action.cardLead-.05)*20)&&events.add("release_aim"))faceNearest();
        }else for(int i=0;i<action.windows.size();i++)
            if(tick>=Math.max(0,Math.round(action.windows.get(i).from*20)-3)&&events.add("release_aim"+i))faceNearest();
    }
    void tick(){
        if(action==null)return;int tick=boss.elapsedTicks();double time=boss.seconds(0);
        if(action.name.equals("counter_guard_sweep")){
            if(time>=.1&&events.add("guard_sweep")){
                boss.sound(SoundEvents.PLAYER_ATTACK_SWEEP,3,.85F);
                for(var player:boss.arenaPlayers())if(!player.isCreative()&&!player.isSpectator()
                        &&player.position().subtract(boss.position()).horizontalDistance()<=ShiHuangSkillShape.GUARD_COUNTER_RADIUS
                        &&Math.abs(player.getY()-boss.getY())<=3&&boss.hasLineOfSight(player))
                    player.hurt(boss.damageSources().mobAttack(boss),35);
            }
            return;
        }
        if(action.name.equals("counter_watch_liaotian")){
            if(time>=action.windows.getFirst().from&&events.add("watch_strike")){
                boss.sound(SoundEvents.PLAYER_ATTACK_SWEEP,3,.85F);
                if(watchTarget!=null&&watchTarget.isAlive()&&!watchTarget.isCreative()&&!watchTarget.isSpectator()
                        &&boss.hasLineOfSight(watchTarget)&&meleeIntersects(boss.position(),boss.yBodyRot,false,watchTarget.getBoundingBox())){
                    watchTarget.hurt(boss.damageSources().mobAttack(boss),30);
                    if(counterStuns){
                        watchTarget.addEffect(new MobEffectInstance(icu.icuqalt10.panlingre.init.ModEffects.freeze,20,0));
                        watchTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,2));
                        watchTarget.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,60,1));
                    }
                }
            }
            return;
        }
        aimBeforeRelease(tick);
        if(action.name.equals("skill_s02_jianzhen")&&time>=action.cardLead+.1&&events.add("rain_prepare"))releaseSkill();
        if(action.cardLead>0&&time>=action.dissolve&&events.add("card_gather"))boss.sound(SoundEvents.AMETHYST_BLOCK_CHIME,2,1.25F);
        for(int i=0;i<action.windows.size();i++){
            Window window=action.windows.get(i);
            int strike=(int)Math.round(window.from*20);
            double start=i==0?0:action.windows.get(i-1).to+.05;
            if((action.name.startsWith("combo_")||action.name.equals("skill_s04_chixiao"))&&time>=start&&tick<strike&&boss.getTarget()!=null){
                var target=boss.getTarget();
                if(target.position().subtract(boss.position()).horizontalDistance()<=6
                        &&!meleeIntersects(boss.position(),boss.yBodyRot,window.thrust,target.getBoundingBox()))events.add("dash"+i);
                if(events.contains("dash"+i))boss.approachTarget(target,1.4,strike-tick);
            }
            if(tick>=strike&&events.contains("dash"+i)&&events.add("dash_end"+i))boss.stopRapidMovement();
        }
        if(action.name.equals("ultimate_full")){ally.tickUltimate(time);
            if(time>=4.4&&events.add("ultimate_roar"))ally.roarUltimate();
            if(time>=4.4&&events.add("ultimate_draw"))boss.sound(SoundEvents.PLAYER_ATTACK_SWEEP,3,.85F);
            if(time>=4.5&&events.add("ultimate_hit")){
            skillOrigin=boss.position();
            boss.sound(SoundEvents.GENERIC_EXPLODE.value(),4,.65F);
            for(var p:boss.arenaPlayers())if(!p.isCreative())p.hurt(boss.damageSources().mobAttack(boss),100);
        }return;}
        for(int i=0;i<action.windows.size();i++){
            Window w=action.windows.get(i);
            if(action.name.equals("skill_s08_wuxing")&&time>=w.to&&events.add("element_wave"+i))releaseElementWave(w);
            if(Set.of("skill_s01_duanyue","skill_s03_fengjiang","skill_s05_zhenyue","skill_s07_rongyan","skill_s09_shengxi").contains(action.name)&&time>=w.to&&events.add("skill_release"+i))releaseSkill();
            if(time>=w.from&&lastTime<=w.to){
                if(events.add("swing"+i)){if(action.name.startsWith("combo_"))boss.addMana(1);boss.sound(SoundEvents.PLAYER_ATTACK_SWEEP,3,.85F);if(action.name.equals("skill_s04_chixiao")||action.name.startsWith("counter_"))releaseSkill();}
                if(action.name.startsWith("combo_")||action.name.startsWith("counter_"))swordHit(i,w);}
        }
        tickEffects(tick);lastTime=time;
    }
    private void swordHit(int index,Window window){
        Set<UUID> struck=hits.computeIfAbsent(index,n->new HashSet<>());
        for(var p:boss.arenaPlayers())if(!p.isCreative()&&!struck.contains(p.getUUID())
                &&boss.hasLineOfSight(p)&&meleeIntersects(boss.position(),boss.yBodyRot,window.thrust,p.getBoundingBox())){
                boolean normal=action.name.startsWith("combo_");
                DamageSource source=boss.damageSources().mobAttack(boss);
                if(normal&&boss.enraged())source=new DamageSource(source.typeHolder(),boss){
                    @Override public boolean is(net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> tag){
                        return tag==net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN||super.is(tag);
                    }
                };
                boolean hit=p.hurt(source,normal?(float)(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)*2/3):skillDamage(35));
                if(hit&&action.name.startsWith("counter_"))p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,200,1));
                if(hit){if(normal&&struck.isEmpty())boss.addMana(1);struck.add(p.getUUID());}
        }
    }
    boolean inMeleeRange(net.minecraft.world.entity.LivingEntity target){
        return target.position().subtract(boss.position()).horizontalDistance()<=MELEE_REACH
                &&target.getBoundingBox().maxY>boss.getBoundingBox().minY
                &&target.getBoundingBox().minY<boss.getBoundingBox().maxY&&boss.hasLineOfSight(target);
    }
    static icu.icuqalt10.panlingre.entity.OrientedBoundingBox swordBox(ShiHuangPose.Frame frame,float yaw,Vec3 origin){
        return ShiHuangPose.box(frame,"sword",BOUNDS.get("sword"),ShiHuangPose.modelToEntity(yaw),origin);
    }
    /** Three-block reach: a 120-degree sweep or a 1.2-block-wide thrust, only in front. */
    static boolean meleeIntersects(Vec3 origin,float yaw,boolean thrust,AABB target){
        if(target.maxY<=origin.y||target.minY>=origin.y+3)return false;
        Vec3 forward=Vec3.directionFromRotation(0,yaw),side=new Vec3(-forward.z,0,forward.x);
        if(thrust)return new icu.icuqalt10.panlingre.entity.OrientedBoundingBox(origin.add(forward.scale(MELEE_REACH/2)).add(0,1.5,0),
                side,new Vec3(0,1,0),forward,new Vec3(.6,1.5,MELEE_REACH/2)).intersects(target);
        // Clip the player's horizontal box to the two edges of the sector, then test its radius.
        List<Vec3> polygon=new ArrayList<>(List.of(new Vec3(target.minX-origin.x,0,target.minZ-origin.z),
                new Vec3(target.maxX-origin.x,0,target.minZ-origin.z),new Vec3(target.maxX-origin.x,0,target.maxZ-origin.z),
                new Vec3(target.minX-origin.x,0,target.maxZ-origin.z)));
        for(int sign:new int[]{-1,1}){
            var clipped=new ArrayList<Vec3>();Vec3 normal=forward.scale(Math.sqrt(3)/2).add(side.scale(sign*.5));
            if(polygon.isEmpty())return false;
            Vec3 previous=polygon.getLast();double before=previous.dot(normal);
            for(Vec3 point:polygon){
                double after=point.dot(normal);
                if((before>=0)!=(after>=0))clipped.add(previous.lerp(point,before/(before-after)));
                if(after>=0)clipped.add(point);
                previous=point;before=after;
            }
            polygon=clipped;
        }
        for(int i=0;i<polygon.size();i++){
            Vec3 a=polygon.get(i),delta=polygon.get((i+1)%polygon.size()).subtract(a);
            double u=delta.lengthSqr()<1e-12?0:net.minecraft.util.Mth.clamp(-a.dot(delta)/delta.lengthSqr(),0,1);
            if(a.add(delta.scale(u)).lengthSqr()<=MELEE_REACH*MELEE_REACH)return true;
        }
        return false;
    }
    record AttackVolume(Vec3 from,Vec3 to,double radius){
        boolean intersects(AABB target){
            if(!new AABB(from,to).inflate(radius,radius+2.5,radius).intersects(target))return false;
            Vec3 delta=to.subtract(from).multiply(1,0,1),center=target.getCenter();
            double u=delta.lengthSqr()<1e-8?0:net.minecraft.util.Mth.clamp(center.subtract(from).dot(delta)/delta.lengthSqr(),0,1);
            Vec3 point=from.add(delta.scale(u));
            double x=net.minecraft.util.Mth.clamp(point.x,target.minX,target.maxX)-point.x;
            double z=net.minecraft.util.Mth.clamp(point.z,target.minZ,target.maxZ)-point.z;
            return x*x+z*z<=radius*radius;
        }
    }
    static AttackVolume lineBox(Vec3 a,Vec3 b,double radius){return new AttackVolume(a,b,radius);}
    private void charge(String element,Vec3 from,double fraction){var frame=boss.pose(0);Vec3 blade=boss.bonePoint(frame,"sword_trail_end",PIVOTS.get("sword_trail_end"));light(false,from,blade,element,.035F,3);}
    private void add(Vec3 a,Vec3 b,String element,int duration,double radius,float damage,int shape){
        // Send during the windup, but use the completed stroke's world tick on both sides.
        int delay=Math.max(0,switch(action.name){
            case "counter_s06_fanzhao"->7-boss.elapsedTicks();
            default->0;
        });
        effects.add(new Effect(a,b,element,boss.elapsedTicks()+delay,duration,radius,damage,shape,new Vec3(0,1,0),new HashSet<>()));
        if(shape!=TombLightPayload.FIRE_FAN)send(new TombLightPayload(shape,a,b,color(element),(float)radius,duration+1,boss.level().getGameTime()+delay).onClock(boss,delay+boss.elapsedTicks()-boss.seconds(0)*20));
    }
    private void releaseSkill(){
        skillOrigin=boss.position().add(0,1,0);skillDirection=boss.getLookAngle().multiply(1,0,1).normalize();
        skillTarget=boss.getTarget()==null?skillOrigin.add(skillDirection.scale(12)):boss.getTarget().position().add(0,1,0);
        Vec3 origin=skillOrigin,direction=skillDirection,side=new Vec3(-direction.z,0,direction.x),target=skillTarget;
        String id=action.name.startsWith("counter_")?"s06":action.name.substring(6,9);
        switch(id){
            case "s01"->{Vec3 floor=boss.position();add(floor,floor.add(direction.scale(28)),"metal",20,4.5,25,TombLightPayload.CRESCENT);}
            case "s02"->{
                int players=Math.max(1,(int)Math.round(boss.getMaxHealth()/ShiHuangEntity.HEALTH_PER_PLAYER));
                double range=4*Math.sqrt(players);
                for(int j=0;j<12*players;j++){
                    double angle=boss.getRandom().nextDouble()*Math.PI*2,r=range*Math.sqrt(boss.getRandom().nextDouble());
                    Vec3 p=ground(target.add(Math.cos(angle)*r,0,Math.sin(angle)*r));
                    add(p,p,"metal",ShiHuangSkillShape.SWORD_DROP_END+60,.7,20,TombLightPayload.FALLING_SWORD);
                }
            }
            case "s03"->{
                int missing=boss.getRandom().nextInt(4);Vec3[] corners={target.add(-1.5,0,-1.5),target.add(1.5,0,-1.5),target.add(1.5,0,1.5),target.add(-1.5,0,1.5)};
                for(int j=0;j<4;j++)if(j!=missing)add(corners[j],corners[(j+1)%4],"wood",24,.55,15,TombLightPayload.FLOW_EDGE);
            }
            case "s06"->{for(int j=0;j<8;j++){
                Vec3 p=ground(boss.position().add(direction.scale(6+j*3.25)));
                add(p,new Vec3(j*ShiHuangSkillShape.WATER_INTERVAL,0,0),"water",(int)Math.ceil(j*ShiHuangSkillShape.WATER_INTERVAL+ShiHuangSkillShape.WATER_LIFE),1.25,25,TombLightPayload.WATER_COLUMN);
            }}
            case "s04"->add(boss.position(),direction,"fire",11,12,30,TombLightPayload.FIRE_FAN);
            case "s05"->{for(int j=-1;j<=1;j++){
                double a=j*Math.PI/6;Vec3 d=direction.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
                Vec3 floor=boss.position();add(floor,floor.add(d.scale(18)),"earth",18,3.5,20,TombLightPayload.CRESCENT);
            }}
            case "s07"->{
                Vec3 muzzle=boss.bonePoint(boss.pose(0),"sword_trail_end",PIVOTS.get("sword_trail_end"));
                Vec3 from=clearProjectileEnd(new Vec3(boss.getX(),muzzle.y,boss.getZ()),muzzle,1.5);
                Vec3 to=clearProjectileEnd(from,from.add(direction.scale(32)),1.5);
                int duration=Math.max(1,(int)Math.ceil(from.distanceTo(to)/2));
                add(from,to,"fire+earth",duration,1.5,17.5F,TombLightPayload.LAVA_BOULDER);
                boss.sound(SoundEvents.BLAZE_SHOOT,3,.7F);
            }
            case "s09"->{
                // Keep the 1.1-block sweep above the floor even when the emperor is scaled down.
                Vec3 start=boss.position().add(0,1.1+.06,0);
                Set<UUID> struck=new HashSet<>();
                for(int i=0;i<24;i++){
                    double along=3+i*.59+.65*Math.sin(i*2.39996);
                    double across=4.2*Math.sin(i*2.39996+.7);
                    Vec3 proposed=start.add(direction.scale(along)).add(side.scale(across));
                    Vec3 clear=clearProjectileEnd(start,proposed,1.1);
                    if(clear.distanceToSqr(proposed)>.001)continue;
                    Vec3 point=ground(proposed);
                    effects.add(new Effect(point,point,"water+wood",boss.level().getGameTime(),ShiHuangSkillShape.WOOD_LIFE,1.1,25,TombLightPayload.WOOD_SURGE,direction,struck));
                    send(new TombLightPayload(TombLightPayload.WOOD_SURGE,point,point,0x43DC95,1.1F,ShiHuangSkillShape.WOOD_LIFE,boss.level().getGameTime(),direction));
                }
                boss.sound(SoundEvents.EVOKER_FANGS_ATTACK,3,.75F);
                boss.sound(SoundEvents.PLAYER_SPLASH,2,.7F);
            }
            default->throw new IllegalStateException("No skill effect: "+action.name);
        }
    }
    private void releaseElementWave(Window window){
        Vec3 forward=boss.getLookAngle().multiply(1,0,1).normalize();
        Vec3 axis=ShiHuangSkillShape.bladeAxis(action.name,window.from,window.to,boss.yBodyRot,forward);
        Vec3 from=boss.position().add(0,1.25*ShiHuangPose.MODEL_SCALE,0),to=from.add(forward.scale(30));
        float damage=switch(window.element){case "metal","fire"->30;case "water"->20;default->25;};
        effects.add(new Effect(from,to,window.element,boss.elapsedTicks(),20,3.5,damage,TombLightPayload.ELEMENT_CRESCENT,axis,new HashSet<>()));
        send(new TombLightPayload(TombLightPayload.ELEMENT_CRESCENT,from,to,color(window.element),3.5F,21,boss.level().getGameTime(),axis).onClock(boss,boss.elapsedTicks()-boss.seconds(0)*20));
    }
    private Vec3 clearProjectileEnd(Vec3 from,Vec3 to,double radius){
        Vec3 end=to;
        for(var shape:boss.level().getBlockCollisions(boss,new net.minecraft.world.phys.AABB(from,to).inflate(radius)))
            for(var box:shape.toAabbs()){
                var expanded=box.inflate(radius);
                if(expanded.contains(from))return from;
                var contact=expanded.clip(from,to);
                if(contact.isPresent()&&contact.get().distanceToSqr(from)<end.distanceToSqr(from))end=contact.get();
            }
        return end;
    }
    private Vec3 ground(Vec3 point){
        if(boss.tomb()!=null){var bounds=boss.tomb().scene().emperorArena();point=new Vec3(net.minecraft.util.Mth.clamp(point.x,bounds.minX+1,bounds.maxX-1),point.y,net.minecraft.util.Mth.clamp(point.z,bounds.minZ+1,bounds.maxZ-1));}
        var hit=boss.level().clip(new net.minecraft.world.level.ClipContext(point.add(0,3,0),point.add(0,-16,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,boss));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?new Vec3(point.x,boss.getY(),point.z):hit.getLocation();
    }
    private void tickEffects(int tick){
        // Damage callbacks can end the instance and clear this action's live effects.
        for(var e:List.copyOf(effects)){if(!effects.contains(e))return;if(e.shape==TombLightPayload.WOOD_SURGE)continue;int age=(int)(tick-e.start);if(age<0||age>e.duration)continue;
            switch(e.shape){
                case TombLightPayload.FLOW_EDGE->hit(e,lineBox(e.from,e.to,e.radius));
                case TombLightPayload.CRESCENT,TombLightPayload.ELEMENT_CRESCENT->tickCrescent(e,age);
                case TombLightPayload.LAVA_BOULDER->{
                    Vec3 a=e.from.lerp(e.to,Math.max(0,(age-boss.actionSpeed())/e.duration)),b=e.from.lerp(e.to,Math.min(1,age/(double)e.duration));
                    hit(e,lineBox(a,b,e.radius));
                    if(age>=e.duration-1&&events.add("impact"+effects.indexOf(e))){send(new TombLightPayload(TombLightPayload.HIT_SPARKS,e.to,Vec3.ZERO,color(e.element),2,12));boss.sound(SoundEvents.GENERIC_EXPLODE.value(),3,.8F);}
                }
                case TombLightPayload.FIRE_FAN->tickFan(e);
                case TombLightPayload.FALLING_SWORD->{
                    if(age>=ShiHuangSkillShape.SWORD_DROP_START&&age<=ShiHuangSkillShape.SWORD_DROP_END){double a=ShiHuangSkillShape.swordDrop(age-boss.actionSpeed()),b=ShiHuangSkillShape.swordDrop(age);
                        hit(e,lineBox(e.from.add(0,a,0),e.from.add(0,b+4.8,0),.35));}
                }
                case TombLightPayload.WATER_COLUMN->{
                    double height=ShiHuangSkillShape.waterHeight(age-e.to.x);
                    if(height>0)for(var player:boss.arenaPlayers()){
                        var box=player.getBoundingBox();double x=net.minecraft.util.Mth.clamp(e.from.x,box.minX,box.maxX),z=net.minecraft.util.Mth.clamp(e.from.z,box.minZ,box.maxZ);
                        if(box.maxY>e.from.y&&box.minY<e.from.y+height&&(x-e.from.x)*(x-e.from.x)+(z-e.from.z)*(z-e.from.z)<=e.radius*e.radius)hitPlayer(e,player);
                    }
                }
            }
        }
    }
    private void tickWoodEffects(){
        long now=boss.level().getGameTime();
        effects.removeIf(e->e.shape==TombLightPayload.WOOD_SURGE&&now-e.start>=e.duration);
        for(var e:List.copyOf(effects))if(e.shape==TombLightPayload.WOOD_SURGE){
            double height=ShiHuangSkillShape.woodHeight(now-e.start);
            if(height>.1)for(int stem=0;stem<3;stem++)for(int segment=0;segment<8;segment++)
                hit(e,lineBox(ShiHuangSkillShape.woodPoint(e.from,e.axis,stem,segment/8.0,height),ShiHuangSkillShape.woodPoint(e.from,e.axis,stem,(segment+1)/8.0,height),.3));
        }
    }
    private void tickCrescent(Effect e,int age){
        Vec3 forward=e.to.subtract(e.from).normalize();
        for(int step=0;step<5;step++){
            double t=Math.max(0,age-boss.actionSpeed()+(step+1)*boss.actionSpeed()/5.0);
            Vec3 center=e.from.lerp(e.to,Math.min(1,t/e.duration));
            for(int i=0;i<24;i++){
                double angle=(i+.5)*Math.PI/24-Math.PI/2;
                Vec3 a=ShiHuangSkillShape.crescent(center,forward,e.axis,e.radius,angle,0),b=ShiHuangSkillShape.crescent(center,forward,e.axis,e.radius,angle,1);
                hit(e,lineBox(a,b,.22));
            }
        }
        if(e.element.equals("earth")){
            Vec3 p=e.from.lerp(e.to,Math.min(1,age/(double)e.duration)).add(forward.scale(e.radius*.82));
            for(var viewer:boss.arenaPlayers())((ServerLevel)boss.level()).sendParticles(viewer,new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()),false,p.x,p.y+.1,p.z,5,.25,.15,.25,.15);
        }
    }
    private void tickFan(Effect e){
        double end=Math.min(ShiHuangSkillShape.FAN_END,boss.seconds(0)-action.cardLead);
        for(int i=0;i<36;i++){
            double t=ShiHuangSkillShape.FAN_START+(end-ShiHuangSkillShape.FAN_START)*(i+.5)/36;
            Vec3 a=ShiHuangSkillShape.fanPoint(boss.position(),boss.yBodyRot,t,.2),b=ShiHuangSkillShape.fanPoint(boss.position(),boss.yBodyRot,t,12);
            hit(e,lineBox(a,b,.38));
        }
    }
    private void hit(Effect e,AttackVolume box){
        for(var p:boss.arenaPlayers())if(box.intersects(p.getBoundingBox()))hitPlayer(e,p);
    }
    private void hitPlayer(Effect e,net.minecraft.server.level.ServerPlayer p){
        if(!effects.contains(e)||p.isCreative())return;
        if(e.shape==TombLightPayload.WOOD_SURGE){
            var control=p.getEffect(icu.icuqalt10.panlingre.init.ModEffects.freeze);
            if(control==null||control.getDuration()<10)p.addEffect(new MobEffectInstance(icu.icuqalt10.panlingre.init.ModEffects.freeze,10,0));
            p.setDeltaMovement(0,p.getDeltaMovement().y,0);p.hurtMarked=true;
        }
        if(e.hit.contains(p.getUUID()))return;
        if(e.shape==TombLightPayload.ELEMENT_CRESCENT){
            int invulnerability=p.invulnerableTime;p.invulnerableTime=0;
            if(!p.hurt(boss.damageSources().mobAttack(boss),skillDamage(e.damage))){p.invulnerableTime=invulnerability;return;}
            e.hit.add(p.getUUID());
            switch(e.element){
                case "metal"->p.addEffect(new MobEffectInstance(MobEffects.WITHER,200,1));
                case "water"->p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,200,1));
                case "wood"->p.addEffect(new MobEffectInstance(MobEffects.POISON,200,1));
                case "fire"->p.igniteForSeconds(10);
                case "earth"->p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,300,1));
            }
            send(new TombLightPayload(TombLightPayload.HIT_SPARKS,p.position().add(0,p.getBbHeight()*.55,0),Vec3.ZERO,color(e.element),1,12));
            return;
        }
        if(e.shape==TombLightPayload.WATER_COLUMN)p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,200,1));
        if(e.element.equals("earth"))p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,1));
        if(e.element.equals("fire")||e.element.equals("fire+earth"))p.igniteForSeconds(10);
        if(p.hurt(boss.damageSources().mobAttack(boss),skillDamage(e.damage))){
            e.hit.add(p.getUUID());
            if(e.shape==TombLightPayload.WOOD_SURGE){
                send(new TombLightPayload(TombLightPayload.HIT_SPARKS,p.position().add(0,p.getBbHeight()*.55,0),Vec3.ZERO,0x43DC95,1,12));
            }
            if(e.shape==TombLightPayload.CRESCENT||e.shape==TombLightPayload.FIRE_FAN||e.shape==TombLightPayload.FLOW_EDGE)
                send(new TombLightPayload(TombLightPayload.HIT_SPARKS,p.position().add(0,p.getBbHeight()*.55,0),Vec3.ZERO,color(e.element),1,12));
        }
    }
    private void send(TombLightPayload payload){for(var viewer:boss.arenaPlayers())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,payload);}
    private void light(boolean ring,Vec3 from,Vec3 to,String element,float width,int ticks){
        int color=color(element);
        var payload=new icu.icuqalt10.panlingre.network.TombLightPayload(ring?1:0,from,to,color,width,ticks);
        for(var viewer:boss.arenaPlayers())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,payload);
    }
    private static int color(String element){return switch(element){case "wood"->0x42EAA0;case "water"->0x577BBA;case "fire","fire+earth"->0xFF5825;case "earth"->0xD29728;case "gold"->0xFFCB55;default->0xF4F7FF;};}
}
