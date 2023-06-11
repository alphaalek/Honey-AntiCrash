package me.alek.honey.netty.api.example;

import me.alek.honey.listeners.check.Check;
import me.alek.honey.listeners.check.CheckType;
import me.alek.honey.netty.api.event.Listener;
import me.alek.honey.netty.api.event.impl.inject.PlayerInjectEvent;
import org.bukkit.Bukkit;

public class InjectListener implements Check {

    @Listener
    public void onInject(PlayerInjectEvent event) {
        Bukkit.getLogger().info(event.getPlayer().getName() + " blev injected!");
    }

    @Override
    public String getName() {
        return "Injector";
    }

    @Override
    public CheckType getType() {
        return CheckType.BUKKIT;
    }
}
