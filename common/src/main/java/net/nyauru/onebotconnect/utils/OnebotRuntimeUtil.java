//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123,烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.nyauru.onebotconnect.config.BotConfigManager;
import net.nyauru.onebotwslib.adapter.client.OneBotClient;
import net.nyauru.onebotwslib.adapter.client.OneBotConnectionListener;
import net.nyauru.onebotwslib.adapter.client.OneBotConnectionState;
import net.nyauru.onebotwslib.adapter.event.GroupMessageEvent;
import net.nyauru.onebotwslib.adapter.event.MessageEvent;
import net.nyauru.onebotwslib.adapter.event.MessageEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnebotRuntimeUtil {
    private static final Logger LOGGER = LogManager.getLogger("onebotconnect");
    private static final DateTimeFormatter CONNECT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd|HH:mm");
    private static final AtomicBoolean CONNECTION_LISTENER_REGISTERED = new AtomicBoolean(false);
    private static MinecraftServer server = GameInstance.getServer();
    public static final OneBotClient CLIENT = new OneBotClient(BotConfigManager.getInstance(),
            new MessageEventListener() {
                @Override
                public void onMessage(@NotNull MessageEvent event) {
                    //MessageEventListener.super.onMessage(event);
                    //捕获到qq群聊消息后作为系统消息发送给玩家
                    if (event instanceof GroupMessageEvent group) {
                        long groupId = group.getGroupId();
                        //只放行bot所在群且已配置的群，避免所有群的消息都进入Minecraft
                        if (!BotGroupCache.isKnownGroup(groupId) || !BotConfigManager.effectiveGroupIds().contains(groupId)) {
                            return;
                        }
                    }
                    PlayerList playerList = server.getPlayerList();

                    for (Player p : playerList.getPlayers()) {
                        p.sendSystemMessage(Component.literal("[" + (event.getMessageType().equals("group") ? "群聊" : "私聊") + "]" + event.getSender().getNickname().toString() + ":" + event.getPlainText()));
                    }
                    //System.out.println(event.getMessage());
                }
            },
            null);
    //private static MinecraftServer server = GameInstance.getServer();

    public static boolean connect(){
        //初始化群聊列表
        BotGroupCache.register();
        //连接前注册一个状态监听器（只注册一次）
        if(CONNECTION_LISTENER_REGISTERED.compareAndSet(false, true)){
            CLIENT.addConnectionListener(new OneBotConnectionListener() {
                @Override
                public void onStateChanged(@NotNull OneBotConnectionState state) {
                    LOGGER.info("[ONEBOT] state -> {}", state);
                    if(state instanceof OneBotConnectionState.Reconnecting r){
                        LOGGER.warn("[ONEBOT] Reconnecting #{} in {}",r.getAttempt(), r.getDelay());
                    }
//                    else if (state instanceof OneBotConnectionState.Connected){
//                        for(Long groupId : BotConfigManager.groupIds()){
//                            CLIENT.sendGroupMessage(groupId, "▌ 群服互联已连接 ┈━═☆");
//                        }
//                    }
                }

                @Override
                public void onError(@NotNull Throwable error) {
                    LOGGER.error("[ONEBOT] Connection broken", error);
                }
            });
        }
        boolean connected = CLIENT.connectBlocking(Duration.ofSeconds(10));
        if(connected){
            try {
                //连接成功时发送消息
                String connectMessage = "OnebotConnect已连接\n"
                        + "游戏版本" + Platform.getMinecraftVersion()
                        + " 时间" + LocalDateTime.now().format(CONNECT_TIME_FORMAT)
                        + " 模组" + Platform.getMod("onebotconnect").getVersion() + "-" + loaderName()
                        + " 服务器[" + BotConfigManager.serverName() + "]";
                for(Long groupId : BotConfigManager.effectiveGroupIds()){
                    CLIENT.sendGroupMessage(groupId, connectMessage);
                }
            } catch (Throwable t) {
                //播报失败不应让调用方误判为连接失败
                LOGGER.warn("[ONEBOT] Connected but failed to broadcast announcement", t);
            }
            LOGGER.info("[ONEBOT] Connected to {}://{}:{}", BotConfigManager.config().scheme, BotConfigManager.config().host, BotConfigManager.config().port);
        } else if(CLIENT.getState() instanceof OneBotConnectionState.Reconnecting r){
            LOGGER.error("[ONEBOT] Connection FAILED, reconnecting #{} in {}", r.getAttempt(), r.getDelay());
        } else {
            LOGGER.error("[ONEBOT] Connection FAILED, current state: {}", CLIENT.getState());
        }
        //demo内容
        // CLIENT.sendPrivateMessage(1652039138L,"服务器已启动喵");
        return connected;
    }
    public static void close(){
        //在客户端集成服务器上不释放client对象以便下次启动时复用
        if(!server.isDedicatedServer()){
            disconnect();
        } else {
            CLIENT.close();
        }
    }
    public static void disconnect(){
        if(CLIENT.isConnected()){
            CLIENT.disconnect();
        }
    }

    private static String loaderName(){
        if(Platform.isFabric()){
            return "fabric";
        } else if(Platform.isNeoForge()){
            return "neoforge";
        }
        return "unknown";
    }

}
