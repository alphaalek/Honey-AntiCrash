package me.alek.honey.checks;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractDecodeListener extends FuzzyPacketAdapter {

    private final Map<UUID, CheckVisitor<?>> visitors = new HashMap<>();
    private boolean initializedCheckInstances;

    @Override
    public void onByteBufDecode(HoneyPlayer player, ByteBuf byteBuf) {
        if (player == null) return;

        if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                && player.player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
            return;
        }
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        // cancel the decoding if the player is not even online
        if (!player.isOnline()) {
            byteBuf.clear();
            return;
        }
        CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

        defaultDevelopmentData.addEmpty();
        defaultDevelopmentData.addValue(" ByteBuf", byteBuf.readableBytes() + " bytes");
        AbstractPacketListener.applyCommonDevelopmentData(defaultDevelopmentData);

        getCachedVisitor(player).check(player, byteBuf, checkContextContainer);
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public synchronized CheckVisitor<?> getCachedVisitor(HoneyPlayer player) {
        if (!visitors.containsKey(player.uuid)) visitors.put(player.uuid, getVisitor());
        initializedCheckInstances = true;

        return visitors.get(player.uuid);
    }

    public abstract CheckVisitor<?> getVisitor();
}
