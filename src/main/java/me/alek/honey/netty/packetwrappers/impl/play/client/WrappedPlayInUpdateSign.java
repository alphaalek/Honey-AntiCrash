package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.packetwrappers.PacketRewriter;
import me.alek.honey.netty.tag.ComponentHelper;
import me.alek.honey.netty.tag.rewriter.impl.NBTWrittenBookRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

public class WrappedPlayInUpdateSign extends ByteInWrappedPacketImpl<WrappedPlayInUpdateSign> implements PacketRewriter {

    public static final int LINES = 4;
    public static final int MAX_LINE_LENGTH = 450;

    @Property
    private final WrappedBlockPosition position;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_20))
    private final boolean isFrontText;
    @Property
    private final String[] text;

    public WrappedPlayInUpdateSign(PacketDataSerializer buf, PacketContainer<WrappedPlayInUpdateSign> packet) {
        super(buf, packet);
        NetProtocol protocol = packet.getProtocol();

        this.position = serializer().readBlockPosition();
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20)) {
            this.isFrontText = buf.readBoolean();
        } else {
            this.isFrontText = true;
        }

        this.text = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            this.text[i] = buf.readString(MAX_LINE_LENGTH);

            if (getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                ComponentHelper.readText(this.text[i]);
            }
        }
    }

    @Override
    public void rewrite(PacketDataSerializer serializer) {
        NetProtocol protocol = getProtocol();

        serializer.writeBlockPosition(this.position);
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20)) {
            serializer.writeBoolean(this.isFrontText);
        }

        for (int i = 0; i < LINES; i++) {
            if (getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
                // force to text component
                TextComponent component = (TextComponent) ComponentHelper.deserialize(this.text[i]);
                serializer.writeString(ComponentHelper.ensureOnlyDeserializableAsRawText(component.content()));
            } else {
                serializer.writeString(this.text[i]);
            }
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
