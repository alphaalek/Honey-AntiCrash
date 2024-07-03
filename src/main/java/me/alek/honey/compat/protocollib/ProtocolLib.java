package me.alek.honey.compat.protocollib;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.ServiceValidator;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.reflection.Reflection;

public class ProtocolLib {

    public static final String PACKAGE = "com.comphenix.protocol";
    public static Class<?> CHANNEL_PROXY;

    static {
        if (ProtocolLibValidator.VALIDATOR.find().get()) {
            CHANNEL_PROXY = Reflection.getUnsureClass(
                    PACKAGE + ".injector.netty.channel.NettyChannelProxy", // 5.0
                    PACKAGE + ".injector.netty.ChannelProxy" // 4.0
            );
        }
    }

    public static class ProtocolLibValidator implements ServiceValidator {

        public static final ServiceValidator VALIDATOR = new ProtocolLibValidator();

        public LazyInit<Boolean> find() {
            return new LazyInit<>(() -> HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("ProtocolLib") != null
                    && HoneyPlugin.get().getPlugin().getServer().getPluginManager().getPlugin("ProtocolLib").isEnabled());
        }
    }
}
