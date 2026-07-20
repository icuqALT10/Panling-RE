package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class FireTornado {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("firetornado")
                        .then(Commands.literal("check")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(FireTornado::checkPlayerOnLava)
                                )
                        )
                                .then(Commands.literal("summon")
                                        .then(Commands.argument("target", Vec3Argument.vec3())
                                                .executes(context -> summonFireTornado(context, 2.0f, 15, null)) // 默认 2 秒（40 tick） 15伤害
                                                .then(Commands.argument("lifetime", FloatArgumentType.floatArg(0.5f, 60.0f))
                                                        .executes(context -> summonFireTornado(
                                                                context,
                                                                FloatArgumentType.getFloat(context, "lifetime"), 15,
                                                                null
                                                        ))
                                                        .then (Commands.argument("damage", FloatArgumentType.floatArg(0))
                                                            .executes(context -> summonFireTornado(context,
                                                                    FloatArgumentType.getFloat(context, "lifetime"),
                                                                    FloatArgumentType.getFloat(context, "damage"),
                                                                    null))
                                                                .then(Commands.argument("team", StringArgumentType.word())
                                                                        .executes(context -> summonFireTornado(
                                                                                context,
                                                                                FloatArgumentType.getFloat(context, "lifetime"),
                                                                                FloatArgumentType.getFloat(context, "damage"),
                                                                                StringArgumentType.getString(context, "team")
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                )
        );
    }

    private static int checkPlayerOnLava(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            Entity targetEntity = EntityArgument.getEntity(context, "entity");

            if (FireTrailTracker.isEntityInTrail(targetEntity)) {
                source.sendSuccess(
                        () -> Component.literal("§c实体 " + targetEntity.getName().getString() + " 正站在火龙卷岩浆块上！"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("§7实体 " + targetEntity.getName().getString() + " 不在火龙卷岩浆块上。"),
                        false
                );
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("无法找到该实体"));
            return 0;
        }
    }

    private static int summonFireTornado(CommandContext<CommandSourceStack> context, float lifetimeSeconds, float damage, String teamName) {
        CommandSourceStack source = context.getSource();
        Vec3 targetPos;

        try {
            targetPos = Vec3Argument.getVec3(context, "target");
        } catch (Exception e) {
            source.sendFailure(Component.literal("无效的目标位置"));
            return 0;
        }

        Vec3 spawnPos = source.getPosition();
        // 计算 tick（秒 * 20）
        int lifetimeTicks = (int)(lifetimeSeconds * 20);

        // 创建实体
        FireTornadoEntity tornado = new FireTornadoEntity(
                ModEntities.FIRE_TORNADO.get(), source.getLevel(),
                spawnPos, targetPos, lifetimeTicks, damage
        );


        // 如果指定了队伍，加入该队伍
        if (teamName != null && !teamName.isEmpty()) {
            PlayerTeam team = source.getLevel().getScoreboard().getPlayerTeam(teamName);
            if (team != null) {
                source.getLevel().getScoreboard().addPlayerToTeam(tornado.getStringUUID(), team);
            }
        }

        if (source.getLevel().addFreshEntity(tornado)) {
            source.sendSuccess(
                    () -> Component.literal(
                            String.format("已生成火龙卷 位置: %.1f %.1f %.1f -> %.1f %.1f %.1f (%.1f秒)%s",
                                    spawnPos.x, spawnPos.y, spawnPos.z,
                                    targetPos.x, targetPos.y, targetPos.z,
                                    lifetimeSeconds,
                                    teamName != null ? " 队伍: " + teamName : ""
                            )
                    ),
                    true
            );
            return 1;
        }

        source.sendFailure(Component.literal("生成火龙卷失败"));
        return 0;
    }
}
