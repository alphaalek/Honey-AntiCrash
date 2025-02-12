package me.alek.honey.netty.tag.rewriter.impl;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.util.Filterable;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.tag.ComponentHelper;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import me.alek.honey.util.Array;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Filter;

public class ComponentMapWrittenBookRewriter implements InTagRewriter.ComponentMapRewriter {

    @Override
    public Rewritable.Result<ComponentMap> rewrite(NetProtocol protocol, TagRewritingContext<ComponentMap> context) {
        ComponentMap map = context.tag();
        WrittenBookContent content = map.getVal(ComponentTypes.WRITTEN_BOOK_CONTENT);
        List<Filterable<Component>> pages = new ArrayList<>();

        int normalPages = 0;
        for (Filterable<Component> page : content.getPages()) {
            Rewritable.Result<TextComponent> rawRewritable = NBTWrittenBookRewriter.readRewritablePage(page.getRaw(), context);

            if (page.getFiltered() != null) {
                Rewritable.Result<TextComponent> filteredRewritable = NBTWrittenBookRewriter.readRewritablePage(page.getFiltered(), context);

                TextComponent raw = rawRewritable.orElse(null);
                TextComponent filtered = filteredRewritable.orElse(null);
                if (raw != null || filtered != null) {
                    pages.add(new Filterable<>(Optional.ofNullable(raw).orElse((TextComponent) page.getRaw()), Optional.ofNullable(filtered).orElse((TextComponent) page.getFiltered())));
                } else {
                    normalPages++;
                    pages.add(page);
                }
            } else if (rawRewritable.isRewritten()) {
                pages.add(new Filterable<>(rawRewritable.get(), Optional.empty()));
            } else {
                normalPages++;
                pages.add(page);
            }
        }

        if (normalPages < content.getPages().size()) {
            content.setPages(pages);
            return Rewritable.Result.rewritten(map);
        } else {
            return Rewritable.Result.noRewritten();
        }
    }
}
