//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: DeepSeek:deepseek-flash
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.nyauru.onebotwslib.adapter.config.OneBotConfig;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 持有 Cloth Config 注册的 {@link BotConfig}，并把配置值实时传递给 {@link OneBotConfig}。
 * 每次读取都取自 holder 的当前配置实例，因此配置重载后无需重建 OneBotClient。
 */
public final class BotConfigManager implements OneBotConfig {
    private static final BotConfigManager INSTANCE = new BotConfigManager();

    private static volatile ConfigHolder<BotConfig> holder;

    private BotConfigManager() {
    }

    /** 触发配置注册，使配置文件在模组初始化阶段被读取或生成。 */
    public static void init() {
        getConfigHolder();
    }

    public static ConfigHolder<BotConfig> getConfigHolder() {
        ConfigHolder<BotConfig> current = holder;
        if (current != null) {
            return current;
        }
        synchronized (BotConfigManager.class) {
            if (holder == null) {
                holder = AutoConfig.register(BotConfig.class, GsonConfigSerializer::new);
            }
            return holder;
        }
    }

    public static BotConfig config() {
        return getConfigHolder().getConfig();
    }

    public static BotConfigManager getInstance() {
        return INSTANCE;
    }

    @Override
    public String getScheme() {
        return config().scheme.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getHost() {
        return config().host;
    }

    @Override
    public int getPort() {
        return config().port;
    }

    @Override
    public String getPath() {
        return config().path;
    }

    @Override
    public String getToken() {
        String token = config().token;
        return token == null || token.isBlank() ? null : token;
    }

    @Override
    public Duration getHeartbeatInterval() {
        return Duration.ofSeconds(config().heartbeatIntervalSeconds);
    }

    @Override
    public boolean getAutoReconnect() {
        return config().autoReconnect;
    }

    @Override
    public Duration getReconnectInterval() {
        return Duration.ofSeconds(config().reconnectIntervalSeconds);
    }

    @Override
    public Duration getActionTimeout() {
        return Duration.ofSeconds(config().actionTimeoutSeconds);
    }

    public static List<Long> groupIds(){
        return List.copyOf(config().groupIds);
    }

    public static String serverName(){
        return config().serverName;
    }
}
