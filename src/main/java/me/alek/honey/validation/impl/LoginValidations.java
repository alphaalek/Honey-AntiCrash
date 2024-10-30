package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.validation.AbstractValidationPacketListener;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class LoginValidations extends AbstractValidationPacketListener<WrappedLoginInLoginStart> {

    public LoginValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Login.Client.LOGIN_START);
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedLoginInLoginStart>> createValidationPipeline() {
        return ValidationPipeline.from(new LoginA());
    }

    public static class LoginA implements PacketValidation<WrappedLoginInLoginStart> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedLoginInLoginStart packet, ValidationPipelineContext context) {
            WrappedGameProfile profile = packet.getProfile();
            if (profile == null) {
                return ValidationServiceContext.PASS;
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(profile.getName())) {
                    return ValidationServiceContext.Builder.validate().cancel().build();
                }
            }

            return ValidationServiceContext.PASS;
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
