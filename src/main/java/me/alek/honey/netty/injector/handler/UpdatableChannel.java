package me.alek.honey.netty.injector.handler;

import me.alek.honey.HoneyPlayer;
import org.bukkit.entity.Player;

public interface UpdatableChannel {

    void setPlayer(HoneyPlayer player);

    HoneyPlayer getPlayer();
}
