//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123，烟花
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class BotConfigManager implements OneBotConfig {
    private static final BotConfigManager INSTANCE = new BotConfigManager();

    private static volatile ConfigHolder<BotConfig> holder;

    //临时覆盖仅保存于内存
    private static volatile String hostOverride;
    private static volatile Integer portOverride;
    private static volatile String pathOverride;
    private static volatile boolean tokenOverridden;
    private static volatile String tokenOverride;
    private static final Set<Long> runtimeAddedGroupIds = ConcurrentHashMap.newKeySet();
    private static final Set<Long> runtimeRemovedGroupIds = ConcurrentHashMap.newKeySet();

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
        String override = hostOverride;
        return override != null ? override : config().host;
    }

    @Override
    public int getPort() {
        Integer override = portOverride;
        return override != null ? override : config().port;
    }

    @Override
    public String getPath() {
        String override = pathOverride;
        return override != null ? override : config().path;
    }

    @Override
    public String getToken() {
        String token = tokenOverridden ? tokenOverride : config().token;
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

    //临时生效配置相关
    public static List<Long> effectiveGroupIds(){
        Set<Long> ids = new LinkedHashSet<>(config().groupIds);
        ids.removeAll(runtimeRemovedGroupIds);
        ids.addAll(runtimeAddedGroupIds);
        return List.copyOf(ids);
    }

    public static boolean addRuntimeGroup(long groupId){
        synchronized (BotConfigManager.class){
            if (effectiveGroupIds().contains(groupId)){
                return false;
            }
            runtimeRemovedGroupIds.remove(groupId);
            if (config().groupIds.contains(groupId)){
                return true;
            }
            return runtimeAddedGroupIds.add(groupId);
        }
    }

    public static boolean removeRuntimeGroup(long groupId){
        synchronized (BotConfigManager.class){
            if (!effectiveGroupIds().contains(groupId)){
                return false;
            }
            runtimeAddedGroupIds.remove(groupId);
            if (config().groupIds.contains(groupId)){
                return runtimeRemovedGroupIds.add(groupId);
            }
            return true;
        }
    }

    public static Set<Long> getRuntimeAddedGroupIds(){
        return Set.copyOf(runtimeAddedGroupIds);
    }

    public static Set<Long> getRuntimeRemovedGroupIds(){
        return Set.copyOf(runtimeRemovedGroupIds);
    }

    public static boolean reload(){
        boolean loaded = getConfigHolder().load();
        if (loaded){
            clearRuntimeOverrides();
        }
        return loaded;
    }

    public static void clearRuntimeOverrides(){
        synchronized (BotConfigManager.class){
            hostOverride = null;
            portOverride = null;
            pathOverride = null;
            tokenOverridden = false;
            tokenOverride = null;
            runtimeAddedGroupIds.clear();
            runtimeRemovedGroupIds.clear();
        }
    }

    public static void setHostOverride(String host){
        hostOverride = host;
    }

    public static void setPortOverride(int port){
        portOverride = port;
    }

    public static void setPathOverride(String path){
        pathOverride = path;
    }

    public static void setTokenOverride(String token){
        tokenOverride = token;
        tokenOverridden = true;
    }

    public static String getHostOverride(){
        return hostOverride;
    }

    public static Integer getPortOverride(){
        return portOverride;
    }

    public static String getPathOverride(){
        return pathOverride;
    }

    public static boolean hasTokenOverride(){
        return tokenOverridden;
    }

    public static String serverName(){
        return config().serverName;
    }
}
