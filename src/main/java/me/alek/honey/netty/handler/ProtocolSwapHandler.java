package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;

public class ProtocolSwapHandler {

    public static void handleProtocolSwap(PacketContainer<? extends WrappedPacket<?>> packet) {
        if (packet.getType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                setProtocolState(packet.getChannel(), PacketState.PLAY);
            } else {
                setProtocolState(packet.getChannel(), PacketState.CONFIGURATION);
            }
        } else if (packet.getType() == PacketType.Play.Server.LOGIN) {
            setProtocolState(packet.getChannel(), PacketState.PLAY);
        } else if (packet.getType() == PacketType.Play.Server.START_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION);
        } else if (packet.getType() == PacketType.Login.Client.LOGIN_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION);
        } else if (packet.getType() == PacketType.Play.Client.CONFIGURATION_ACKNOWLEDGED) {
            setProtocolState(packet.getChannel(), PacketState.CONFIGURATION);
        } else if (packet.getType() == PacketType.Configuration.Client.FINISH_CONFIGURATION
                || packet.getType() == PacketType.Configuration.Server.FINISH_CONFIGURATION) {
            setProtocolState(packet.getChannel(), PacketState.PLAY);
        } else if (packet.getType() == PacketType.Handshake.Client.SET_PROTOCOL) {
            JavaConverter<Enum<?>, WrappedEnumProtocol> converter = Converters.getEnumProtocolConverter();
            int packetStateOrdinal;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_1)) {
                WrappedEnumProtocol protocol = packet.getModifier(NMS.getEnumProtocolClass(), converter).readField(0);
                packetStateOrdinal = ((Enum<?>)protocol.getHandle()).ordinal();
            } else {
                Object clientIntent = packet.getModifier(NMS.getClientIntentClass()).readField(0);
                int clientIntentOrdinal = ((Enum<?>)clientIntent).ordinal();
                packetStateOrdinal = clientIntentOrdinal == 0 ? 2 : /* intent ordinal = 1 */ 3;
            }
            PacketState packetState = PacketState.values()[packetStateOrdinal];
            setProtocolState(packet.getChannel(), packetState);
        }
    }

    private static void setProtocolState(Channel channel, PacketState state) {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).setProtocolState(state);
        } catch (MissingHandlerException ex) {
            channel.close();
        }
    }
}
