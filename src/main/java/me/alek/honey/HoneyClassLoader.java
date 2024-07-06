package me.alek.honey;

import java.net.URL;
import java.net.URLClassLoader;

public class HoneyClassLoader extends URLClassLoader {

    private static final HoneyClassLoader LOADER = new HoneyClassLoader();

    public static HoneyClassLoader get() {
        return LOADER;
    }

    public HoneyClassLoader() {
        super(new URL[]{HoneyClassLoader.class.getProtectionDomain().getCodeSource().getLocation()}, ClassLoader.getSystemClassLoader());
    }
}
