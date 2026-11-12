//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: DeepSeek:deepseek-flash
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: aurora4123

package net.nyauru.onebotconnect.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * onebotconnect 的配置文件（config/onebotconnect.json）。
 * 字段与 net.nyauru.onebotwslib.adapter.config.OneBotConfig 一一对应，
 * 时长统一以秒存储，默认值与 OneBotConfig 的默认值保持一致。
 */
@Config(name = "onebotconnect")
public class BotConfig implements ConfigData {
    public Scheme scheme = Scheme.WS;
    public String host = "127.0.0.1";
    public int port = 6700;
    public String path = "/";
    public String token = "";
    public int heartbeatIntervalSeconds = 5;
    public boolean autoReconnect = true;
    public int reconnectIntervalSeconds = 5;
    public int actionTimeoutSeconds = 30;
    //配置需要发送消息的群聊
    public List<Long> groupIds = new ArrayList<>();
    //配置服务器名称（可选）
    public String serverName = "";

    @Override
    public void validatePostLoad() throws ValidationException {
        if (scheme == null) {
            throw new ValidationException("scheme must not be null");
        }
        if (host == null || host.isBlank()) {
            throw new ValidationException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new ValidationException("port must be in 1-65535");
        }
        if (path == null || !path.startsWith("/")) {
            throw new ValidationException("path must start with /");
        }
        if (heartbeatIntervalSeconds < 0) {
            throw new ValidationException("heartbeatIntervalSeconds must not be negative");
        }
        if (reconnectIntervalSeconds < 0) {
            throw new ValidationException("reconnectIntervalSeconds must not be negative");
        }
        if (actionTimeoutSeconds <= 0) {
            throw new ValidationException("actionTimeoutSeconds must be positive");
        }
        List<Long> validGroupIds = new ArrayList<>();
        if(groupIds != null){
            for(Long groupId : groupIds){
                if(groupId != null && groupId > 0){
                    validGroupIds.add(groupId);
                }
            }
        }
        groupIds = validGroupIds;

        String validServerName = "MC";
        if(serverName.isEmpty()){
            serverName = validServerName;
        }
    }

    public enum Scheme {
        WS,
        WSS
    }
}
