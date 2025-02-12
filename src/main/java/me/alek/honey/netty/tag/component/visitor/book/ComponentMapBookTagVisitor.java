package me.alek.honey.netty.tag.component.visitor.book;

import com.github.retrooper.packetevents.protocol.component.builtin.item.WritableBookContent;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.util.Filterable;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.tag.ComponentHelper;
import me.alek.honey.netty.tag.component.visitor.CommonComponentMapInVisitor;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.rewriter.impl.NBTWrittenBookRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

import java.util.List;

public class ComponentMapBookTagVisitor extends CommonComponentMapInVisitor {

    public ComponentMapBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        super(protocol, protocolCreated, type);
    }

    protected List<Filterable<String>> visitAtFormattedPages(PacketDataSerializer serializer, int maxPageLength) {
        serializer.markReaderIndex();
        int pageCount = serializer.readVarInt();
        if (pageCount > ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(this.protocolCreated)) {
            throw new DecoderException("Invalid book data: too many pages: " + pageCount);
        }
        serializer.resetReaderIndex();

        PacketWrapper<?> wrapper = createUniversalWrapper(serializer);
        return wrapper.readList(ew -> Filterable.read(wrapper, eew -> wrapper.readString(maxPageLength)));
    }

    @Override
    public WritableBookContent visitAtWritableBookContent(PacketDataSerializer serializer) {
        return new WritableBookContent(visitAtFormattedPages(serializer, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH));
    }

    protected String visitAtAuthor(PacketDataSerializer serializer) {
        return serializer.readString(ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH);
    }

    protected Component visitPage(Component page) {
        TextComponent component = ComponentHelper.readText(page);
        NBTWrittenBookRewriter.checkFormatted(component);
        return component;
    }

    protected List<Filterable<Component>> visitAtSerializedPages(PacketDataSerializer serializer, int maxPageCount) {
        // component map is only present in 1.20.5+, so we will ignore the codec for components prior to the 1.20.3 change
        // therefore, we will read components as nbt
        PacketWrapper<?> wrapper = createUniversalWrapper(serializer);
        return wrapper.readList(ew -> Filterable.read(wrapper, eew -> {
            AbstractTag<?, ?, ?> tag = serializer.readNBT(this.protocol, this.protocolCreated);
            String str = tag.toString();
            if (str.length() > maxPageCount) {
                throw new DecoderException("Invalid book data: too long page: " + str.length() + " > " + maxPageCount);
            }

            Component page = ComponentHelper.deserialize(str);
            return visitPage(page);
        }));
    }

    @Override
    public WrittenBookContent visitAtWrittenBookContent(PacketDataSerializer serializer) {
        PacketWrapper<?> wrapper = createUniversalWrapper(serializer);

        Filterable<String> title = Filterable.read(wrapper, ew -> ew.readString(ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH));
        String author = visitAtAuthor(serializer);
        int generation = wrapper.readVarInt();
        List<Filterable<Component>> pages = visitAtSerializedPages(serializer, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH);
        boolean resolved = wrapper.readBoolean();

        return new WrittenBookContent(title, author, generation, pages, resolved);
    }
}
