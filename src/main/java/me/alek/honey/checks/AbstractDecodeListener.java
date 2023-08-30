package me.alek.honey.checks;

import io.netty.buffer.ByteBuf;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import org.bukkit.entity.Player;

public abstract class AbstractDecodeListener extends FuzzyPacketAdapter {

    private CheckVisitor<?> cachedVisitor;

    @Override
    public void onByteBufDecode(Player player, ByteBuf byteBuf) {
        if (player == null) return;

        if (ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED && player.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION)) {
            return;
        }
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        // cancel the decoding if the player is not even online
        if (!player.isOnline()) {
            if (byteBuf.refCnt() > 0) { byteBuf.clear(); }
            return;
        }
        CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

        defaultDevelopmentData.addEmpty();
        defaultDevelopmentData.addValue(" ByteBuf", byteBuf.readableBytes() + " bytes");
        AbstractPacketListener.applyCommonDevelopmentData(defaultDevelopmentData);

        getCachedVisitor().check(player, byteBuf, checkContextContainer);
    }

    public CheckVisitor<?> getCachedVisitor() {
        if (cachedVisitor == null) {
            cachedVisitor = getVisitor();
        }
        return cachedVisitor;
    }

    public abstract CheckVisitor<?> getVisitor();
}
