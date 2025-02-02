package me.alek.honey;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class HoneyPluginInitializer extends JavaPlugin {

    private static HoneyAPI plugin;

    public HoneyPluginInitializer() {
        if (plugin != null) {
            throw new UnsupportedOperationException("This class in singleton!");
        }

        if (ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(i -> Arrays.asList("-agentlib", "-javaagent").contains(i))) {
            throw new UnsupportedOperationException("It is not allowed to use a JavaAgent for Honey!");
        }

        plugin = new HoneyPlugin(this, getDescription().getVersion(), null);
    }

    @Override
    public void onLoad() {
        plugin.onLoad();
    }

    @Override
    public void onEnable() {
        plugin.onEnable();
    }

    @Override
    public void onDisable() {
        plugin.onDisable();
    }
}
