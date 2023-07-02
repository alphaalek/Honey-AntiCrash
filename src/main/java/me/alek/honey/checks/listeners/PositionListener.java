package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.structure.converters.SetConverter;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PositionListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static SetConverter<Object, WrappedChunkCoordIntPair> chunks;

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return !Locations.compare3DLocation(packetContainer.getPacket().getLocation(), player.getLocation());
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new PositionA<>());
        visitor.visit(new PositionB<>());
        visitor.visit(new PositionC<>());
        visitor.visit(new PositionD<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        PositionC.lastChunkMap.remove(player.getUniqueId());
        PositionB.lastPacketLocation.remove(player.getUniqueId());
    }

    // forhindrer den chunk i at blive loadet hvis ikke er det i forvejen
    public static synchronized boolean isChunkLoaded(World world, int x, int z) {
        if (chunks == null) {
            Class<Object> worldServerClass = NMSUtils.getWorldServerClass();
            IStructureModifier<Object> craftWorldModifier = ReflectStructureCache.acquireStructure(NMSUtils.getCraftWorldClass())
                    .withType(worldServerClass)
                    .withTarget(world);

            Object worldServer = craftWorldModifier.readField("world", 0);
            Class<Object> chunkProviderServerClass = NMSUtils.getChunkProviderServerClass();
            IStructureModifier<Object> worldServerModifier = ReflectStructureCache.acquireStructure(worldServerClass)
                    .withType(chunkProviderServerClass)
                    .withTarget(worldServer);

            Object chunkProviderServer = worldServerModifier.readField("chunkProviderServer", 0);
            IStructureModifier<Object> chunkProviderServerModifier = ReflectStructureCache.acquireStructure(chunkProviderServerClass)
                    .withWeakType(NMSUtils.getIChunkLoaderClass())
                    .withTarget(chunkProviderServer);

            Object chunkLoader = chunkProviderServerModifier.readField("chunkLoader", 0);
            IStructureModifier<SetConverter<Object, WrappedChunkCoordIntPair>> chunkLoaderModifier = ReflectStructureCache.acquireStructure(NMSUtils.getChunkRegionLoaderClass())
                    .withWeakType(Set.class, Converters.getSetConverterOf(NMSUtils.getChunkCoordIntPairClass(), Converters.getChunkCoordIntConverter(), false))
                    .withTarget(chunkLoader);
            chunks = chunkLoaderModifier.readFieldAndApplyMapping(0);
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        return chunks.containsConverted()
    }

    public static boolean isChunkLoaded(Location location) {
        return isChunkLoaded(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    public static synchronized Chunk getChunkAt(Location location) {
        if (isChunkLoaded(location)) {
            int x = location.getBlockX();
            int z = location.getBlockZ();

            int chunkX = x >> 4;
            int chunkZ = z >> 4;

            long longHash = toLong(chunkX, chunkZ);
            Object chunk = chunkMap.get(longHash);
            Bukkit.broadcastMessage("chunk: " + chunk);
            if (chunk != null) {
                return Converters.getChunkConverter().convertDelegate(chunk);
            }
        }
        return null;
    }

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            final double x = packet.getX();
            final double y = packet.getY();
            final double z = packet.getZ();

            if (x > 3.0E7 || x < -3.0E7 || z > 3.0E7 || z < -3.0E7 || y < ConfigurationOptions.Position.Height.MIN_HEIGHT || y > ConfigurationOptions.Position.Height.MAX_HEIGHT) {
                return ResponseBuilder.inst().cancel().punish().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.A";
        }
    }

    public static class PositionB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Location> lastPacketLocation = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            try {
                final double maxDistance = ConfigurationOptions.Position.Movement.MAX_DISTANCE;
                final double minDistance = ConfigurationOptions.Position.Movement.MIN_DISTANCE;

                final Location playerLoc = player.getLocation();
                final Location packetLoc = packet.getLocation();
                final Location lastKnownPacketLoc = lastPacketLocation.get(player.getUniqueId());

                if (lastKnownPacketLoc != null) {

                    // for at undgå flag ved lagspikes
                    final double distance2d_client = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
                    final double distanceY_client = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);

                    if ((distance2d_client == 0) || distance2d_client < maxDistance && distance2d_client > minDistance) {
                        return ResponseBuilder.SUCCESS;
                    }
                    if ((distanceY_client == 0) || distanceY_client < maxDistance && distanceY_client > minDistance) {
                        return ResponseBuilder.SUCCESS;
                    }
                }
                lastPacketLocation.put(player.getUniqueId(), packetLoc);

                final double distance2d = Locations.calculate2DDistance(packetLoc, playerLoc);
                final double distanceY = Locations.calculateYDistance(packetLoc, playerLoc);

                if (distance2d > maxDistance || distance2d < minDistance && distance2d != 0) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (distanceY > maxDistance || distanceY < minDistance && distanceY != 0) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            } catch (Exception ex) {
                return ResponseBuilder.inst().cancel().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.B";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.Movement.class);
        }
    }

    public static class PositionC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Chunk chunk = getChunkAt(packet.getLocation());
            Bukkit.broadcastMessage(chunk + "");
            if (chunk != null) {
                container.putChunk(chunk);
                if (lastChunkMap.get(player.getUniqueId()) == chunk) {
                    return ResponseBuilder.SUCCESS.builder().skip(1).build(); // skipper unloaded chunk check
                }
                lastChunkMap.put(player.getUniqueId(), chunk);
            }
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public String getViolationName() {
            return "Position.C";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.NewChunks.class);
        }
    }

    public static class PositionD<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Chunk chunk = container.getChunk(0);
            if (chunk != null) {
                return ResponseBuilder.SUCCESS;
            }
            return ResponseBuilder.inst().cancel().flag().build();
        }

        @Override
        public String getViolationName() {
            return "Position.D";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.NewUnloadedChunks.class);
        }
    }

    // PositionE

    // interval, percentage-of-vertical-switch, vls-amount

    // tjekker seneste cached model class "VelocityReference", og sammenligner den direction med
    // den direction som bliver regnet ud ift. til seneste lokation af den VelocityReference
    // hvis det er en anden direction end den tidligere cached, vil det blive flagged som target,
    // ellers vil det blive flagged som normal (forestil dig hvis du hopper, vil alle position packets
    // fra jorden op til det højeste punkt være i et flow af samme direction, men hvis man derimod
    // spammer en masse packets op og ned i et mønster der ikke er normalt i Minecraft, vil det blive
    // flagged som target. Når det interval er ovre, tjekker den om target er højere end det antal af
    // procent som er blevet konfigureret til. jeg tænkte på omkring min. 35-45

    // put det ind i violation service og lav det som en mulighed for andre modules også, hvis jeg tænker
    // at det kunne være en god idé at implementere flere steder i fremtiden. Det samme gælder for
    // buffer violation check, jeg har bare ikke noget konkret sted lige nu at bruge det, men jeg ved det
    // vil blive nyttigt på et tidspunkt

}
