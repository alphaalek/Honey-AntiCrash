package me.alek.honey.netty.injector;

import org.bukkit.entity.Player;

public interface AbstractPipelineProxy {

    void setPlayer(Player player);

    Player getPlayer();
}
