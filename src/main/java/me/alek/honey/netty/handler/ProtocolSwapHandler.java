package me.alek.honey.netty.handler;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;
import me.alek.honey.util.protocol.NetProtocol;

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
        if (packet.getType() == PacketTypes.Login.Server.LOGIN_SUCCESS && packet.getProtocolCreated().isOlderThan(NetProtocol.v1_20_2)) {
            handler.setSimulatedServerboundProtocolState(PacketState.PLAY);
            handler.setSimulatedClientboundProtocolState(PacketState.PLAY);
        // configuration -> play (server -> client)
        } else if (packet.getType() == PacketTypes.Configuration.Server.FINISH_CONFIGURATION) {
            handler.setSimulatedClientboundProtocolState(PacketState.PLAY);
        // play -> configuration (server -> client)
        } else if (packet.getType() == PacketTypes.Play.Server.START_CONFIGURATION) {
            handler.setSimulatedClientboundProtocolState(PacketState.CONFIGURATION);
        }
    }

    public void swapProtocolInboundFilter(PacketContainer<?> packet) {
        HoneyPacketHandler handler = packet.getPacketHandler();

        // login -> configuration (client -> server)
        if (packet.getType() == PacketTypes.Login.Client.LOGIN_ACKNOWLEDGED) {
            handler.setSimulatedServerboundProtocolState(PacketState.CONFIGURATION);
            handler.setSimulatedClientboundProtocolState(PacketState.CONFIGURATION);
        }
        // configuration -> play (client -> server)
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
            handler.setGlobalServerboundProtocolState(PacketState.PLAY);
        // play -> configuration (client -> server)
        } else if (packet.getType() == PacketTypes.Play.Client.CONFIGURATION_START_ACKNOWLEDGED) {
            handler.setSimulatedServerboundProtocolState(PacketState.CONFIGURATION);
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                handler.setProtocolState(PacketBound.SERVER, PacketState.CONFIGURATION);
            }
        // handshake -> status/login (client -> server)
        } else if (packet.getType() == PacketTypes.Handshake.Client.SET_PROTOCOL) {
            WrappedHandshakeInSetProtocol handshake = (WrappedHandshakeInSetProtocol) packet.getInitialByteWrapped();
            PacketState intentState;
            switch (handshake.getIntention()) {
                case STATUS:
                    intentState = PacketState.STATUS;
                    break;
                case TRANSFER:
                case LOGIN:
                    intentState = PacketState.LOGIN;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid protocol intent");
            }
            if (packet.getPlayer().pinging && !intentState.equals(PacketState.STATUS)) {
                throw new IllegalStateException("Can only set state to status while pinging");
            }

            handler.setGlobalServerboundProtocolState(intentState);
            handler.setGlobalClientboundProtocolState(intentState);
        }
    }
}
