package me.alek.honey;

import org.bukkit.plugin.java.JavaPlugin;

public class HoneyPluginInitializer extends JavaPlugin {

    private final HoneyAPI plugin = new HoneyPlugin(this, getDescription().getVersion(), getDescription().getVersion());

    @Override
    public void onEnable() {
        plugin.onEnable();
    }

    @Override
    public void onDisable() {
        plugin.onDisable();
    }
}
