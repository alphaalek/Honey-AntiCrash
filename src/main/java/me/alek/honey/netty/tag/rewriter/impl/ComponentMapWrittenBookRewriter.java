package me.alek.honey.netty.tag.rewriter.impl;

import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import me.alek.honey.util.protocol.NetProtocol;

public class ComponentMapWrittenBookRewriter implements InTagRewriter.ComponentMapRewriter {

    @Override
    public Rewritable.Result<ComponentMap> rewrite(NetProtocol protocol, TagRewritingContext<ComponentMap> context) {
        return null;
    }
}
