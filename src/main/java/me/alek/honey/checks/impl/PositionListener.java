package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractPositional;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PositionListener<WP extends WrappedAbstractPositional<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, SampleDecayableList<Location>> lastTeleportationLocationMap = new HashMap<>();
    private static final Map<UUID, Location> lastPacketLocation = new HashMap<>();
    private static final DecimalFormat decimalFormatter = new DecimalFormat("#.##");
    private static final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(null);

    public PositionListener() {
        HoneyPlugin.get().getPacketLibrary().addListener(new SyncPacketAdapter<WrappedPlayOutPosition>() {

            @Override
            public void onPacketSend(Player player, PacketContainer<WrappedPlayOutPosition> packetContainer) {
                synchronized (lastTeleportationLocationMap) {
                    final UUID uuid = player.getUniqueId();

                    if (!lastTeleportationLocationMap.containsKey(uuid)) lastTeleportationLocationMap.put(uuid, new SampleDecayableList<>(20000, 10, 1));
                    final SampleDecayableList<Location> sampleDecayableList = lastTeleportationLocationMap.get(uuid);

                    sampleDecayableList.put(packetContainer.getPacket().getLocation());
                }
            }

        }, ListenerPriority.HIGHEST, () -> PacketType.Play.Server.POSITION);
    }

    public static class SampleDecayableList<T> {

        private final long collectInterval;
        private final int sampleSize;
        private final int lockAmount;
        private final LinkedList<SampleNode> sampleNodes = new LinkedList<>();
        private final LinkedList<T> lockedNodes = new LinkedList<>();

        public SampleDecayableList(long collectInterval, int sampleSize, int lockAmount) {
            this.collectInterval = collectInterval;
            this.sampleSize = sampleSize;
            this.lockAmount = lockAmount;
        }

        public class SampleNode {

            public SampleNode(T value) { this.value = value; }

            private final T value;
            private final long insertionInstant = System.currentTimeMillis();

        }

        public void put(T node) {
            synchronized (sampleNodes) {
                sampleNodes.addLast(new SampleNode(node));

                if (sampleNodes.size() > sampleSize) sampleNodes.removeFirst();

                if (lockAmount > 0) {

                    lockedNodes.addLast(node);
                    if (lockedNodes.size() > lockAmount) lockedNodes.removeFirst();
                }
            }
        }

        private void collect() {
            synchronized (sampleNodes) {

                final Iterator<SampleNode> sampleNodeIterator = sampleNodes.iterator();
                while (sampleNodeIterator.hasNext()) {

                    final SampleNode node = sampleNodeIterator.next();
                    final long diff = System.currentTimeMillis() - node.insertionInstant;

                    if (diff > collectInterval) {
                        sampleNodeIterator.remove();
                        continue;
                    }
                    break;
                }
            }
        }

        private T getWithoutCollecting(int index) {
            synchronized (sampleNodes) {
                return sampleNodes.get(index).value;
            }
        }

        public T get(int index) {
            collect();
            synchronized (sampleNodes) {
                return sampleNodes.get(index).value;
            }
        }

        public T getLast() {
            return get(sampleNodes.size());
        }

        public T getFirst() {
            return get(0);
        }

        public List<T> getAll() {
            collect();
            final List<T> list = new ArrayList<>();

            for (int i = 0; i < sampleNodes.size(); i++) {
                list.add(getWithoutCollecting(i));
            }

            return list;
        }

        public List<T> getLocked() {
            return lockedNodes;
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.VEHICLE_MOVE,
                PacketType.Play.Client.GROUND,
                PacketType.Play.Client.FLYING
        );
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        final Location currentLocation = packetContainer.getPacket().getLocation();
        final Location lastLocation = lastPacketLocation.getOrDefault(player.getUniqueId(), currentLocation);

        // check if the packet has any valid location to be checked, or just origin location
        // if it does, ignore this packet and avoid adding this location to the last packet location lookup
        if (Locations.calculate3DDistance(currentLocation, originLocation) < 10) return false;
        lastPacketLocation.put(player.getUniqueId(), currentLocation);

        // check if the player has moved by checking if the last location is equal to the current location
        if (Locations.compare3DLocation(currentLocation, lastLocation)) return false;

        // sharing this last location with the pipeline, so other checks can take use of it
        container.getStorage().put(Location.class, lastLocation);

        final String x = decimalFormatter.format(currentLocation.getX());
        final String y = decimalFormatter.format(currentLocation.getY());
        final String z = decimalFormatter.format(currentLocation.getZ());

        container.getDefaultDevelopmentData().addPoint("Position", x + ", " + y + ", " + z);
        container.getDefaultDevelopmentData().addPoint("Delta Movement", Locations.calculate3DDistance(currentLocation, lastLocation));

        return true;
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
        visitor.visit(new PositionE<>());
        visitor.visit(new PositionF<>());
        visitor.visit(new PositionG<>());
        visitor.visit(new PositionH<>());
        visitor.visit(new PositionI<>());
        visitor.visit(new PositionJ<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        final UUID uuid = player.getUniqueId();

        lastPacketLocation.remove(uuid);
        PositionD.manipulatedFlightSettingsMap.remove(uuid);
        PositionD.lastChunkMap.remove(uuid);
        PositionF.lastDirectionFlowMap.remove(uuid);
        PositionH.guessedDelta2D.remove(uuid);
        PositionH.guessedDeltaY.remove(uuid);

        lastTeleportationLocationMap.remove(uuid);
    }

    public static Location getLastLocationFor(Player player) {
        return lastPacketLocation.getOrDefault(player.getUniqueId(), player.getLocation());
    }

    public static boolean isChunkLoaded(Location location) {
        final int x = location.getBlockX() >> 4;
        final int z = location.getBlockZ() >> 4;

        return location.getWorld().isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordIntPair getChunkAt(Location location) {
        if (isChunkLoaded(location)) {
            final int x = location.getBlockX() >> 4;
            final int z = location.getBlockZ() >> 4;

            return new WrappedChunkCoordIntPair(x, z);
        }
        return null;
    }

    public static boolean isAnyLatelyTeleportationLocationsCloseTo(Player player, Function<Location, Double> distanceFunction, double diff) {
        final UUID uuid = player.getUniqueId();

        synchronized (lastTeleportationLocationMap) {
            if (lastTeleportationLocationMap.containsKey(uuid)) {

                final SampleDecayableList<Location> sampleDecayableList = lastTeleportationLocationMap.get(uuid);

                final List<Location> recentLocations = sampleDecayableList.getAll();
                final List<Location> lockedLocations = sampleDecayableList.getLocked();

                final Stream<Location> allLocations = Stream.concat(recentLocations.stream(), lockedLocations.stream());

                // check if any of those locations is within the distance specified
                final Optional<Location> isCloseTo = allLocations
                    .filter((location) -> distanceFunction.apply(location) < diff).findFirst();

                return isCloseTo.isPresent();
            }
        }
        return false;
    }

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Boolean> lastOnGroundMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (!(packet.getType() == PacketType.Play.Client.FLYING || packet.getType() == PacketType.Play.Client.GROUND))
                return ResponseBuilder.SUCCESS;

            final UUID uuid = player.getUniqueId();
            if (!lastOnGroundMap.containsKey(uuid)) lastOnGroundMap.put(uuid, packet.onGround());

            final boolean onGround = packet.onGround();
            final boolean wasOnGround = lastOnGroundMap.get(uuid);

            lastOnGroundMap.put(uuid, onGround);

            final ResponseContainer response = ResponseBuilder.inst().terminate().build();
            if (getSetting().isEnabled()) {

                if (onGround != wasOnGround) {
                    return response.builder().flag().build();
                }
            }
            return response;
        }

        @Override
        public String getCheckName() {
            return "Position.A";
        }

        @Override
        public String getDescription() {
            return "Switching between ground and air too quickly";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionA.class);
        }
    }

    public static class PositionB<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            final Location packetLoc = packet.getLocation();
            final Location lastKnownPacketLoc = container.getStorage().get(Location.class, 0);

            if (isAnyLatelyTeleportationLocationsCloseTo(
                    player,
                    (location) -> Locations.calculate3DDistance(location, packetLoc),
                    15)
            ) {
                return ResponseBuilder.SUCCESS.builder().skip(3).build();
            }

            final double distance2d = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distance2d);

            final double distanceY = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);
            container.getStorage().putDouble(distanceY);

            if (getSetting().isEnabled()) {

                final double maxDistance = ConfigurationOptions.Position.PositionB.MAX;

                // 2d distance check
                if (distance2d >= maxDistance && distance2d != 0) {
                    container.getDevelopmentData(this).addPoint("2D Distance", distance2d);
                    return ResponseBuilder.inst().cancel().flag().build();
                }

                // y distance check
                if (distanceY >= maxDistance && distanceY != 0) {
                    container.getDevelopmentData(this).addPoint("Y Distance", distanceY);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.B";
        }

        @Override
        public String getDescription() {
            return "Too high delta movement";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionB.class);
        }
    }

    public static class PositionC<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private final Map<UUID, Location> lastFlaggedLocationMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // tjekker om en spiller flyver, for så kan man sagtens fylde bufferen hurtigt op alligevel
                if (player.isFlying())
                    return ResponseBuilder.SUCCESS;

                // ignorerer når man falder ned. bemærk, at hvis den havde været på 0, vil den også ignorere
                // når man går rundt på jorden, da tyngdekraft er en stor faktor ift. en spillers velocity
                if (player.getVelocity().getY() < -1.5)
                    return ResponseBuilder.SUCCESS;

                final Location priorLocation = container.getStorage().get(Location.class, 0);
                // returnerer hvis det er den første lokation packet der bliver sendt
                if (priorLocation == null)
                    return ResponseBuilder.SUCCESS;

                final UUID uuid = player.getUniqueId();
                // ignorerer packets tæt på den seneste flag for at minimere false positives ved lagspikes
                if (lastFlaggedLocationMap.containsKey(uuid)) {

                    if (Locations.calculate3DDistance(lastFlaggedLocationMap.get(uuid), packet.getLocation()) < 1.5)
                        return ResponseBuilder.SUCCESS;
                }
                lastFlaggedLocationMap.put(player.getUniqueId(), packet.getLocation());

                final double priorDistance = Locations.calculate3DDistance(priorLocation, packet.getLocation());
                if (HoneyPlugin.get().getLoggerService().isDebug()) {
                    HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: position distance buffer inc: " + priorDistance);
                }
                // this might be a false positive, shouldn't be that high. but position B would flag for this otherwise
                if (priorDistance > 20)
                    return ResponseBuilder.SUCCESS;

                return ResponseBuilder.inst().flag(priorDistance).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.C";
        }

        @Override
        public String getDescription() {
            return "Buffer of position distance exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionC.class);
        }
    }

    public static class PositionD<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, WrappedChunkCoordIntPair> lastChunkMap = new ConcurrentHashMap<>();
        private static final Set<UUID> manipulatedFlightSettingsMap = new HashSet<>();

        private final ViolationService.CheckKey flightKey = new ViolationService.CheckKey("Position.B", true);
        private final ViolationService.CheckKey nonFlightKey = new ViolationService.CheckKey("Position.B", false);

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {

            final ViolationService.CheckKey key = (player.isFlying()) ? flightKey : nonFlightKey;

            // laver en kopi af settings for at lave en til når man flyver, da man kan besøge langt flere chunks så
            if (!manipulatedFlightSettingsMap.contains(player.getUniqueId())) {
                final int maxPerInterval = getSetting().getMaxPerInterval();
                final ConfigurationOptions.VlSetting setting = getSetting().clone();
                setting.maxPerInterval = maxPerInterval * 3;

                // tvinger en ny packet limit handler til at blive oprettet med den nye flight setting
                HoneyPlugin.get().getViolationService().getLimits(player, flightKey, setting);

                manipulatedFlightSettingsMap.add(player.getUniqueId());
            }
            // får chunk på spillerens lokation som skal sammenlignes ift. sidste chunk hos spilleren, og på den måde vurdere
            // om spilleren bevæger sig for hurtigt rundt på mappet, og måske forsøger at crash serveren på den måde, ved at
            // load alt for mange chunks
            final WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());
            if (chunk != null) {
                // den chunk er loaded, og vi skal derfor sørge for, at alle responses skipper næste check i pipeline

                if (!getSetting().isEnabled() || lastChunkMap.containsKey(player.getUniqueId()) && lastChunkMap.get(player.getUniqueId()).equals(chunk)) {
                    // spilleren er inde i den samme chunk som sidste position update, og derfor skal der ikke flagges
                    return ResponseBuilder.SUCCESS.builder().skip(1).build();
                }
                // spilleren er bevæget sig ind i en ny chunk, og vi skal derfor registrere den chunk som den nuværende samt flag for dette check
                lastChunkMap.put(player.getUniqueId(), chunk);
                return ResponseBuilder.inst().flag().skip(1).withLimitsKey(key).build();
            }
            // den chunk er unloaded og derfor skal Position.E flagges
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.D";
        }

        @Override
        public String getDescription() {
            return "Too many chunks discovered";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionD.class);
        }
    }

    public static class PositionE<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().cancel().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.E";
        }

        @Override
        public String getDescription() {
            return "Too many unloaded chunks discovered";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionE.class);
        }
    }

    public static class PositionF<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<Direction, Axis> axisLookup = new EnumMap<>(Direction.class);

        private enum Direction {
            UP, DOWN, NORTH, SOUTH, WEST, EAST, STILL
        }

        private enum Axis {
            HORIZONTAL_1(Direction.NORTH, Direction.SOUTH),
            HORIZONTAL_2(Direction.WEST, Direction.EAST),
            VERTICAL(Direction.UP, Direction.DOWN);

            private final Direction opp1;
            private final Direction opp2;

            public Direction getOther(Direction dir) {
                return (dir == opp1) ? opp2 : opp1;
            }

            Axis(Direction opp1, Direction opp2) {
                this.opp1 = opp1;
                this.opp2 = opp2;
            }
        }

        static {
            for (Axis axis : Axis.values()) {
                axisLookup.put(axis.opp1, axis);
                axisLookup.put(axis.opp2, axis);
            }
        }

        private Direction[] calculateVelocity(Location loc1, Location loc2) {
            final Direction[] velocity = new Direction[3];

            final double deltaX = loc1.getX() - loc2.getX();
            velocity[0] = (deltaX > 0) ? Direction.EAST : (deltaX == 0) ? Direction.STILL : Direction.WEST;

            final double deltaY = loc1.getY() - loc2.getY();
            velocity[1] = (deltaY > 0) ? Direction.DOWN : (deltaY == 0) ? Direction.STILL : Direction.UP;

            final double deltaZ = loc1.getZ() - loc2.getZ();
            velocity[2] = (deltaZ > 0) ? Direction.SOUTH : (deltaZ == 0) ? Direction.STILL : Direction.NORTH;

            return velocity;
        }

        private static final Map<UUID, Direction[]> lastDirectionFlowMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                final Location priorLocation = container.getStorage().get(Location.class, 0);
                // når den allerførste lokation packet bliver sendt, er der ikke nogen prior location, og derfor skal den
                // bare returnere indtil den næste position packet kommer
                if (priorLocation == null)
                    return ResponseBuilder.SUCCESS;

                final Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                final Direction[] priorVelocity = lastDirectionFlowMap.get(player.getUniqueId());

                lastDirectionFlowMap.put(player.getUniqueId(), velocity);
                if (priorVelocity != null && priorVelocity.length == velocity.length) {

                    int i = -1;
                    for (final Direction dir : velocity) {

                        // sørger for der ikke sker uoverensstemmelse i længderne og indices af de forskellige velocities
                        if (++i == priorVelocity.length) break;

                        final Axis axis = axisLookup.get(dir);
                        final Direction corr = priorVelocity[i];

                        if (corr == Direction.STILL || dir == Direction.STILL) continue;

                        if (corr == axis.getOther(dir)) {
                            HoneyPlugin.get().getViolationService().flag(
                                    player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()),
                                    new ViolationService.CheckKey("Position.F", axis.ordinal()), new ViolationService.CheckKey(axis.name().toLowerCase()),
                                    1, () -> packet.getContainer().cancel(), null, getCheckName(), getSetting(), container.getDevelopmentData(this), false);
                        }
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.F";
        }

        @Override
        public String getDescription() {
            return "Switching directions too quickly";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionF.class);
        }
    }

    public static class PositionG<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // tjekker om den packet er en vehicle move packet
                if (packet.getType() == PacketType.Play.Client.VEHICLE_MOVE) {

                    if (player.getVehicle() == null) {
                        return ResponseBuilder.inst().flag().onViolation(() -> packet.getContainer().cancel()).build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.G";
        }

        @Override
        public String getDescription() {
            return "Moving vehicle without being in one";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionG.class);
        }
    }

    public static class PositionH<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Double> guessedDelta2D = new HashMap<>();
        private static final Map<UUID, Double> guessedDeltaY = new HashMap<>();

        private static final ViolationService.CheckKey key = new ViolationService.CheckKey(PositionH.class, "PositionH");

        @Override
        public synchronized ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            final ResponseContainer response = ResponseBuilder.SUCCESS.builder().skip(1).build();
            if (getSetting().isEnabled()) {

                final double delta2D = container.getStorage().getDouble(0);
                final double deltaY = container.getStorage().getDouble(1);

                if (delta2D == 0 && deltaY == 0) return response;

                final UUID uuid = player.getUniqueId();
                if (!guessedDelta2D.containsKey(uuid)) guessedDelta2D.put(uuid, 0d);
                if (!guessedDeltaY.containsKey(uuid)) guessedDeltaY.put(uuid, 0d);

                boolean hasPattern = false;
                if (delta2D > 0.05) {
                    if (guessedDelta2D.get(uuid) == delta2D) {

                        HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), key, new ViolationService.CheckKey("2d"), 1, "Position.H", getSetting(), container.getDevelopmentData(this), false);
                        hasPattern = true;
                    }
                    guessedDelta2D.put(uuid, delta2D);
                }
                if (deltaY > 0.5) {
                    if (guessedDeltaY.get(uuid) == deltaY) {

                        final Material material = player.getEyeLocation().getBlock().getType();
                        if (material.name().contains("WATER") || material.name().contains("LAVA")) {
                            return response;
                        }
                        if (deltaY % 0.375 < 0.000001) {
                            return response;
                        }
                        HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), key, new ViolationService.CheckKey("y"), 1, "Position.H", getSetting(), container.getDevelopmentData(this), false);
                        hasPattern = true;
                    }
                    guessedDeltaY.put(uuid, deltaY);
                }
                if (hasPattern) return ResponseBuilder.SUCCESS;
            }
            return response;
        }

        @Override
        public String getCheckName() {
            return "Position.H";
        }

        @Override
        public String getDescription() {
            return "Too many identical delta movements";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionH.class);
        }
    }

    public static class PositionI<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                return ResponseBuilder.inst().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.I";
        }

        @Override
        public String getDescription() {
            return "Too many identical delta movements";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionI.class);
        }
    }

    public static class PositionJ<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                final double x = packet.getX();
                final double y = packet.getY();
                final double z = packet.getZ();

                if (x > 3.0E7 || x < -3.0E7) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (z > 3.0E7 || z < -3.0E7) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (y < ConfigurationOptions.Position.PositionJ.MIN || y > ConfigurationOptions.Position.PositionJ.MAX) {

                    double y_ = y;
                    if (y < ConfigurationOptions.Position.PositionJ.MIN) {
                        while (player.getWorld().getBlockAt((int) x, (int) y_++, (int) z).getType() != Material.AIR || y_ < 0) {
                            if (y_ > ConfigurationOptions.Position.PositionJ.MAX - 3) break;
                        }
                    }
                    if (y > ConfigurationOptions.Position.PositionJ.MAX) {
                        while (player.getWorld().getBlockAt((int) x, (int) y_--, (int) z).getType() == Material.AIR) {
                            if (y_ < 16) break;
                        }
                        y_ += 2;
                    }
                    // teleport the player and set the health to max to ensure he is not stuck and will not die on this illegal spot
                    player.teleport(new Location(player.getWorld(), x, y_, z));
                    player.setHealth(20);

                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Position.J";
        }

        @Override
        public String getDescription() {
            return "Too high up in air or below bedrock";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionJ.class);
        }
    }
}
