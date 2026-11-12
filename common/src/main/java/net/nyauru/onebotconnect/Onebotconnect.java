//SPDX-License-Identifier: MIT
//Copyright (c) 2026 aurora4123
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: aurora4123

package net.nyauru.onebotconnect;

import dev.architectury.event.events.common.LifecycleEvent;
import net.nyauru.onebotconnect.config.BotConfigManager;
import net.nyauru.onebotconnect.utils.OnebotRuntimeUtil;

public final class Onebotconnect {
    public static final String MOD_ID = "onebotconnect";

    public static void init() {
        // Write common init code here.
        // 注册配置文件，启动时读取或生成 config/onebotconnect.json
        BotConfigManager.init();
        // 启动服务器时连接onebot（异步执行，避免阻塞服务器线程）
        LifecycleEvent.SERVER_STARTED.register(server ->
                Thread.ofPlatform().daemon().name("onebotconnect-connect").start(OnebotRuntimeUtil::connect));
        LifecycleEvent.SERVER_STOPPING.register(server -> OnebotRuntimeUtil.close());
    }

}
