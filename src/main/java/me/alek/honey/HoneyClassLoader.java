package me.alek.honey;

import me.alek.honey.util.reflection.Reflection;

import java.net.URL;
import java.net.URLClassLoader;

public class HoneyClassLoader extends URLClassLoader {

    public static final Class<?> PLUGIN_CLASS_LOADER;
    public static final Class<?> PAPER_PLUGIN_CLASS_LOADER;

    static {
        PLUGIN_CLASS_LOADER = Reflection.getClass("org.bukkit.plugin.java.PluginClassLoader");

        Class<?> paperPluginClassLoaderClass = null;
        try {
            paperPluginClassLoaderClass = Reflection.getClassWithException("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
        } catch (Exception ignored) {
        }
        PAPER_PLUGIN_CLASS_LOADER = paperPluginClassLoaderClass;
    }

    private static final HoneyClassLoader LOADER = new HoneyClassLoader();

    public static HoneyClassLoader get() {
        return LOADER;
    }

    public HoneyClassLoader() {
        super(new URL[]{HoneyClassLoader.class.getProtectionDomain().getCodeSource().getLocation()}, getPluginPlatformClassLoader());
    }

    private static ClassLoader getPluginPlatformClassLoader() {
        ClassLoader loader = HoneyClassLoader.class.getClassLoader();
        while (!PLUGIN_CLASS_LOADER.isInstance(loader)
                && !(PAPER_PLUGIN_CLASS_LOADER != null && PAPER_PLUGIN_CLASS_LOADER.isInstance(loader))) {
            loader = loader.getParent();
        }

        return loader.getParent();
    }
}
