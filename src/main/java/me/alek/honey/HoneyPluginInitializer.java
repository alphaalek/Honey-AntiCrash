package me.alek.honey;

import me.alek.honey.session.NoSessionEncryption;
import org.bukkit.plugin.java.JavaPlugin;

@NoSessionEncryption
public class HoneyPluginInitializer extends JavaPlugin {

    private static HoneyAPI plugin;

    public HoneyPluginInitializer() {
        if (plugin != null) {
            throw new UnsupportedOperationException("This class in singleton!");
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
