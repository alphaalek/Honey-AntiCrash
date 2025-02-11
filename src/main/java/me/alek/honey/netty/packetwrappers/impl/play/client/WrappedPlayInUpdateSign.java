package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.packetwrappers.PacketRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

public class WrappedPlayInUpdateSign extends ByteInWrappedPacketImpl<WrappedPlayInUpdateSign> implements PacketRewriter {

    public static final int LINES = 4;
    public static final int MAX_LINE_LENGTH = 45;

    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

    @Property
    private final WrappedBlockPosition position;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_20))
    private final boolean isFrontText;
    @Property
    private final String[] text;

    public WrappedPlayInUpdateSign(PacketDataSerializer buf, PacketContainer<WrappedPlayInUpdateSign> packet) {
        super(buf, packet);
        NetProtocol protocol = packet.getProtocol();

        this.position = WrappedBlockPosition.fromLong(buf.readLong());
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20)) {
            this.isFrontText = buf.readBoolean();
        } else {
            this.isFrontText = true;
        }

        this.text = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            this.text[i] = buf.readString(MAX_LINE_LENGTH);
        }
    }

    @Override
    public void rewrite(PacketDataSerializer serializer) {
        NetProtocol protocol = getProtocol();

        serializer.writeLong(this.position.asLong());
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20)) {
            serializer.writeBoolean(this.isFrontText);
        }

        for (int i = 0; i < LINES; i++) {
            if (protocol.isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                // force to text component
                Component component = GSON_COMPONENT_SERIALIZER.deserialize(this.text[i]);
                if (!(component instanceof TextComponent)) {
                    this.text[i] = "{text:\"" + this.text[i] + "\"}";
                } else {
                    TextComponent text = (TextComponent) component;
                    // make sure there are no other components than text content
                    if (!text.children().isEmpty() || !text.style().isEmpty()) {
                        this.text[i] = GSON_COMPONENT_SERIALIZER.serialize(Component.text(text.content()));
                    }
                }
            }
            serializer.writeString(this.text[i]);
        }
    }

    public WrappedBlockPosition getPosition() {
        return this.position;
    }

    public boolean isFrontText() {
        return this.isFrontText;
    }

    public String[] getText() {
        return this.text;
    }

    public String getLine(int line) {
        return this.text[line];
    }
}
