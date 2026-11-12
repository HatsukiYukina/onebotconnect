//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nyauru.onebotconnect.config.BotConfigManager;

import java.util.List;

public class ServerRuntimeEvents {
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
    }
    private static void sendGroupMessage(String message){
        List<Long> groupIds = BotConfigManager.groupIds();
        for(Long groupId : groupIds){
            OnebotRuntimeUtil.CLIENT.sendGroupMessage(groupId, message);
        }
    }
}
