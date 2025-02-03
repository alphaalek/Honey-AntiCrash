package me.alek.honey.netty.handler;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.LegacyProtocol;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MinecraftReflection;

public class ProtocolSwapHandler {

    public void swapProtocolOutboundPostPipeline(PacketContainer<?> packet) {
        HoneyPacketHandler handler = packet.getPacketHandler();

        // login -> play (server -> client) (legacy)
        if (packet.getType() == PacketTypes.Login.Server.LOGIN_SUCCESS && NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            handler.setProtocolState(PacketBound.CLIENT, PacketState.PLAY);
            handler.setProtocolState(PacketBound.SERVER, PacketState.PLAY);
        // configuration -> play (server -> client)
        } else if (packet.getType() == PacketTypes.Configuration.Server.FINISH_CONFIGURATION) {
            handler.setProtocolState(PacketBound.CLIENT, PacketState.PLAY);
        // play -> configuration (server -> client)
        } else if (packet.getType() == PacketTypes.Play.Server.START_CONFIGURATION) {
            handler.setProtocolState(PacketBound.CLIENT, PacketState.CONFIGURATION);
        }
    }

    public void swapProtocolOutbound(PacketContainer<?> packet) {
        HoneyPacketHandler handler = packet.getPacketHandler();

        // login -> play (server -> client) (legacy)
        if (packet.getType() == PacketTypes.Login.Server.LOGIN_SUCCESS) {
            setConfigurationBridgePhase(handler, PacketBound.CLIENT);

            if (packet.getPlayer().clientInfo.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
                handler.setSimulatedServerboundProtocolState(PacketState.PLAY);
                handler.setSimulatedClientboundProtocolState(PacketState.PLAY);
            }
        // configuration -> play (server -> client)
        } else if (packet.getType() == PacketTypes.Configuration.Server.FINISH_CONFIGURATION) {
            setConfigurationBridgePhase(handler, PacketBound.CLIENT);
            handler.setSimulatedClientboundProtocolState(PacketState.PLAY);
        // play -> configuration (server -> client)
        } else if (packet.getType() == PacketTypes.Play.Server.START_CONFIGURATION) {
            setConfigurationBridgePhase(handler, PacketBound.CLIENT);
            handler.setSimulatedClientboundProtocolState(PacketState.CONFIGURATION);
        }
    }

    public void swapProtocolInboundFilter(PacketContainer<?> packet) {
        HoneyPacketHandler handler = packet.getPacketHandler();
        if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED) {
            handler.setSimulatedServerboundProtocolState(PacketState.CONFIGURATION);
            handler.setSimulatedClientboundProtocolState(PacketState.CONFIGURATION);
        }
        if (packet.getType() == PacketTypes.Configuration.Client.FINISH_CONFIGURATION) {
            handler.setSimulatedServerboundProtocolState(PacketState.PLAY);
        }
    }

    public void swapProtocolViaDropped(PacketContainer<?> packet) {
        if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED
                || packet.getType() == PacketTypes.Configuration.Client.FINISH_CONFIGURATION) {
            // already handled in swap protocol filter
            return;
        }
        swapProtocolInbound(packet);
    }

    public void swapProtocolInbound(PacketContainer<?> packet) {
        HoneyPacketHandler handler = packet.getPacketHandler();

        // login -> configuration (client -> server)
        if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED) {
            setConfigurationBridgePhase(handler, PacketBound.SERVER);

            // check if this protocol swap check has not already been called in the filter swap handler
            if (!packet.getCreatedPipelineState().equals(PacketContainer.PipelineState.INITIAL_BYTE) && packet.getPlayer().clientInfo.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                handler.setSimulatedServerboundProtocolState(PacketState.CONFIGURATION);
                handler.setSimulatedClientboundProtocolState(PacketState.CONFIGURATION);
            }
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                handler.setProtocolState(PacketBound.CLIENT, PacketState.CONFIGURATION);
                handler.setProtocolState(PacketBound.SERVER, PacketState.CONFIGURATION);
            }
        // configuration -> play (client -> server)
        } else if (packet.getType() == PacketTypes.Configuration.Client.FINISH_CONFIGURATION) {
            setConfigurationBridgePhase(handler, PacketBound.SERVER);
            handler.setGlobalServerboundProtocolState(PacketState.PLAY);
        // play -> configuration (client -> server)
        } else if (packet.getType() == PacketTypes.Play.Client.CONFIGURATION_START_ACKNOWLEDGED) {
            setConfigurationBridgePhase(handler, PacketBound.SERVER);
            handler.setSimulatedServerboundProtocolState(PacketState.CONFIGURATION);
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                handler.setProtocolState(PacketBound.SERVER, PacketState.CONFIGURATION);
            }
        // handshake -> status/login (client -> server)
        } else if (packet.getType() == PacketTypes.Handshake.Client.SET_PROTOCOL && packet.getObjectWrapped() != null) {
            setConfigurationBridgePhase(handler, PacketBound.SERVER);

            int packetStateOrdinal;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_20_1)) {
                LegacyProtocol protocol = packet.getStructure().getModifier(MinecraftReflection.getEnumProtocolClass(), Converters.getLegacyProtocolConverter(), packet.getType().getBound())
                        .readField(0);
                packetStateOrdinal = ((Enum<?>) protocol.getHandle()).ordinal();
            } else {
                Object clientIntent = packet.getStructure().getModifier(MinecraftReflection.getClientIntentClass()).readField(0);
                packetStateOrdinal = ((Enum<?>) clientIntent).ordinal() == 0 ? PacketState.STATUS.ordinal() : PacketState.LOGIN.ordinal();
            }

            PacketState intentState = PacketState.values()[packetStateOrdinal];
            handler.setGlobalServerboundProtocolState(intentState);
            handler.setGlobalClientboundProtocolState(intentState);
        }
    }
    
    private void setConfigurationBridgePhase(HoneyPacketHandler handler, PacketBound bound) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5) && !ConfigurationOptions.Decoder.USE_DEFAULT && handler.hasResolvedBound(bound)) {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(handler.getChannel()).setConfiguration(bound);
            } catch (MissingHandlerException ex) {
                ChannelCloseInspection.close(handler.getChannel());
            }
        }
    }
}
