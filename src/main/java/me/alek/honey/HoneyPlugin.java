package me.alek.honey;

import me.alek.honey.listeners.check.Check;
import me.alek.honey.listeners.check.CheckRegistry;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.wrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class HoneyPlugin extends JavaPlugin {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private boolean isRunning;
    private PacketLibrary packetLibrary;

    @Override
    public void onEnable() {
        isRunning = true;
        INSTANCE = this;

        packetLibrary = PacketLibrary.set(new PacketLibrarySettings(this, "alek_packets"));
        new Thread(() -> {
            // some logic

            violationService = new ViolationService();
            registerChecks();
        }).start();
    }

    @Override
    public void onDisable() {
        isRunning = false;

        PacketLibrary.get().disable();
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (CheckRegistry checkEnum : CheckRegistry.values()) {
            final Check check = checkEnum.getCheck().get();

            switch (check.getType()) {

                case NETTY: {
                    if (check instanceof AsyncPacketAdapter) {
                        PacketLibrary.get().addListeners((AsyncPacketAdapter<WP>) check, check.getTypes());
                    }
                    break;
                }
                case BUKKIT: {
                    PacketLibrary.get().getEventManager().addListener(check);
                    break;
                }
            }
        }
    }

    public static HoneyPlugin get() {
        return INSTANCE;
    }

    public PacketLibrary getPacketLibrary() {
        return packetLibrary;
    }

    public ViolationService getViolationService() {
        return violationService;
    }
}
