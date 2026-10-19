package icu.icuqalt10.panlingre.entity;

import com.mojang.authlib.GameProfile;
import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class CustomPelletTest {
    @GameTest(template = "empty", templateNamespace = "panlingre")
    public static void healingPelletsIncludeThrower(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var items = List.of(ModItems.hui_chun_1, ModItems.hui_chun_2, ModItems.hui_chun_3, ModItems.qi_si);
            int[] healing = {8, 16, 32, 96};
            for (int i = 0; i < items.size(); i++) {
                player.setHealth(10);
                splash(helper, player, items.get(i).get(), player.position());
                helper.assertTrue(player.getHealth() == 10 + healing[i],
                        items.get(i).getId() + " did not heal its thrower");
            }
            player.setHealth(10);
            splash(helper, player, ModItems.hui_chun_1.get(), player.position().add(8, 0, 0));
            helper.assertTrue(player.getHealth() == 10, "Out-of-range thrower was healed");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "panlingre")
    public static void beneficialPelletsRespectTeamsAndIncludeThrower(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var ally = helper.spawn(EntityType.COW, new BlockPos(2, 2, 1));
        var enemy = helper.spawn(EntityType.COW, new BlockPos(1, 2, 2));
        var scoreboard = helper.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("pellet_" + UUID.randomUUID().toString().substring(0, 8));
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        try {
            player.setHealth(10);
            ally.setHealth(2);
            enemy.setHealth(2);
            splash(helper, player, ModItems.hui_chun_1.get(), player.position());
            helper.assertTrue(player.getHealth() == 18 && ally.getHealth() == 10 && enemy.getHealth() == 2,
                    "Healing must reach thrower and teammate only");
            for (var item : List.of(ModItems.tian_shen_1, ModItems.tian_shen_2, ModItems.tian_shen_3,
                    ModItems.jiu_zhuan_1, ModItems.jiu_zhuan_2, ModItems.jiu_zhuan_3)) {
                player.removeAllEffects();
                ally.removeAllEffects();
                splash(helper, player, item.get(), player.position());
                var effect = item.getId().getPath().startsWith("tian_shen") ? MobEffects.DAMAGE_BOOST : MobEffects.REGENERATION;
                helper.assertTrue(player.hasEffect(effect) && ally.hasEffect(effect) && !enemy.hasEffect(effect),
                        item.getId() + " must buff thrower and teammate only");
            }
        } finally {
            scoreboard.removePlayerTeam(team);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "panlingre")
    public static void damagingPelletsStillExcludeThrowerAndTeammates(GameTestHelper helper) {
        var owner = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        var ally = helper.spawn(EntityType.COW, new BlockPos(2, 2, 1));
        var enemy = helper.spawn(EntityType.COW, new BlockPos(1, 2, 2));
        var scoreboard = helper.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("pellet_" + UUID.randomUUID().toString().substring(0, 8));
        scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        try {
            enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            for (var item : List.of(ModItems.feng_hou_1, ModItems.feng_hou_2, ModItems.feng_hou_3, ModItems.jian_xue)) {
                enemy.setHealth(1000);
                enemy.invulnerableTime = 0;
                splash(helper, owner, item.get(), owner.position());
                helper.assertTrue(enemy.getHealth() < 1000, item.getId() + " did not damage enemy");
                helper.assertTrue(owner.getHealth() == owner.getMaxHealth() && ally.getHealth() == ally.getMaxHealth(),
                        item.getId() + " damaged thrower or teammate");
            }
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    private static void splash(GameTestHelper helper, LivingEntity owner, Item item, Vec3 impact) {
        var pellet = new CustomPelletEntity(helper.getLevel(), owner);
        pellet.setItem(new ItemStack(item));
        pellet.onHit(new BlockHitResult(impact, Direction.UP, BlockPos.containing(impact), false));
        helper.assertTrue(pellet.isRemoved(), "Pellet was not consumed after impact");
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pellet-test"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        helper.getLevel().addNewPlayer(player);
        return player;
    }
}
