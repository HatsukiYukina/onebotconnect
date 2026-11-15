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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                    String killerName = source.getEntity().getPlainTextName();
                    String methodInfo = getMethodInfoByDamageSource(source);
                    if (methodInfo == null) {
                        //没有任何可识别的武器：使用默认播报
                        sendGroupMessage(player.getPlainTextName() + "死于" + killerName + "的毒手");
                    } else {
                        //有武器：拼接具体击杀方式
                        sendGroupMessage(player.getPlainTextName() + "被" + killerName + "下毒手" + methodInfo + "了");
                    }
                } else {
                    sendGroupMessage(player.getPlainTextName() + getInfoByDamageSource(source));
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
            DisplayInfo info = advancement.display().orElse(null);
            //不展示技术性进度与无展示信息的进度
            if(info == null || !info.shouldAnnounceChat()){
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
        List<Long> groupIds = BotConfigManager.effectiveGroupIds();
        if(OnebotRuntimeUtil.CLIENT.isConnected()){
            for(Long groupId : groupIds){
                OnebotRuntimeUtil.CLIENT.sendGroupMessage(groupId, message);
            }
        } else {
            LOGGER.warn("OneBot is NOT CONNECTED, skipping sending group message!");
        }
    }

    private static String getInfoByDamageSource(DamageSource source){
        if(source.is(DamageTypes.FALL)){
            return "摔死了";
        } else if(source.is(DamageTypes.DROWN)){
            return "淹死了";
        } else if(source.is(DamageTypes.IN_FIRE)){
            return "烧死了";
        } else if(source.is(DamageTypes.LAVA)){
            return "试图在岩浆里游泳";
        } else if(source.is(DamageTypes.STARVE)){
            return "饿死了";
        } else if(source.is(DamageTypes.FELL_OUT_OF_WORLD)){
            return "掉出了这个世界";
        } else if(source.is(DamageTypes.EXPLOSION)){
            return "爆炸了";
        }
        return "死了";
    }
    /**
     * 根据击杀方式返回播报用的动词（如“射死”“砍死”），无法识别武器时返回 null。
     * 返回值会拼接为“被[击杀者]下毒手[返回值]了”，因此这里不带“了”字。
     */
    //Assisted-by: DeepSeek:deepseek-flash
    //该代码使用了llm辅助，按必要的披露责任将使用的llm列出
    //This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
    private static String getMethodInfoByDamageSource(DamageSource source){
        //投掷物 / 特殊攻击：直接根据伤害类型判断
        if (source.is(DamageTypes.ARROW)) {
            return "射死";
        }
        if (source.is(DamageTypes.TRIDENT)) {
            return "戳死";
        }
        if (source.is(DamageTypes.SPEAR)) {
            return "刺死";
        }
        if (source.is(DamageTypes.MACE_SMASH)) {
            return "砸死";
        }
        if (source.is(DamageTypes.FIREWORKS)) {
            return "炸死";
        }
        if (source.is(DamageTypes.FIREBALL)) {
            return "烧死";
        }
        if (source.is(DamageTypes.THROWN)) {
            return "砸死";
        }
        if (source.is(DamageTypes.WIND_CHARGE)) {
            return "吹死";
        }

        //近战：根据击杀者主手武器判断（支持物品标签，兼容模组武器）
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            return null;
        }
        if (weapon.is(ItemTags.SWORDS)) {
            return "劈死";
        }
        if (weapon.is(ItemTags.AXES)) {
            return "砍死";
        }
        if (weapon.is(ItemTags.SPEARS)) {
            return "刺死";
        }
        if (weapon.is(Items.MACE)) {
            return "砸死";
        }
        if (weapon.is(Items.TRIDENT)) {
            return "戳死";
        }
        return null;
    }
}
