package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.AbstractPacketListener;
import me.alek.honey.validation.CheckContextContainer;
import me.alek.honey.validation.CheckPipeline;
import me.alek.honey.validation.response.ResponseContainer;
import me.alek.honey.validation.response.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginValidations extends AbstractPacketListener<WrappedLoginInLoginStart> {

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public CheckPipeline<PacketValidation<WrappedLoginInLoginStart>> getNewPipeline() {
        return CheckPipeline.from(new LoginA());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Login.Client.LOGIN_START);
    }

    public static class LoginA implements PacketValidation<WrappedLoginInLoginStart> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            WrappedGameProfile profile = packet.getProfile();
            if (profile == null) {
                return Validation.PASS;
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(profile.getName())) {
                    return Validation.validate().cancel().punish().build();
                }
            }

            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Login.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.login.a");
        }
    }
}
