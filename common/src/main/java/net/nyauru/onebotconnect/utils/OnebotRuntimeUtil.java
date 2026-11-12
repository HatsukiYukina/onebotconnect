//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import dev.architectury.utils.GameInstance;
import net.minecraft.server.MinecraftServer;
import net.nyauru.onebotconnect.config.BotConfigManager;
import net.nyauru.onebotwslib.adapter.client.OneBotClient;
import net.nyauru.onebotwslib.adapter.config.OneBotConfig;
import net.nyauru.onebotwslib.adapter.event.MessageEvent;
import net.nyauru.onebotwslib.adapter.event.MessageEventListener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class OnebotRuntimeUtil {
    public static final OneBotClient CLIENT = new OneBotClient(BotConfigManager.getInstance(),
            new MessageEventListener() {
                @Override
                public void onMessage(@NotNull MessageEvent event) {
                    //MessageEventListener.super.onMessage(event);
                    //此处暂时留空以供后续捕获消息事件时使用
                }
            },
            null);
    private static MinecraftServer server = GameInstance.getServer();
    public static void connect(){
        CLIENT.connectBlocking(Duration.ofSeconds(10));
        //demo内容
        //CLIENT.sendPrivateMessage(1652039138L,"服务器已启动喵");
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
