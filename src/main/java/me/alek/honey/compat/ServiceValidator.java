package me.alek.honey.compat;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.util.LazyInit;

public interface ServiceValidator {

    static ServiceValidator of(String plugin) {
        return new ServiceValidator() {

            private final LazyInit<Boolean> find = new LazyInit<>(() -> HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin(plugin) != null
                    && HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin(plugin).isEnabled());
            @Override
            public LazyInit<Boolean> find() {
                return this.find;
            }
        };
    }

    LazyInit<Boolean> find();
}
