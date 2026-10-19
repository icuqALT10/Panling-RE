package icu.icuqalt10.panlingre.instance;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.*;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class InstancePartyTest {
    private static com.google.gson.JsonObject spawnConfig(ResourceLocation id, Vec3 spawn) {
        return com.google.gson.JsonParser.parseString("""
                {"controller":"%s","template":"%s","center_pos":[%s,%s,%s],
                 "player_spawn_pos":[%s,%s,%s],"spacing":512,"prewarm_slots":1,
                 "success":"panlingre:party_test/success","rotation":[135,-25],"safe_distance":3,"player_limit_count":5}
                """.formatted(id, id, spawn.x, spawn.y, spawn.z, spawn.x, spawn.y, spawn.z)).getAsJsonObject();
    }

    @GameTest(template = "empty")
    public static void arrivalConfiguration(GameTestHelper h) {
        var json = spawnConfig(ResourceLocation.parse("panlingre:arrival_test"), Vec3.ZERO);
        var definition = InstanceDefinition.parse(json);
        h.assertTrue(definition.intoFunction().isEmpty(), "Omitted into_function must default to empty");
        for (var empty : List.of(com.google.gson.JsonNull.INSTANCE, new com.google.gson.JsonPrimitive(""))) {
            json.add("into_function", empty);
            h.assertTrue(InstanceDefinition.parse(json).intoFunction().isEmpty(), "Empty into_function must be disabled");
        }
        json.addProperty("into_function", "plre:instances/shihuang/into");
        h.assertTrue(InstanceDefinition.parse(json).intoFunction().orElseThrow()
                .equals(ResourceLocation.parse("plre:instances/shihuang/into")), "Entry function not parsed");
        json.remove("into_function");
        h.assertTrue(definition.rotation().equals(new InstanceDefinition.Rotation(135, -25))
                && definition.safeDistance() == 3, "Arrival fields not parsed");
        h.assertTrue(definition.playerLimitCount().equals(new InstanceDefinition.PlayerLimit(1, 5)), "Integer limit not parsed");
        h.assertTrue(definition.playerSpawnForSlot(2).equals(new Vec3(1024, 0, 0)), "Slot offset changed");
        for (String invalid : List.of("{\"rotation\":[1]}", "{\"rotation\":[0,91]}",
                "{\"rotation\":[\"NaN\",0]}", "{\"safe_distance\":-1}", "{\"safe_distance\":\"Infinity\"}",
                "{\"player_limit_count\":0}", "{\"player_limit_count\":-1}", "{\"player_limit_count\":2.5}",
                "{\"player_limit_count\":2147483648}", "{\"player_limit_count\":\"5\"}",
                "{\"player_limit_count\":{\"min\":3}}", "{\"player_limit_count\":{\"min\":5,\"max\":3}}",
                "{\"player_limit_count\":{\"min\":0,\"max\":5}}")) {
            var changed = json.deepCopy();
            com.google.gson.JsonParser.parseString(invalid).getAsJsonObject().entrySet().forEach(e -> changed.add(e.getKey(), e.getValue()));
            boolean rejected = false;
            try { InstanceDefinition.parse(changed); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Invalid arrival configuration accepted: " + invalid);
        }
        json.add("player_limit_count", com.google.gson.JsonParser.parseString("{\"min\":3,\"max\":5}"));
        h.assertTrue(InstanceDefinition.parse(json).playerLimitCount().equals(new InstanceDefinition.PlayerLimit(3, 5)), "Range limit not parsed");
        json.remove("rotation"); json.remove("safe_distance"); json.remove("player_limit_count");
        definition = InstanceDefinition.parse(json);
        h.assertTrue(definition.rotation().equals(new InstanceDefinition.Rotation(0, 0)) && definition.safeDistance() == 0
                && definition.playerLimitCount().equals(new InstanceDefinition.PlayerLimit(1, 1)), "Optional defaults incorrect");
        h.succeed();
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class Controller implements InstanceController {
        InstanceSession session;
        int ticks, stops, leaves;
        public void start(InstanceSession session) { this.session = session; }
        public void tick(InstanceSession session) { ticks++; }
        public void stop(InstanceSession session, InstanceResult result) { stops++; }
        public void onPlayerLeave(InstanceSession session, ServerPlayer player) { leaves++; }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    @SuppressWarnings("unchecked")
    public static void selectorSharedSessionAndElimination(GameTestHelper h) throws Exception {
        var server = h.getLevel().getServer();
        var playerList = server.getPlayerList();
        var onlinePlayers = (List<ServerPlayer>) field(net.minecraft.server.players.PlayerList.class, "players").get(playerList);
        var online = (Map<UUID, ServerPlayer>) field(playerList.getClass().getSuperclass(), "playersByUUID").get(playerList);
        var sessions = (Map<UUID, InstanceSession>) field(InstanceManager.class, "SESSIONS").get(null);
        var runtimes = (Map<ResourceLocation, Object>) field(InstanceManager.class, "RUNTIMES").get(null);
        var queue = (Queue<Object>) field(InstanceManager.class, "BUILD_QUEUE").get(null);
        var savedSessions = new HashMap<>(sessions);
        var savedRuntimes = new LinkedHashMap<>(runtimes);
        var savedQueue = new ArrayDeque<>(queue);
        var activeBuild = field(InstanceManager.class, "activeBuild");
        var oldBuild = activeBuild.get(null);
        var activeRequest = field(InstanceManager.class, "activeRequest");
        var oldRequest = activeRequest.get(null);
        var players = new ArrayList<ServerPlayer>();
        var extraPlayers = new ArrayList<ServerPlayer>();
        var controllers = new ArrayList<Controller>();
        var id = ResourceLocation.parse("panlingre:party_command_test");
        InstanceControllerRegistry.register(id, () -> { var c = new Controller(); controllers.add(c); return c; });
        try {
            sessions.clear(); runtimes.clear(); queue.clear(); activeBuild.set(null, null); activeRequest.set(null, null);
            Vec3 spawn = Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4)));
            var definition = new InstanceDefinition(id, id, spawn, spawn, new InstanceDefinition.Rotation(0, 0), 0,
                    new InstanceDefinition.PlayerLimit(1, 5), 512, 1, 1, 1,
                    ResourceLocation.parse("panlingre:party_test/success"), Optional.of(ResourceLocation.parse("panlingre:party_test/failure")),
                    Optional.of(ResourceLocation.parse("panlingre:party_test/into")));
            var runtimeType = Class.forName(InstanceManager.class.getName() + "$RuntimeDefinition");
            var constructor = runtimeType.getDeclaredConstructor(InstanceDefinition.class, int.class);
            constructor.setAccessible(true);
            var runtime = constructor.newInstance(definition, 4);
            var slots = (List<Object>) field(runtimeType, "slots").get(runtime);
            var stateType = (Class<? extends Enum>) Class.forName(InstanceManager.class.getName() + "$SlotState");
            Object ready = Enum.valueOf(stateType, "READY");
            Collections.fill(slots, ready); runtimes.put(id, runtime);
            for (int i = 0; i < 3; i++) {
                var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "PartyTest" + i), false);
                var player = new ServerPlayer(server, h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
                    @Override public void kill() {
                        // Logout and orphan login must clear the tag before death can trigger respawn/save.
                        if (getTags().contains("assert_clean_before_kill"))
                            h.assertTrue(!getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Tag still present when forced death starts");
                        super.kill();
                    }
                };
                player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
                    @Override public void send(Packet<?> packet) {}
                };
                player.setPos(spawn); player.addTag("party_test"); players.add(player);
                online.put(player.getUUID(), player); onlinePlayers.add(player); h.getLevel().players().add(player);
            }
            var dispatcher = server.getCommands().getDispatcher();
            var source = players.getFirst().createCommandSourceStack().withPermission(4);
            var scoreboard = server.getScoreboard();
            if (scoreboard.getObjective("locate_at") == null)
                dispatcher.execute("scoreboard objectives add locate_at dummy", source);
            if (scoreboard.getObjective("instance_entry_test") == null)
                dispatcher.execute("scoreboard objectives add instance_entry_test dummy", source);
            if (scoreboard.getObjective("instance_into_test") == null)
                dispatcher.execute("scoreboard objectives add instance_into_test dummy", source);
            var entryCount = scoreboard.getObjective("instance_entry_test");
            var intoCount = scoreboard.getObjective("instance_into_test");
            var location = scoreboard.getObjective("locate_at");
            for (var player : players) {
                scoreboard.getOrCreatePlayerScore(player, entryCount).set(0);
                scoreboard.getOrCreatePlayerScore(player, intoCount).set(0);
                scoreboard.getOrCreatePlayerScore(player, location).set(42);
            }
            players.getFirst().setYRot(67); players.getFirst().setXRot(-21);
            h.assertTrue(dispatcher.execute("plre instance start party_command_test", source) == 1, "Omitted selector must start only the executor");
            var solo = sessions.get(players.getFirst().getUUID());
            h.assertTrue(solo.participantIds().size() == 1 && sessions.size() == 1, "Default selector included other players");
            h.assertTrue(players.getFirst().getYRot() == 0 && players.getFirst().getXRot() == 0, "Default teleport view not applied");
            h.assertTrue(!InstanceManager.start(players, id) && sessions.size() == 1, "Busy member caused partial party entry");
            h.assertTrue(scoreboard.getOrCreatePlayerScore(players.getFirst(), entryCount).get() == 1
                    && scoreboard.getOrCreatePlayerScore(players.getFirst(), intoCount).get() == 1
                    && scoreboard.getOrCreatePlayerScore(players.getFirst(), location).get() == -1,
                    "Solo entry must execute into_load once as the entering player");
            for (var player : players.subList(1, 3)) h.assertTrue(
                    scoreboard.getOrCreatePlayerScore(player, entryCount).get() == 0
                    && scoreboard.getOrCreatePlayerScore(player, intoCount).get() == 0
                    && scoreboard.getOrCreatePlayerScore(player, location).get() == 42,
                    "Non-participants or rejected entry executed into_load");
            solo.finish(InstanceResult.SUCCESS);
            h.assertTrue(players.getFirst().getTags().contains("party_test_success"), "Solo success callback missing");
            queue.clear(); Collections.fill(slots, ready);
            try {
                dispatcher.execute("plre instance start party_command_test @e", source);
                h.fail("Non-player selector accepted");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {}
            h.assertTrue(sessions.isEmpty(), "Invalid selector allocated a session");
            h.assertTrue(dispatcher.execute("plre instance start party_command_test @a[tag=party_test]", server.createCommandSourceStack().withPermission(4)) == 3,
                    "Console selector did not enter all three players");
            var party = sessions.get(players.getFirst().getUUID());
            h.assertTrue(sessions.values().stream().allMatch(s -> s == party) && party.participantIds().size() == 3, "Players received different sessions");
            for (var player : players) h.assertTrue(player.position().distanceToSqr(party.playerSpawn()) < .01, "Party member not teleported to shared slot");
            for (int i = 0; i < players.size(); i++) h.assertTrue(
                    scoreboard.getOrCreatePlayerScore(players.get(i), entryCount).get() == (i == 0 ? 2 : 1)
                    && scoreboard.getOrCreatePlayerScore(players.get(i), intoCount).get() == (i == 0 ? 2 : 1)
                    && scoreboard.getOrCreatePlayerScore(players.get(i), location).get() == -1,
                    "Console party entry must execute into_load once per player, including re-entry");
            System.out.println("INSTANCE_ENTRY_FUNCTION PASS: solo, console party, re-entry, outsider and rejected entry");
            var controller = controllers.getLast();
            InstanceManager.tick(); h.assertTrue(controller.ticks == 1, "Shared controller ticked more than once");
            players.getFirst().kill();
            h.assertTrue(!party.isEnding() && party.participantIds().size() == 2, "One death ended the party");
            var leaving = players.get(1); leaving.addTag("assert_clean_before_kill");
            InstanceEvents.playerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(leaving));
            h.assertTrue(!party.isEnding() && party.participantIds().size() == 1 && controller.stops == 0, "Logout stopped survivors");
            var saved = new net.minecraft.nbt.CompoundTag(); leaving.saveWithoutId(saved);
            h.assertTrue(!saved.getList("Tags", 8).toString().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Logout saved stale active tag");
            h.assertTrue(players.getFirst().getTags().contains("party_test_failure") && leaving.getTags().contains("party_test_failure"), "Eliminated players missing individual failure callbacks");
            party.finish(InstanceResult.SUCCESS);
            h.assertTrue(controller.stops == 1 && sessions.isEmpty(), "Shared session was not released exactly once");
            h.assertTrue(players.get(2).getTags().contains("party_test_success") && !leaving.getTags().contains("party_test_success"), "Success awarded to eliminated member or omitted survivor");
            leaving.addTag(InstanceManager.ACTIVE_PLAYER_TAG);
            InstanceEvents.playerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(leaving));
            h.assertTrue(!leaving.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Orphan login kept stale tag");
            leaving.addTag(InstanceManager.ACTIVE_PLAYER_TAG);
            InstanceEvents.playerRespawned(new PlayerEvent.PlayerRespawnEvent(leaving, false));
            h.assertTrue(!leaving.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Respawn kept orphan tag");
            for (var player : players) player.setHealth(player.getMaxHealth());
            queue.clear(); Collections.fill(slots, ready);
            h.assertTrue(InstanceManager.start(players.subList(1, 3), id), "Re-entry after elimination failed");
            var lastParty = sessions.get(players.get(1).getUUID());
            InstanceManager.failPlayer(players.get(1));
            h.assertTrue(!lastParty.isEnding(), "First elimination ended second party");
            InstanceManager.failPlayer(players.get(2));
            h.assertTrue(lastParty.isEnding() && sessions.isEmpty(), "All eliminated did not release shared slot");

            Vec3 arrival = spawn.add(0, -.5, 0);
            var arrivalBlock = net.minecraft.core.BlockPos.containing(arrival);
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                for (int y = 0; y <= 2; y++) h.getLevel().setBlock(arrivalBlock.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                h.getLevel().setBlock(arrivalBlock.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
            var arrivalJson = spawnConfig(id, arrival);
            var arrivalDefinition = InstanceDefinition.parse(arrivalJson);
            var arrivalRuntime = constructor.newInstance(arrivalDefinition, 4);
            var arrivalSlots = (List<Object>) field(runtimeType, "slots").get(arrivalRuntime);
            Collections.fill(arrivalSlots, ready); runtimes.put(id, arrivalRuntime); queue.clear();
            for (var player : players) { player.setHealth(player.getMaxHealth()); player.setPos(arrival.add(20, 0, 0)); }
            h.assertTrue(InstanceManager.start(players.getFirst(), id), "Configured solo entry failed");
            h.assertTrue(players.getFirst().position().distanceToSqr(arrival) < 1e-8, "Solo entry did not use safe centre");
            h.assertTrue(players.getFirst().getYRot() == 135 && players.getFirst().getXRot() == -25, "Configured view not applied");
            sessions.get(players.getFirst().getUUID()).finish(InstanceResult.SUCCESS);
            queue.clear(); Collections.fill(arrivalSlots, ready);
            // Remove the centre and two edge locations: a wall, a pit and lava must all be avoided.
            h.getLevel().setBlock(arrivalBlock, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            h.getLevel().setBlock(arrivalBlock.offset(3, -1, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            h.getLevel().setBlock(arrivalBlock.offset(-3, 0, 0), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            for (var player : players) player.setPos(arrival.add(20, 0, 0));
            h.assertTrue(InstanceManager.start(List.of(players.get(0), players.get(1), players.get(2), players.get(0)), id), "Distributed party entry failed");
            var distributed = sessions.get(players.getFirst().getUUID());
            h.assertTrue(distributed.participantIds().size() == 3, "Duplicate target changed allocation count");
            for (int i = 0; i < players.size(); i++) {
                var player = players.get(i);
                h.assertTrue(player.position().distanceToSqr(arrival) <= 9 && player.getY() == arrival.y, "Arrival outside horizontal radius");
                h.assertTrue(player.getYRot() == 135 && player.getXRot() == -25, "Party view differs from configuration");
                h.assertTrue(h.getLevel().noCollision(player, player.getBoundingBox()) && !h.getLevel().containsAnyLiquid(player.getBoundingBox()), "Unsafe arrival chosen");
                h.assertTrue(h.getLevel().getBlockCollisions(player, player.getBoundingBox().move(0, -.05, 0)).iterator().hasNext(), "Arrival has no floor");
                for (int j = 0; j < i; j++) h.assertTrue(player.distanceToSqr(players.get(j)) >= 1
                        && !player.getBoundingBox().intersects(players.get(j).getBoundingBox()), "Party members overlap");
            }
            distributed.finish(InstanceResult.SUCCESS);
            for (var player : players) h.assertTrue(scoreboard.getOrCreatePlayerScore(player, intoCount).get() == 2,
                    "Omitted into_function executed a callback or configured re-entry omitted one");
            queue.clear();
            h.getLevel().setBlock(arrivalBlock, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            arrivalJson.addProperty("safe_distance", .1);
            var smallRuntime = constructor.newInstance(InstanceDefinition.parse(arrivalJson), 4);
            var smallSlots = (List<Object>) field(runtimeType, "slots").get(smallRuntime);
            Collections.fill(smallSlots, ready); runtimes.put(id, smallRuntime);
            for (var player : players) player.setPos(arrival.add(20, 0, 0));
            var prepared = (InstanceSavedData) field(InstanceManager.class, "savedData").get(null);
            prepared.setPreparedVersion(id.toString(), 0, 123);
            h.assertTrue(!InstanceManager.start(players, id), "Insufficient space accepted");
            h.assertTrue(sessions.isEmpty() && queue.isEmpty() && smallSlots.getFirst() == ready
                    && prepared.preparedVersion(id.toString(), 0) == 123, "Rejected entry consumed a slot or changed membership");
            for (var player : players) h.assertTrue(player.position().equals(arrival.add(20, 0, 0))
                    && !player.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Partial party teleport occurred");
            System.out.println("INSTANCE_ARRIVAL_REVIEW PASS: configured view, radius, solo, party separation, obstacles and atomic refusal");

            var limitPlayers = new ArrayList<>(players);
            for (int i = 3; i < 6; i++) {
                var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "LimitTest" + i), false);
                var player = new ServerPlayer(server, h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
                player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
                    @Override public void send(Packet<?> packet) {}
                };
                extraPlayers.add(player); limitPlayers.add(player);
                online.put(player.getUUID(), player); onlinePlayers.add(player); h.getLevel().players().add(player);
            }
            var feedback = new ArrayList<net.minecraft.network.chat.Component>();
            var commandSource = server.createCommandSourceStack().withPermission(4).withSource(new net.minecraft.commands.CommandSource() {
                public void sendSystemMessage(net.minecraft.network.chat.Component message) { feedback.add(message); }
                public boolean acceptsSuccess() { return false; }
                public boolean acceptsFailure() { return true; }
                public boolean shouldInformAdmins() { return false; }
            });
            for (String config : List.of("null", "5", "{\"min\":3,\"max\":5}")) {
                var json = spawnConfig(id, arrival);
                json.remove("rotation"); json.remove("safe_distance");
                if (config.equals("null")) json.remove("player_limit_count");
                else json.add("player_limit_count", com.google.gson.JsonParser.parseString(config));
                var configured = InstanceDefinition.parse(json);
                for (int count : List.of(1, 2, 3, 5, 6)) {
                    var configuredRuntime = constructor.newInstance(configured, 4);
                    var configuredSlots = (List<Object>) field(runtimeType, "slots").get(configuredRuntime);
                    Collections.fill(configuredSlots, ready); runtimes.put(id, configuredRuntime); queue.clear(); feedback.clear();
                    prepared.setPreparedVersion(id.toString(), 0, 123);
                    for (int i = 0; i < limitPlayers.size(); i++) {
                        var player = limitPlayers.get(i); player.setPos(arrival.add(20, 0, 0));
                        player.setYRot(67); player.setXRot(-21);
                        player.removeTag("party_limit_test"); if (i < count) player.addTag("party_limit_test");
                    }
                    boolean allowed = count >= configured.playerLimitCount().min() && count <= configured.playerLimitCount().max();
                    int result = dispatcher.execute("plre instance start party_command_test @a[tag=party_limit_test]", commandSource);
                    h.assertTrue(result == (allowed ? count : 0), "Wrong selector limit result: " + config + " / " + count);
                    if (allowed) {
                        h.assertTrue(sessions.size() == count && feedback.isEmpty(), "Allowed entry failed");
                        for (int i = 0; i < count; i++) h.assertTrue(limitPlayers.get(i).getYRot() == 0
                                && limitPlayers.get(i).getXRot() == 0, "Omitted rotation did not default to zero");
                        sessions.get(limitPlayers.getFirst().getUUID()).finish(InstanceResult.SUCCESS);
                    } else {
                        h.assertTrue(sessions.isEmpty() && queue.isEmpty() && configuredSlots.getFirst() == ready
                                && prepared.preparedVersion(id.toString(), 0) == 123, "Rejected count consumed a slot");
                        for (var player : limitPlayers) h.assertTrue(player.position().equals(arrival.add(20, 0, 0))
                                && !player.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Count rejection partly teleported the team");
                        h.assertTrue(feedback.size() == 1, "Command issuer did not receive refusal reason");
                        var reason = (net.minecraft.network.chat.contents.TranslatableContents) feedback.getFirst().getSiblings().getFirst().getContents();
                        boolean low = count < configured.playerLimitCount().min();
                        h.assertTrue(reason.getKey().equals("command.panlingre.instance." + (low ? "too_few_players" : "too_many_players"))
                                && reason.getArgs()[0].equals(low ? configured.playerLimitCount().min() : configured.playerLimitCount().max())
                                && reason.getArgs()[1].equals(count), "Refusal reason has incorrect limit or selected count");
                    }
                }
            }
            System.out.println("INSTANCE_PLAYER_LIMIT_REVIEW PASS: default/int/range x 1/2/3/5/6 players; defaults and issuer feedback");

            // Drive the real emperor finale at both sides of the 60-tick remnant boundary.
            var scene = new icu.icuqalt10.panlingre.instance.shihuang.ShiHuangScene(h.absolutePos(net.minecraft.core.BlockPos.ZERO));
            var finaleController = new icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController();
            var finale = new InstanceSession(server, id, definition, 0, players, finaleController);
            field(finaleController.getClass(), "session").set(finaleController, finale);
            field(finaleController.getClass(), "scene").set(finaleController, scene);
            for (var player : players) {
                player.setHealth(player.getMaxHealth()); player.setPos(scene.coffinMouth());
                player.removeTag("party_test_success");
                player.addTag(InstanceManager.ACTIVE_PLAYER_TAG); sessions.put(player.getUUID(), finale);
            }
            // This eliminated player remains alive inside the room but must not receive rewards.
            InstanceManager.failPlayer(players.get(2));
            var emperor = icu.icuqalt10.panlingre.init.ModEntities.SHI_HUANG.get().create(h.getLevel());
            emperor.setPos(scene.coffinMouth()); finaleController.bindEmperor(emperor);
            emperor.die(h.getLevel().damageSources().generic());
            var startKey = (net.minecraft.network.syncher.EntityDataAccessor<Long>) field(emperor.getClass(), "START").get(null);
            var tickFinale = emperor.getClass().getDeclaredMethod("tickFinale"); tickFinale.setAccessible(true);
            int vanish = icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity.VANISH_TICK;
            h.assertTrue(icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.duration("death") * 20 == vanish,
                    "Finale timer disagrees with death animation duration");
            var lastFrame = icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.sample("death", vanish / 20.0, false);
            for (int held = 0; held < 60; held++) {
                emperor.getEntityData().set(startKey, h.getLevel().getGameTime() - vanish - held);
                h.assertTrue(emperor.pose(.5F).bones().equals(lastFrame.bones()), "Death pose changed during last-frame hold");
            }
            emperor.getEntityData().set(startKey, h.getLevel().getGameTime() - vanish - 59);
            tickFinale.invoke(emperor);
            h.assertTrue(!finale.isEnding() && !emperor.isRemoved(), "Sword removed before three-second hold ended");
            emperor.getEntityData().set(startKey, h.getLevel().getGameTime() - vanish - 60);
            tickFinale.invoke(emperor);
            h.assertTrue(finale.isEnding() && emperor.isRemoved() && InstanceManager.sessionFor(emperor) == null && sessions.isEmpty(),
                    "Finale did not clear session, ownership and planted sword");
            for (int i = 0; i < 2; i++) {
                var player = players.get(i);
                h.assertTrue(!player.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG), "Finale left active tag");
                h.assertTrue(player.getTags().contains("party_test_success"), "Configured success callback missing");
                h.assertTrue(!player.getTags().contains("party_reward_clean"), "Removed reward function was still called");
                h.assertTrue(!player.getTags().contains("party_reward_duplicate"), "Duplicate reward");
            }
            h.assertTrue(!players.get(2).getTags().contains("party_reward_clean"), "Eliminated member received reward");
            h.assertTrue(!players.get(2).getTags().contains("party_test_success"), "Eliminated member received success callback");
            finaleController.completeEmperor(emperor);
            h.assertTrue(players.stream().noneMatch(p -> p.getTags().contains("party_reward_clean")), "Repeated finale called removed reward function");
            h.assertTrue(players.stream().noneMatch(p -> p.getTags().contains("party_reward_duplicate")), "Repeated finale awarded twice");
            h.succeed();
        } finally {
            sessions.clear(); sessions.putAll(savedSessions); runtimes.clear(); runtimes.putAll(savedRuntimes);
            queue.clear(); queue.addAll(savedQueue); activeBuild.set(null, oldBuild); activeRequest.set(null, oldRequest);
            for (var player : java.util.stream.Stream.concat(players.stream(), extraPlayers.stream()).toList()) {
                online.remove(player.getUUID()); onlinePlayers.remove(player); h.getLevel().players().remove(player);
                player.getTextFilter().leave(); player.discard();
            }
        }
    }
}
