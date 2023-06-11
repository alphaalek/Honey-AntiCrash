package me.alek.honey.checks.impl.online;

import com.mojang.authlib.GameProfile;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.wrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.services.NotificationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AlreadyOnline extends AsyncPacketAdapter<WrappedLoginInLoginStart> implements Check {

    private static final NotificationService NOTIFICATION_SERVICE = HoneyPlugin.get().getNotificationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        GameProfile profile = packetContainer.getPacket().getProfile();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(profile.getName())) {
                packetContainer.cancel();
                NOTIFICATION_SERVICE.notify("some reason");
            }
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.LOGIN_START);
    }

    @Override
    public String getName() {
        return "Already Online";
    }
}
