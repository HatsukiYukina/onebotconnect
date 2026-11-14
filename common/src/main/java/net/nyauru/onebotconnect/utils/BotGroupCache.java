//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: DeepSeek:deepseek-flash
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.utils;

import net.nyauru.onebotwslib.adapter.client.ActionData;
import net.nyauru.onebotwslib.adapter.client.ActionResponse;
import net.nyauru.onebotwslib.adapter.client.OneBotClient;
import net.nyauru.onebotwslib.adapter.client.OneBotConnectionListener;
import net.nyauru.onebotwslib.adapter.client.OneBotConnectionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//缓存bot所在群聊的id用于过滤进入Minecraft的群消息
public final class BotGroupCache {
    private static final OneBotClient CLIENT = OnebotRuntimeUtil.CLIENT;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Logger LOGGER = LogManager.getLogger("onebotconnect");
    private static volatile Map<Long, String> groups = Map.of(); // id -> 群名，方便打日志/做指令

    private BotGroupCache() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        CLIENT.addConnectionListener(new OneBotConnectionListener() {
            @Override
            public void onStateChanged(OneBotConnectionState state) {
                if (state instanceof OneBotConnectionState.Connected) {
                    refreshAsync();
                }
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }

    //当前缓存的群
    public static Set<Long> groupIds() {
        return groups.keySet();
    }

    public static Map<Long, String> groups() {
        return groups;
    }

    //是否属于bot所在群
    public static boolean isKnownGroup(long groupId) {
        LOGGER.debug("[ONEBOT] Current message is not in the whitelist groups,SKIPPING");
        return groups.containsKey(groupId);
    }

    //异步刷新
    public static void refreshAsync() {
        LOGGER.info("[ONEBOT] Getting existing groups...");
        CLIENT.callAction("get_group_list", Map.of()).whenComplete((response, error) -> {
            if (error != null) {
                LOGGER.warn("[ONEBOT] Unable to get existing groups", error);
                return; // 保留上一次结果
            }
            Map<Long, String> parsed = parse(response);
            if (!parsed.isEmpty()) {
                groups = Map.copyOf(parsed);
            }
            LOGGER.info("[ONEBOT] Got all existing groups");
        });
    }

    public static Set<Long> refreshNow() {
        try {
            Map<Long, String> parsed = parse(CLIENT.callAction("get_group_list", Map.of()).join());
            if (!parsed.isEmpty()) {
                groups = Map.copyOf(parsed);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return groupIds();
    }

    private static Map<Long, String> parse(ActionResponse response) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (response == null || !response.isSuccess() || !(response.getData() instanceof ActionData.ArrayData array)) {
            return result;
        }
        for (Object item : array.getValues()) {
            if (!(item instanceof Map<?, ?> group)) {
                continue;
            }
            Long id = toLong(group.get("group_id"));
            if (id == null) {
                continue;
            }
            Object name = group.get("group_name");
            result.put(id, name instanceof String s ? s : "");
        }
        return result;
    }

    private static Long toLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
