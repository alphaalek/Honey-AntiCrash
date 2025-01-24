package me.alek.honey;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

public class DummyPlugin implements Plugin {

    @Override
    public File getDataFolder() {
        return null;
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return null;
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return null;
    }

    @Override
    public FileConfiguration getConfig() {
        return null;
    }

    @Override
    public InputStream getResource(String s) {
        return null;
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(String s, boolean b) {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public @NotNull PluginLoader getPluginLoader() {
        return null;
    }

    @Override
    public Server getServer() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(boolean b) {

    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String s, String s1) {
        return null;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull String s, @Nullable String s1) {
        return null;
    }

    @Override
    public Logger getLogger() {
        return HoneyPlugin.get().getPlugin().getLogger();
    }

    @Override
    public String getName() {
        return "Honey-Failed-To-Load";
    }

    @Override
    public @NotNull LifecycleEventManager<Plugin> getLifecycleManager() {
        return null;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
