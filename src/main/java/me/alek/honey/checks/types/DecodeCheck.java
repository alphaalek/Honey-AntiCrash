package me.alek.honey.checks.types;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;

public interface DecodeCheck extends HoneyCheck {

    ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception;

}
