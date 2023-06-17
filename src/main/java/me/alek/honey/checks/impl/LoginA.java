package me.alek.honey.checks.impl;

import com.mojang.authlib.GameProfile;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoginA implements PacketCheck<WrappedLoginInLoginStart> {

    @Override
    public ResponseContainer accept(Player player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
        GameProfile profile = packet.getProfile();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(profile.getName())) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .notify("")
                        .build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }
}
