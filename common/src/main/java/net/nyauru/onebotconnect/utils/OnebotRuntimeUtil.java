//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import dev.architectury.utils.GameInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.nyauru.onebotconnect.config.BotConfigManager;
import net.nyauru.onebotwslib.adapter.client.OneBotClient;
import net.nyauru.onebotwslib.adapter.config.OneBotConfig;
import net.nyauru.onebotwslib.adapter.event.GroupMessageEvent;
import net.nyauru.onebotwslib.adapter.event.MessageEvent;
import net.nyauru.onebotwslib.adapter.event.MessageEventListener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class OnebotRuntimeUtil {
    private static MinecraftServer server = GameInstance.getServer();
    public static final OneBotClient CLIENT = new OneBotClient(BotConfigManager.getInstance(),
            new MessageEventListener() {
                @Override
                public void onMessage(@NotNull MessageEvent event) {
                    //MessageEventListener.super.onMessage(event);
                    //捕获到qq群聊消息后作为系统消息发送给玩家
                    if (event instanceof GroupMessageEvent group) {
                        long groupId = group.getGroupId();
                        //只放行 bot 所在群且已配置的群，避免所有群的消息都进入 Minecraft
                        if (!BotGroupCache.isKnownGroup(groupId) || !BotConfigManager.groupIds().contains(groupId)) {
                            return;
                        }
                    }
                    PlayerList playerList = server.getPlayerList();

                    for (Player p : playerList.getPlayers()) {
                        p.sendSystemMessage(Component.literal("[" + (event.getMessageType().equals("group") ? "群聊" : "私聊") + "]" + event.getSender().getNickname().toString() + ":" + event.getPlainText()));
                    }
                    System.out.println(event.getMessage());
                }
            },
            null);
    //private static MinecraftServer server = GameInstance.getServer();
    public static void connect(){
        BotGroupCache.register();
        CLIENT.connectBlocking(Duration.ofSeconds(10));
        //demo内容
        // CLIENT.sendPrivateMessage(1652039138L,"服务器已启动喵");
    }
    public static void close(){
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

}
