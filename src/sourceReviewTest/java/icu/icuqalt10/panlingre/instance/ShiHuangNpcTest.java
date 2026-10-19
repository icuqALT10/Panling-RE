package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class ShiHuangNpcTest {
    private static void set(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void initializationKeepsNpcs(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "NpcReview"), false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        var scene = new ShiHuangScene(h.absolutePos(new BlockPos(0, 120, 0)));
        var pos = scene.point(10, 12, 10);
        level.getChunk(BlockPos.containing(pos));
        var npc = EntityType.VILLAGER.create(level);
        npc.addTag("shi_huang_ling"); npc.setNoAi(true); npc.setPos(pos);
        level.addFreshEntity(npc);
        var leftover = EntityType.ZOMBIE.create(level);
        leftover.setNoAi(true); leftover.setPos(pos.add(2, 0, 0)); level.addFreshEntity(leftover);
        var definition = InstanceDefinition.parse(com.google.gson.JsonParser.parseString("""
                {"controller":"panlingre:shihuang","template":"panlingre:instance/shihuang",
                 "center_pos":[0,112,0],"player_spawn_pos":[0,72,0],"spacing":512,
                 "success":"panlingre:party_test/success"}
                """).getAsJsonObject());
        var controller = new ShiHuangController();
        var session = new InstanceSession(level.getServer(), ResourceLocation.parse("panlingre:npc_test"), definition, 0, player, controller);
        set(controller, "session", session); set(controller, "scene", scene);
        try {
            player.setGameMode(GameType.SPECTATOR);
            var initialize = ShiHuangController.class.getDeclaredMethod("initializeLoadedScene");
            initialize.setAccessible(true); initialize.invoke(controller);
            h.assertTrue(!npc.isRemoved(), "Scene initialization deleted the authored NPC");
            h.assertTrue(leftover.isRemoved(), "Old combat entity was not cleaned up");
            player.setGameMode(GameType.CREATIVE);
            controller.tick(session);
            h.assertTrue(!npc.isRemoved(), "NPC disappeared after switching back from spectator");
            System.out.println("SHIHUANG_NPC_INIT PASS: NPC preserved, combat leftovers removed, spectator/creative switch");
            h.succeed();
        } finally {
            controller.stop(session, InstanceResult.FAILURE);
            npc.discard(); leftover.discard(); player.discard();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 4000)
    public static void refreshFunctionsStayLocal(GameTestHelper h) {
        var level = h.getLevel();
        var loaded = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for (int offset : new int[]{0, 512}) for (int id = 3; id <= 7; id++) {
            var chunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(new BlockPos(offset + id * 16 - 1, 6, 0)));
            loaded.add(chunk); level.setChunkForced(chunk.x, chunk.z, true);
        }
        h.startSequence().thenWaitUntil(() -> h.assertTrue(loaded.stream().allMatch(p -> level.areEntitiesLoaded(p.toLong())),
                "Waiting for NPC chunks to become entity-accessible")).thenExecute(() -> {
        for (int offset : new int[]{0, 512}) for (int id = 3; id <= 7; id++) {
            var commandPos = Vec3.atCenterOf(h.absolutePos(new BlockPos(offset + id * 16, 6, 0)));
            var expected = commandPos.add(-1, 3.5, 0);
            level.getChunk(BlockPos.containing(expected));
            var box = new AABB(expected.add(-.1, -.1, -.1), expected.add(.1, 2, .1));
            var source = level.getServer().createCommandSourceStack().withLevel(level).withPosition(commandPos).withPermission(2);
            for (int attempt = 0; attempt < 2; attempt++) {
                level.getServer().getCommands().performPrefixedCommand(source, "execute positioned ~-1 ~3.5 ~ run function plre:npcs/instance/shi_huang_ling/" + id);
                var npcs = level.getEntitiesOfClass(Villager.class, box, e -> e.getTags().contains("shi_huang_ling"));
                h.assertTrue(npcs.size() == 1, "Refresh " + id + " offset " + offset + " spawned " + npcs.size() + " local NPCs");
                h.assertTrue(npcs.getFirst().getTags().contains(Integer.toString(id)), "Wrong NPC identity");
                var nbt = new net.minecraft.nbt.CompoundTag(); npcs.getFirst().saveWithoutId(nbt);
                h.assertTrue(nbt.getCompound("can_interact").getString("command").equals("dialog show task_instance_shi_huang_ling_" + id), "Dialogue lost");
            }
            level.getEntitiesOfClass(Villager.class, box).forEach(Villager::discard);
        }
        for (var chunk : loaded) level.setChunkForced(chunk.x, chunk.z, false);
        System.out.println("SHIHUANG_NPC_REFRESH PASS: all five functions, two slots, repeated refresh, dialogue preserved");
        }).thenSucceed();
    }
}
