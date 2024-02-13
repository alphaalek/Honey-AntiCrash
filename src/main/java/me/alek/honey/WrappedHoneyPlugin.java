package me.alek.honey;

import com.avaje.ebean.EbeanServer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

public class WrappedHoneyPlugin implements Plugin {

    private final Plugin delegate;

    public WrappedHoneyPlugin(Plugin delegate) {
        this.delegate = delegate;
    }

    @Override
    public File getDataFolder() {
        return delegate.getDataFolder();
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return delegate.getDescription();
    }

    @Override
    public FileConfiguration getConfig() {
        return delegate.getConfig();
    }

    @Override
    public InputStream getResource(String s) {
        return delegate.getResource(s);
    }

    @Override
    public void saveConfig() {
        delegate.saveConfig();
    }

    @Override
    public void saveDefaultConfig() {
        delegate.saveDefaultConfig();
    }

    @Override
    public void saveResource(String s, boolean b) {
        delegate.saveResource(s, b);
    }

    @Override
    public void reloadConfig() {
        delegate.reloadConfig();
    }

    @Override
    public PluginLoader getPluginLoader() {
        return delegate.getPluginLoader();
    }

    @Override
    public Server getServer() {
        return delegate.getServer();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void onDisable() {}

    @Override
    public void onLoad() {}

    @Override
    public void onEnable() {}

    @Override
    public boolean isNaggable() {
        return delegate.isNaggable();
    }

    @Override
    public void setNaggable(boolean b) {
        delegate.setNaggable(b);
    }

    @Override
    public EbeanServer getDatabase() {
        return delegate.getDatabase();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String s, String s1) {
        return delegate.getDefaultWorldGenerator(s, s1);
    }

    @Override
    public Logger getLogger() {
        return delegate.getLogger();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return delegate.onCommand(commandSender, command, s, strings);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return delegate.onTabComplete(commandSender, command, s, strings);
    }
}
