//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花

package net.nyauru.onebotconnect.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.nyauru.onebotconnect.config.BotConfigManager;
import net.nyauru.onebotconnect.utils.OnebotRuntimeUtil;
import net.nyauru.onebotwslib.adapter.client.OneBotConnectionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//注册/onebotconnect
public final class OnebotCommand {
    private static final Logger LOGGER = LogManager.getLogger("onebotconnect");
    //防止同时触发
    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);

    private OnebotCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registry,
                                Commands.CommandSelection selection) {
        dispatcher.register(
                Commands.literal("onebotconnect")
                        //仅权限2
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        //后续命令在这追加
                        .then(Commands.literal("connect")
                                .executes(OnebotCommand::executeConnect))
                        .then(Commands.literal("disconnect")
                                .executes(OnebotCommand::executeDisconnect))
                        .then(Commands.literal("addgroup")
                                .then(Commands.argument("group", LongArgumentType.longArg(1))
                                        .executes(OnebotCommand::executeAddGroup)))
                        .then(Commands.literal("delgroup")
                                .then(Commands.argument("group", LongArgumentType.longArg(1))
                                        .executes(OnebotCommand::executeDelGroup)))
                        .then(Commands.literal("reload")
                                .executes(OnebotCommand::executeReload))
                        .then(Commands.literal("setwebsocketip")
                                .then(Commands.argument("ip", StringArgumentType.greedyString())
                                        .executes(OnebotCommand::executeSetWebsocketIp)))
                        .then(Commands.literal("setwebsocketport")
                                .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                                        .executes(OnebotCommand::executeSetWebsocketPort)))
                        .then(Commands.literal("setwebsocketpath")
                                .then(Commands.argument("path", StringArgumentType.greedyString())
                                        .executes(OnebotCommand::executeSetWebsocketPath)))
                        .then(Commands.literal("setwebsockettoken")
                                .then(Commands.argument("token", StringArgumentType.greedyString())
                                        .executes(OnebotCommand::executeSetWebsocketToken)))
                        .then(Commands.literal("status")
                                .executes(OnebotCommand::executeStatus)));
    }

    private static int executeConnect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        //已连接时直接提示
        if (OnebotRuntimeUtil.CLIENT.isConnected()) {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.connect.already_connected"), false);
            return 0;
        }
        return startConnect(source, false, "onebotconnect.command.connect.connecting");
    }

    private static int executeDisconnect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        //断开连接并取消等待中的自动重连
        OnebotRuntimeUtil.CLIENT.disconnect();
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.disconnect.success",
                describeState(OnebotRuntimeUtil.CLIENT.getState())), false);
        return 1;
    }

    private static int executeAddGroup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        long groupId = LongArgumentType.getLong(context, "group");

        if (BotConfigManager.addRuntimeGroup(groupId)) {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.group.added",
                    groupId, String.valueOf(BotConfigManager.effectiveGroupIds())), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.group.already_present",
                groupId), false);
        return 0;
    }

    private static int executeDelGroup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        long groupId = LongArgumentType.getLong(context, "group");

        if (BotConfigManager.removeRuntimeGroup(groupId)) {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.group.removed",
                    groupId, String.valueOf(BotConfigManager.effectiveGroupIds())), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.group.not_present",
                groupId), false);
        return 0;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean wasConnected = OnebotRuntimeUtil.CLIENT.isConnected();

        boolean loaded;
        try {
            //重新读取配置文件，成功后清除所有临时覆盖
            loaded = BotConfigManager.reload();
        } catch (Throwable t) {
            LOGGER.error("[ONEBOT]Reload config failed", t);
            source.sendFailure(Component.translatable("onebotconnect.command.reload.exception",
                    String.valueOf(t.getMessage())));
            return 0;
        }

        if (!loaded) {
            source.sendFailure(Component.translatable("onebotconnect.command.reload.reset_to_default"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("onebotconnect.command.reload.success"), false);

        //与修改前一样处于已连接状态时才自动重连
        if (wasConnected) {
            startConnect(source, true, "onebotconnect.command.reload.reconnecting");
        }
        return 1;
    }

    private static int executeSetWebsocketIp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String ip = StringArgumentType.getString(context, "ip").trim();
        if (ip.isEmpty()) {
            source.sendFailure(Component.translatable("onebotconnect.command.set.ip.empty"));
            return 0;
        }

        BotConfigManager.setHostOverride(ip);
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.set.ip.success", ip), false);
        return 1;
    }

    private static int executeSetWebsocketPort(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int port = IntegerArgumentType.getInteger(context, "port");

        BotConfigManager.setPortOverride(port);
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.set.port.success", port), false);
        return 1;
    }

    private static int executeSetWebsocketPath(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawPath = StringArgumentType.getString(context, "path").trim();
        if (rawPath.isEmpty()) {
            source.sendFailure(Component.translatable("onebotconnect.command.set.path.empty"));
            return 0;
        }

        String path = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        BotConfigManager.setPathOverride(path);
        source.sendSuccess(() -> Component.translatable("onebotconnect.command.set.path.success", path), false);
        return 1;
    }

    private static int executeSetWebsocketToken(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String token = StringArgumentType.getString(context, "token").trim();

        BotConfigManager.setTokenOverride(token);
        if (token.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.set.token.cleared"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.set.token.set"), false);
        }
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BotConfigManager config = BotConfigManager.getInstance();

        MutableComponent status = Component.empty();
        status.append(Component.translatable("onebotconnect.command.status.state",
                describeState(OnebotRuntimeUtil.CLIENT.getState())));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.connected",
                OnebotRuntimeUtil.CLIENT.isConnected()));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.websocket",
                config.getScheme() + "://" + config.getHost() + ":" + config.getPort() + config.getPath()));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.overrides", describeOverrides()));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.token",
                Component.translatable(config.getToken() == null
                        ? "onebotconnect.command.status.token.not_set"
                        : "onebotconnect.command.status.token.set")));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.groups.effective",
                String.valueOf(BotConfigManager.effectiveGroupIds())));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.groups.config",
                String.valueOf(BotConfigManager.groupIds())));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.groups.added",
                String.valueOf(BotConfigManager.getRuntimeAddedGroupIds())));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.groups.removed",
                String.valueOf(BotConfigManager.getRuntimeRemovedGroupIds())));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.server_name",
                BotConfigManager.serverName()));
        status.append(Component.literal("\n"));
        status.append(Component.translatable("onebotconnect.command.status.heartbeat",
                config.getHeartbeatInterval().toSeconds(), config.getAutoReconnect(),
                config.getReconnectInterval().toSeconds()));

        source.sendSuccess(() -> status, false);
        return 1;
    }

    private static Component describeOverrides() {
        List<Component> overrides = new ArrayList<>();
        if (BotConfigManager.getHostOverride() != null) {
            overrides.add(Component.translatable("onebotconnect.command.status.override.ip",
                    BotConfigManager.getHostOverride()));
        }
        if (BotConfigManager.getPortOverride() != null) {
            overrides.add(Component.translatable("onebotconnect.command.status.override.port",
                    BotConfigManager.getPortOverride()));
        }
        if (BotConfigManager.getPathOverride() != null) {
            overrides.add(Component.translatable("onebotconnect.command.status.override.path",
                    BotConfigManager.getPathOverride()));
        }
        if (BotConfigManager.hasTokenOverride()) {
            overrides.add(Component.translatable("onebotconnect.command.status.override.token"));
        }
        if (overrides.isEmpty()) {
            return Component.translatable("onebotconnect.command.status.override.none");
        }

        MutableComponent result = Component.empty();
        for (int i = 0; i < overrides.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(", "));
            }
            result.append(overrides.get(i));
        }
        return result;
    }

    private static int startConnect(CommandSourceStack source, boolean forceDisconnect, String pendingKey) {
        if (!CONNECTING.compareAndSet(false, true)) {
            source.sendSuccess(() -> Component.translatable("onebotconnect.command.connect.busy"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable(pendingKey), false);

        MinecraftServer server = source.getServer();
        try {
            Thread.ofPlatform()
                    .daemon()
                    .name("onebotconnect-command-connect")
                    .start(() -> connectAsync(server, source, forceDisconnect));
        } catch (Throwable t) {
            CONNECTING.set(false);
            LOGGER.error("[ONEBOT]Failed to start manual connect thread", t);
            source.sendFailure(Component.translatable("onebotconnect.command.connect.thread_failed",
                    String.valueOf(t.getMessage())));
            return 0;
        }
        return 1;
    }

    private static void connectAsync(MinecraftServer server, CommandSourceStack source, boolean forceDisconnect) {
        boolean connected = false;
        Component stateText;
        String stateLog;
        try {
            if (forceDisconnect) {
                OnebotRuntimeUtil.CLIENT.disconnect();
            }
            //复用启动时的连接与播报逻辑
            connected = OnebotRuntimeUtil.connect();
            stateText = describeState(OnebotRuntimeUtil.CLIENT.getState());
            stateLog = stateText.getString();
        } catch (Throwable t) {
            stateText = Component.literal(t.getClass().getSimpleName() + ": " + t.getMessage());
            stateLog = stateText.getString();
            LOGGER.error("[ONEBOT]Manual connect failed", t);
        } finally {
            CONNECTING.set(false);
        }

        final boolean result = connected;
        final Component detail = stateText;
        LOGGER.info("[ONEBOT]Manual connect result: {}, state: {}", result, stateLog);

        try {
            //回到服务器主线程
            server.execute(() -> {
                if (result) {
                    source.sendSuccess(() -> Component.translatable("onebotconnect.command.connect.success"), false);
                } else {
                    source.sendFailure(Component.translatable("onebotconnect.command.connect.failed", detail));
                }
            });
        } catch (Throwable t) {
            LOGGER.warn("[ONEBOT]Unable to report manual connect result to command source", t);
        }
    }

    private static Component describeState(OneBotConnectionState state) {
        if (state instanceof OneBotConnectionState.Reconnecting r) {
            return Component.translatable("onebotconnect.state.reconnecting", r.getDelay().toSeconds());
        }
        if (state instanceof OneBotConnectionState.Connected) {
            return Component.translatable("onebotconnect.state.connected");
        }
        if (state instanceof OneBotConnectionState.Connecting) {
            return Component.translatable("onebotconnect.state.connecting");
        }
        if (state instanceof OneBotConnectionState.Closing) {
            return Component.translatable("onebotconnect.state.closing");
        }
        return Component.translatable("onebotconnect.state.disconnected");
    }
}
