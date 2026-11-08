package net.nyauru.onebotconnect.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.nyauru.onebotconnect.Onebotconnect;

@Mod(Onebotconnect.MOD_ID)
public final class OnebotconnectNeoForge {
    public OnebotconnectNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // Run our common setup.
        Onebotconnect.init();
    }
}
