package me.alek.honey.compat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public interface ServiceProvider<R> {

    R get();

    static <R> R initRegistration(Class<R> clazz) {
        RegisteredServiceProvider<R> provider = Bukkit.getServicesManager().getRegistration(clazz);
        if (provider != null) {
            return provider.getProvider();
        }

        throw new UnsupportedCompatabilityException();
    }
}
