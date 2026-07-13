package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(FireTornado::checkPlayerOnLava)
                                )
                        )
                                .then(Commands.literal("summon")
                                        .then(Commands.argument("target", Vec3Argument.vec3())
                                                .executes(context -> summonFireTornado(context, 2.0f)) // 默认 2 秒（40 tick）
                                                .then(Commands.argument("lifetime", FloatArgumentType.floatArg(0.5f, 60.0f))
                                                        .executes(context -> summonFireTornado(
                                                                context,
                                                                FloatArgumentType.getFloat(context, "lifetime")
                                                        ))
                                                )
                                        )
                                )
                )
        );
    }

    private static int checkPlayerOnLava(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            Player targetPlayer = EntityArgument.getPlayer(context, "target");

            if (FireTrailTracker.isPlayerInTrail(targetPlayer)) {
                source.sendSuccess(
                        () -> Component.literal("§c玩家 " + targetPlayer.getName().getString() + " 正站在火龙卷岩浆块上！"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("§7玩家 " + targetPlayer.getName().getString() + " 不在火龙卷岩浆块上。"),
                        false
                );
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("无法找到该玩家"));
            return 0;
        }
    }

    private static int summonFireTornado(CommandContext<CommandSourceStack> context, float lifetimeSeconds) {
        CommandSourceStack source = context.getSource();
        Vec3 targetPos;

        try {
            targetPos = Vec3Argument.getVec3(context, "target");
        } catch (Exception e) {
            source.sendFailure(Component.literal("无效的目标位置"));
            return 0;
        }

        Vec3 spawnPos = source.getPosition();

        // 创建实体
        FireTornadoEntity tornado = new FireTornadoEntity(
                ModEntities.FIRE_TORNADO.get(),
                source.getLevel()
        );

        tornado.setPos(spawnPos);

        // 计算 tick（秒 * 20）
        int lifetimeTicks = (int)(lifetimeSeconds * 20);
        tornado.setMovementParameters(targetPos, lifetimeTicks);

        if (source.getLevel().addFreshEntity(tornado)) {
            source.sendSuccess(
                    () -> Component.literal(
                            String.format("已生成火龙卷 位置: %.1f %.1f %.1f -> %.1f %.1f %.1f (%.1f秒)",
                                    spawnPos.x, spawnPos.y, spawnPos.z,
                                    targetPos.x, targetPos.y, targetPos.z,
                                    lifetimeSeconds
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
