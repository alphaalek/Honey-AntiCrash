package me.alek.honey.checks.impl;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.entity.Player;

public class PayloadE implements PacketCheck<WrappedPlayInCustomPayload> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
        String channel = packet.getChannel();
        ByteBuf serializer = container.getSerializer(0);

        if (channel.toLowerCase().contains("register")) {
            int channelSize = player.getListeningPluginChannels().size();

            for (int i = 0; i < serializer.toString(Charsets.UTF_8).split("\000").length; i++) {
                if (++channelSize > ConfigurationOptions.CustomPayload.MAX_PLUGIN_CHANNELS) {
                    serializer.release();
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .punish()
                            .build();
                }
            }
        }
        return ResponseBuilder.SUCCESS;
    }
}
