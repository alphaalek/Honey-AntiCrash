package me.alek.honey;

import org.bukkit.plugin.java.JavaPlugin;

public class HoneyPluginInitializer extends JavaPlugin {

    private static HoneyAPI plugin;

    public HoneyPluginInitializer() {
        if (plugin != null) {
            throw new UnsupportedOperationException("This class in singleton!");
        }

        plugin = new HoneyPlugin(this, getDescription().getVersion(), null);
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
