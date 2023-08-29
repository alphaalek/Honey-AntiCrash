package me.alek.honey.checks.types;

import io.netty.buffer.ByteBuf;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;
import org.bukkit.entity.Player;

public interface DecodeCheck extends HoneyCheck {

    ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception;
}
