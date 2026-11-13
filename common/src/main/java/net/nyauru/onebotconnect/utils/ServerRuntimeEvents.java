//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nyauru.onebotconnect.config.BotConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ServerRuntimeEvents {
    private static final Logger LOGGER = LogManager.getLogger("onebotconnect");
    public static void register(){
        //玩家加入播报
        PlayerEvent.PLAYER_JOIN.register(player -> {
            sendGroupMessage(player.getPlainTextName() + "加入了服务器");
        });
        //玩家退出播报
        PlayerEvent.PLAYER_QUIT.register(player -> {
            sendGroupMessage(player.getPlainTextName() + "离开了服务器");
        });
        //玩家死亡播报
        EntityEvent.LIVING_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if(entity instanceof Player player){
                if (source.getEntity() != null) {
                    sendGroupMessage(player.getPlainTextName() + " was slain by " + source.getEntity().getPlainTextName());
                } else {
                    sendGroupMessage(player.getPlainTextName() + " 死了");
                }
            }
            return EventResult.pass();
        });
        //玩家消息回传到onebot
        ChatEvent.RECEIVED.register(((player, component) -> {
            if(OnebotRuntimeUtil.CLIENT.isConnected()){
                if (player != null) {
                    sendGroupMessage("[" + BotConfigManager.serverName() + "]<" + player.getPlainTextName() + "> " + component.getString());
                }
            }
            return EventResult.pass();
        }));
        //玩家成就播报
        PlayerEvent.PLAYER_ADVANCEMENT.register((serverPlayer, advancementHolder) -> {
            //取得玩家当前成就信息
            Advancement advancement = advancementHolder.value();
            DisplayInfo info = advancement.display().get();
            //不展示技术性进度
            if(!info.shouldAnnounceChat()){
                return;
            }
            Component advancementTitle = info.getTitle();

            if (serverPlayer != null){
                sendGroupMessage(serverPlayer.getPlainTextName() + " has reached advancement " + advancementTitle.getString());
            }
        });
    }
    //@TODO 僵尸连接检测与清除
    private static void sendGroupMessage(String message){
        List<Long> groupIds = BotConfigManager.groupIds();
        if(OnebotRuntimeUtil.CLIENT.isConnected()){
            for(Long groupId : groupIds){
                OnebotRuntimeUtil.CLIENT.sendGroupMessage(groupId, message);
            }
        } else {
            LOGGER.warn("OneBot is NOT CONNECTED, skipping sending group message!");
        }

    }
}
