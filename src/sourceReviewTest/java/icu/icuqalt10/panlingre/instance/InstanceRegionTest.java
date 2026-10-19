package icu.icuqalt10.panlingre.instance;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

@GameTestHolder("panlingre") @PrefixGameTestTemplate(false)
public class InstanceRegionTest {
    @GameTest(template="empty",timeoutTicks=80)
    public static void loadedResetDoesNotCapCapacity(GameTestHelper h)throws Exception {
        var level=h.getLevel();
        var original=InstanceDefinitionLoader.get(ResourceLocation.fromNamespaceAndPath("panlingre","qinglong")).orElseThrow();
        var definition=new InstanceDefinition(original.controller(),original.template(),original.centerPos().add(51200,0,0),
                original.playerSpawnPos().add(51200,0,0),original.rotation(),original.safeDistance(),original.playerLimitCount(),512,1,1,2,original.success(),original.failure(),original.intoFunction());
        var chunk=new ChunkPos(BlockPos.containing(definition.centerPos()));level.getChunk(chunk.x,chunk.z);
        h.assertTrue(!InstanceRegionTask.unloaded(level,definition,0),"Test reset is not resident");
        var runtimeClass=Class.forName(InstanceManager.class.getName()+"$RuntimeDefinition");
        var constructor=runtimeClass.getDeclaredConstructor(InstanceDefinition.class,int.class);constructor.setAccessible(true);
        Object runtime=constructor.newInstance(definition,1);
        var serverField=InstanceManager.class.getDeclaredField("server");serverField.setAccessible(true);
        var dataField=InstanceManager.class.getDeclaredField("savedData");dataField.setAccessible(true);
        var queueField=InstanceManager.class.getDeclaredField("BUILD_QUEUE");queueField.setAccessible(true);
        Object previousServer=serverField.get(null),previousData=dataField.get(null);
        @SuppressWarnings("unchecked") var queue=(Queue<Object>)queueField.get(null);
        var previousQueue=new ArrayDeque<>(queue);
        try {
            serverField.set(null,level.getServer());var data=new InstanceSavedData();dataField.set(null,data);
            var replenish=InstanceManager.class.getDeclaredMethod("replenish",ResourceLocation.class,runtimeClass);replenish.setAccessible(true);
            replenish.invoke(null,ResourceLocation.fromNamespaceAndPath("panlingre","reserve_test"),runtime);
            h.assertTrue(data.allocatedSlots("panlingre:reserve_test")>=2,"Loaded reset consumed all reserve capacity");
        } finally {
            serverField.set(null,previousServer);dataField.set(null,previousData);queue.clear();queue.addAll(previousQueue);
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void mapUpdatePreservesInstanceRegions(GameTestHelper h)throws Exception {
        var base=h.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("region-update-test");
        var world=base.resolve("world");var template=base.resolve("template");
        for(var root:List.of(world,template))for(String dir:List.of("region","data"))java.nio.file.Files.createDirectories(root.resolve(dir));
        CompoundTag tag=new CompoundTag(),data=new CompoundTag(),settings=new CompoundTag();
        settings.putLong("seed",202961L);data.put("WorldGenSettings",settings);data.putLong("Time",318271408L);tag.put("Data",data);
        NbtIo.writeCompressed(tag,world.resolve("level.dat"));data.putLong("Time",0);NbtIo.writeCompressed(tag,template.resolve("level.dat"));
        byte[] instance={1,2,3,4},marker={5,6,7};
        java.nio.file.Files.write(world.resolve("region/r.0.4.mca"),instance);
        java.nio.file.Files.write(template.resolve("region/r.0.4.mca"),new byte[]{9});
        java.nio.file.Files.write(world.resolve("data/panlingre_instances.dat"),marker);
        java.nio.file.Files.write(template.resolve("data/panlingre_instances.dat"),new byte[]{9});
        byte[] boundary=new byte[12288];
        int mapIndex=4*32,oldQinglong=27*32,oldDungeon=29*32;
        for(int index:new int[]{mapIndex,oldQinglong,oldDungeon})boundary[index*4+3]=1;
        java.nio.file.Files.write(template.resolve("region/r.0.3.mca"),boundary);
        var update=icu.icuqalt10.panlingre.worldtemplate.WorldTemplateUpdater.class.getDeclaredMethod("updateIfNeeded",java.nio.file.Path.class,java.nio.file.Path.class,String.class);
        update.setAccessible(true);update.invoke(null,world,template,"region-test-"+System.nanoTime());
        h.assertTrue(Arrays.equals(instance,java.nio.file.Files.readAllBytes(world.resolve("region/r.0.4.mca"))),"Map update overwrote instance region");
        h.assertTrue(Arrays.equals(marker,java.nio.file.Files.readAllBytes(world.resolve("data/panlingre_instances.dat"))),"Map update overwrote prepared state");
        byte[] after=java.nio.file.Files.readAllBytes(world.resolve("region/r.0.3.mca"));
        h.assertTrue(after[mapIndex*4+3]==1&&after[oldQinglong*4+3]==0&&after[oldDungeon*4+3]==0,"Boundary map chunks or old instance chunks handled incorrectly");
        h.assertTrue(NbtIo.readCompressed(world.resolve("level.dat"),NbtAccounter.unlimitedHeap()).getCompound("Data").getLong("Time")==318271408L,"Map update changed world clock");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=160000)
    public static void copiesAndReset(GameTestHelper h) {
        InstanceManager.stopServer();
        checkPrewarmInvalidation(h);
        var level=h.getLevel();
        var definitions=InstanceDefinitionLoader.entries().stream().toList();
        int[] i={0},phase={0},checks={0};
        InstanceRegionTask[] task={null};
        long[] max={0},sum={0};int[] ticks={0};
        Set<UUID> uuids=new HashSet<>();
        h.onEachTick(()->{
            if(i[0]<definitions.size()*2){
                var entry=definitions.get(i[0]/2);var d=entry.getValue();int slot=i[0]%2==0?0:d.prewarmSlots();
                if(task[0]==null){
                    h.assertTrue(InstanceRegionTask.unloaded(level,d,slot),"Copy destination loaded before write");
                    task[0]=new InstanceRegionTask(level,d,slot);
                }
                long start=System.nanoTime();boolean done=task[0].tick();long elapsed=System.nanoTime()-start;max[0]=Math.max(max[0],elapsed);sum[0]+=elapsed;ticks[0]++;
                if(!done)return;
                h.assertTrue(InstanceRegionTask.unloaded(level,d,slot),"Baked copy loaded live chunks");
                var map=level.getChunkSource().chunkMap;var storage=(EntityStorage)level.entityManager.permanentStorage;
                int minX=((int)d.centerForSlot(slot).x-256)>>4,minZ=((int)d.centerPos().z-d.regionDepth()*256)>>4;
                for(int z=minZ;z<minZ+32*d.regionDepth();z++)for(int x=minX;x<minX+32;x++){
                    var p=new ChunkPos(x,z);var nbt=map.read(p).join().orElseThrow();
                    h.assertTrue(nbt.getInt("xPos")==x&&nbt.getInt("zPos")==z,"Misplaced chunk");
                    h.assertTrue(nbt.getString("Status").equals("minecraft:full")&&nbt.getBoolean("isLightOn"),"Chunk needs generation/light");checks[0]++;
                }
                int gates=0;
                if(d.regionDepth()==2){
                    try(var input=level.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("panlingre","instance_regions/shihuang.nbt")).orElseThrow().open()){
                        var baked=NbtIo.readCompressed(input,NbtAccounter.unlimitedHeap());
                        for(Tag raw:baked.getList("Entities",10)){
                            var e=(CompoundTag)raw;var pos=new ChunkPos(e.getInt("X")+slot*32,e.getInt("Z"));
                            var nbt=storage.simpleRegionStorage.read(pos).join().orElseThrow();
                            for(Tag entity:nbt.getList("Entities",10)){
                                var tag=(CompoundTag)entity;h.assertTrue(uuids.add(tag.getUUID("UUID")),"Reused entity UUID");
                                var origin=BlockPos.of(baked.getLong("Origin")).offset(slot*512,0,0);
                                h.assertTrue(tag.getList("Tags",8).getString(0).startsWith("panlingre.tomb."+origin.asLong()+"."),"Old gate ownership");gates++;
                            }
                        }
                    }catch(java.io.IOException e){throw new RuntimeException(e);}
                    h.assertTrue(gates==10,"Missing gate entities");
                }
                System.out.println("REGION_CHECK copied "+entry.getKey()+" slot="+slot+" gates="+gates);
                var saved=level.getDataStorage().computeIfAbsent(InstanceSavedData.FACTORY,"region_copy_test");saved.setPreparedVersion(entry.getKey().toString(),slot,d.arenaVersion());
                task[0]=null;i[0]++;return;
            }
            var d=definitions.stream().filter(e->e.getKey().getPath().equals("qinglong")).findFirst().orElseThrow().getValue();
            var p=BlockPos.containing(d.centerPos());var cp=new ChunkPos(p);
            if(phase[0]==0){
                h.assertTrue(d.contains(0,d.playerSpawnPos()),"Spawn outside region");
                level.setChunkForced(cp.x,cp.z,true);level.getChunk(cp.x,cp.z);level.setBlock(p,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
                h.assertTrue(!InstanceRegionTask.unloaded(level,d,0),"Overwriting loaded region allowed");
                level.setChunkForced(cp.x,cp.z,false);phase[0]=1;return;
            }
            if(phase[0]==1){
                if(!InstanceRegionTask.unloaded(level,d,0))return;
                var outside=new ChunkPos(0,((int)d.centerPos().z-256)>>4);
                var map=level.getChunkSource().chunkMap;
                var stale=map.read(cp).join().orElseThrow();
                stale.putInt("xPos",outside.x);stale.putInt("zPos",outside.z);
                stale.putBoolean("StaleScene",true);map.write(outside,stale).join();
                var marker=new CompoundTag();marker.putBoolean("StaleScene",true);
                ((EntityStorage)level.entityManager.permanentStorage).simpleRegionStorage.write(outside,marker).join();
                level.getPoiManager().simpleRegionStorage.write(outside,marker).join();
                var bumped=new InstanceDefinition(d.controller(),d.template(),d.centerPos(),d.playerSpawnPos(),d.rotation(),d.safeDistance(),d.playerLimitCount(),
                        d.spacing(),d.regionDepth(),d.prewarmSlots(),d.arenaVersion()+1,d.success(),d.failure(),d.intoFunction());
                task[0]=new InstanceRegionTask(level,bumped,0);phase[0]=2;return;
            }
            if(phase[0]==2){
                if(!task[0].tick())return;
                var outside=new ChunkPos(0,((int)d.centerPos().z-256)>>4);
                var cleared=level.getChunkSource().chunkMap.read(outside).join().orElseThrow();
                h.assertTrue(!cleared.contains("StaleScene"),"Old chunk outside building survived version rebuild");
                h.assertTrue(((EntityStorage)level.entityManager.permanentStorage).simpleRegionStorage.read(outside).join().isEmpty(),"Old entities survived rebuild");
                h.assertTrue(level.getPoiManager().simpleRegionStorage.read(outside).join().isEmpty(),"Old POI survived rebuild");
                h.assertTrue(level.getBlockState(new BlockPos(outside.getMinBlockX()+(p.getX()&15),p.getY(),outside.getMinBlockZ()+(p.getZ()&15))).isAir(),"Old building outside new footprint survived rebuild");
                h.assertTrue(!level.getBlockState(p).is(Blocks.DIAMOND_BLOCK),"Reset resurrected stale live chunk");
                level.getDataStorage().save();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
                try{
                    var file=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/region_copy_test.dat");
                    var saved=InstanceSavedData.FACTORY.deserializer().apply(NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getCompound("data"),level.registryAccess());
                    h.assertTrue(saved.allocatedSlots("panlingre:qinglong")==21,"Overflow allocation lost on reload");
                    h.assertTrue(saved.preparedVersion("panlingre:shihuang",10)==2,"Ready marker lost on reload");
                }catch(Exception e){throw new RuntimeException(e);}
                System.out.println("REGION_CHECK fullLitChunks="+checks[0]+" no-load copy, version rebuild, stale blocks/entities/POI removal, live reset exclusion, disk reload passed; submitMaxMs="+max[0]/1e6+" pollMeanMs="+sum[0]/1e6/ticks[0]);
                h.succeed();phase[0]=3;
            }
        });
    }

    private static void checkPrewarmInvalidation(GameTestHelper h) {
        try {
            var dataField=InstanceManager.class.getDeclaredField("savedData");dataField.setAccessible(true);
            var queueField=InstanceManager.class.getDeclaredField("BUILD_QUEUE");queueField.setAccessible(true);
            var reload=InstanceManager.class.getDeclaredMethod("reloadDefinitions");reload.setAccessible(true);
            var data=new InstanceSavedData();int count=0;
            for(var entry:InstanceDefinitionLoader.entries())for(int slot=0;slot<entry.getValue().prewarmSlots();slot++) {
                data.setPreparedVersion(entry.getKey().toString(),slot,entry.getValue().arenaVersion());count++;
            }
            dataField.set(null,data);reload.invoke(null);
            h.assertTrue(((Queue<?>)queueField.get(null)).isEmpty(),"Matching versions rebuilt unnecessarily");
            var entry=InstanceDefinitionLoader.entries().iterator().next();
            data.setPreparedVersion(entry.getKey().toString(),0,entry.getValue().arenaVersion()-1);reload.invoke(null);
            h.assertTrue(((Queue<?>)queueField.get(null)).size()==1,"Version mismatch did not queue exactly its scene");
            // A missing .dat supplies a fresh SavedData instance through the same storage factory.
            dataField.set(null,InstanceSavedData.FACTORY.constructor().get());reload.invoke(null);
            h.assertTrue(((Queue<?>)queueField.get(null)).size()==count,"Missing saved state did not queue all initial scenes");
            System.out.println("REGION_CHECK version mismatch and missing-dat initial prewarm queued correctly");
        } catch(Exception e) { throw new RuntimeException(e); }
        finally { InstanceManager.stopServer(); }
    }
}
