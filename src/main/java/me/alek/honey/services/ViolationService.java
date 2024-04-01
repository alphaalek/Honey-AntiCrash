package me.alek.honey.services;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.utility.Array;
import me.alek.honey.utility.BungeeMessenger;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ViolationService {

    private static ViolationService INSTANCE;

    public ViolationService() { INSTANCE = this; }

    public void reload() {
        // loop through all the players and update their packet limit handlers
        for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
            player.violationContext = new ViolationContext(player);
        }
    }

    public ViolationDistributor getDistributor(HoneyPlayer player, HoneyKey honeyKey, ConfigurationOptions.VlSetting setting) {
        return player.violationContext.getLimits(honeyKey, setting);
    }

    public boolean flag(HoneyPlayer player,
                        Object handle,
                        CancellablePacketObserver cancellablePacketObserver,
                        HoneyKey limitsKey,
                        HoneyKey honeyKey,
                        double amount,
                        Runnable onPunish,
                        Runnable onViolation,
                        Runnable onFlag,
                        HoneyCheck check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData
    ) {
        return getDistributor(player, limitsKey, setting)
                .flag(honeyKey, handle, cancellablePacketObserver, check, amount, onPunish, onViolation, onFlag, developmentData);
    }

    public boolean flag(HoneyPlayer player,
                        Object handle,
                        CancellablePacketObserver cancellablePacketObserver,
                        HoneyKey limitsKey,
                        HoneyKey honeyKey,
                        double amount,
                        HoneyCheck check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData
    ) {
        return flag(player, handle, cancellablePacketObserver, limitsKey, honeyKey, amount, null, null, null, check, setting, developmentData);
    }

    public synchronized void punish(Runnable onPunish, HoneyPlayer player, HoneyCheck check, HoneyKey honeyKey) {
        if (onPunish != null) onPunish.run();

        if (player.punished) return;

        player.channel.eventLoop().execute(() -> {
            if (player.channel.isActive()) {
                player.channel.close();
            }
        });

        BungeeMessenger.sendPluginMessage(player.player, "kickPlayer", player.name, "Disconnected");
        Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.player.kickPlayer("Disconnected"));

        if (check != null) {
            HoneyPlugin.get().getNotificationService().warn(player, check.getName(), honeyKey);
        }

        player.punished = true;
    }

    public static class ViolationContext {

        private final HoneyPlayer player;
        private final Array<ViolationDistributor> packetLimiters = new Array<>();

        public ViolationContext(HoneyPlayer player) {
            this.player = player;
        }

        public ViolationDistributor getLimits(HoneyKey key, ConfigurationOptions.VlSetting setting) {
            return packetLimiters.get(key.getRegistry().getID(), () -> {
                final AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
                if (packetLimitHandler == null)
                    throw new RuntimeException("No packet limit handler could be created.");

                if (setting.keyedAnnotation != null)
                    return new ViolationDistributor(player,
                            new KeyedViolationLookupGenerator(key, packetLimitHandler, setting));
                else
                    return new ViolationDistributor(player, packetLimitHandler);
            });
        }

        public void setSetting(HoneyKey key, HoneyKey violationKey, ConfigurationOptions.VlSetting setting) {
            if (packetLimiters.has(key.getRegistry().getID())) {
                packetLimiters.get(key.getRegistry().getID()).setSetting(violationKey, setting);
            }
        }

        public Array<ViolationDistributor> getPacketlimitMap() {
            return packetLimiters;
        }

        public ViolationDistributor getHandler(HoneyKey key) {
            return packetLimiters.get(key.getRegistry().getID(), null);
        }

        public HoneyPlayer getPlayer() {
            return player;
        }
    }

    private static AbstractPacketLimitHandler generatePacketLimitHandler(PacketLimitHandlerType type, HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
        switch (type) {
            case SIMPLE:
                return new PacketSimpleHandler(player, setting);
            case INTERVAL:
                return new PacketIntervalHandler(player, setting);
            case INTERVAL_THROTTLE:
                return new PacketIntervalThrottleHandler(player, setting);
            case SINGLE_OVERFLOW:
                return new PacketSingleOverflowHandler(player, setting);
            case BUFFER_OVERFLOW:
                return new PacketBufferOverflowHandler(player, setting);
            case BUFFER_THROTTLE:
                return new PacketBufferThrottleHandler(player, setting);
            case TRIGGER_THROTTLE:
                return new PacketTriggerThrottleHandler(player, setting);
            case CHIPSTATE_THROTTLE:
                return new PacketChipstateThrottleHandler(player, setting);
            default:
                return null;
        }
    }

    public static class HoneyKeyRegistry {

        private static final int MAX_ID = ListenerRegistry.getMaxID();
        private final HoneyCheck check;
        private final int rest;

        public HoneyKeyRegistry(HoneyCheck check, int rest) {
            this.check = check;
            this.rest = rest;
        }

        public int getID() {
            if (rest == 0) return check.getID();
            return MAX_ID + rest;
        }
    }

    public static class HoneyKey extends Tuple2<Object, Object> implements Cloneable {

        private final HoneyKeyRegistry registry;

        private HoneyKey(HoneyKeyRegistry registry, Object object1, Object object2) {
            super(object1, object2);
            this.registry = registry;
        }

        public static HoneyKey of(HoneyCheck check) {
            return of(new HoneyKeyRegistry(check, 0), null, null);
        }

        public static HoneyKey of(HoneyKeyRegistry registry) {
            return of(registry, null, null);
        }

        public static HoneyKey of(HoneyCheck check, Object object1) {
            return of(new HoneyKeyRegistry(check, 0), object1, null);
        }

        public static HoneyKey of(HoneyKeyRegistry registry, Object object1) {
            return of(registry, object1, null);
        }

        public static HoneyKey of(HoneyCheck check, Object object1, Object object2) {
            return of(new HoneyKeyRegistry(check, 0), object1, object2);
        }

        public static HoneyKey of(HoneyKeyRegistry check, Object object1, Object object2) {
            return new HoneyKey(check, object1, object2);
        }

        public static HoneyKey of(HoneyKey key) {
            return of(key.getRegistry(), key.getT1(), key.getT2());
        }

        public int getId() {
            return registry.getID();
        }

        public HoneyKeyRegistry getRegistry() {
            return registry;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public HoneyKey clone() {
            try {
                return (HoneyKey) super.clone();
            } catch (Exception ex) {
                throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return super.toString() + ",id=" + registry.getID();
        }
    }

    public static class KeyedViolationLookupGenerator {

        private final AbstractPacketLimitHandler handler;
        private final ConfigurationOptions.VlSetting setting;
        private final HoneyKey limitsKey;

        public KeyedViolationLookupGenerator(HoneyKey limitsKey, AbstractPacketLimitHandler handler, ConfigurationOptions.VlSetting setting) {
            this.handler = handler;
            this.setting = setting;
            this.limitsKey = limitsKey;
        }

        public Map<HoneyKey, AbstractPacketLimitHandler> generateMap(HoneyPlayer player) {
            if (setting.keyedAnnotation == null || setting.keyedAnnotation.keys().length == 0) {
                return new HashMap<HoneyKey, AbstractPacketLimitHandler>(){{
                    put(HoneyKey.of(limitsKey.getRegistry()), handler);
                }};
            }
            final Map<HoneyKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (final String key : setting.keyedAnnotation.keys()) {
                map.put(HoneyKey.of(limitsKey.getRegistry(), key), generatePacketLimitHandler(handler.getType(), player, setting));
            }
            return map;
        }
    }

    public static class ViolationDistributor {

        private final HoneyPlayer player;
        private final Array<AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new Array<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(HoneyPlayer player, KeyedViolationLookupGenerator generator) {
            this.player = player;

            for (Map.Entry<HoneyKey, AbstractPacketLimitHandler> entry : generator.generateMap(player).entrySet()) {
                packetLimitHandlerDelegateLookup.set(entry.getKey().getRegistry().getID(), entry.getValue());
            }
        }

        public ViolationDistributor(HoneyPlayer player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.set(0, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) return null;

            return packetLimitHandlerDelegateLookup.get(0);
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(HoneyKey key, boolean orJustAny) {
            AbstractPacketLimitHandler handler = packetLimitHandlerDelegateLookup.get(key.getRegistry().getID());

            return (handler != null || !orJustAny) ? handler : getAnyPacketLimitHandler();
        }

        public Array<AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        private AbstractPacketLimitHandler getHandler(HoneyKey key) {
            if (packetLimitHandlerDelegateLookup.isEmpty() && cachedDefaultPacketLimitHandler == null) {
                return null;
            }

            AbstractPacketLimitHandler delegate = null;
            if (key == null) {
                delegate =
                        cachedDefaultPacketLimitHandler == null
                                ? (cachedDefaultPacketLimitHandler = getAnyPacketLimitHandler())
                                : cachedDefaultPacketLimitHandler;
            }
            if (delegate == null && key != null)
                delegate = getPacketLimitHandler(key, false);

            return delegate;
        }

        public void setSetting(HoneyKey key, ConfigurationOptions.VlSetting setting) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return;
            }
            handler.setting = setting;
        }

        public boolean flag(HoneyKey key, Object handle, CancellablePacketObserver cancellablePacketObserver,
                            HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag,
                            CheckContextContainer.DevelopmentData developmentData) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return false;
            }

            return handler.flag(handle, cancellablePacketObserver, check, amount, onPunish, onViolation, onFlag, developmentData, key);
        }
    }

    public static abstract class AbstractPacketLimitHandler {

        private final HoneyPlayer player;
        private ConfigurationOptions.VlSetting setting;

        private final AtomicLong lastViolationClear = new AtomicLong();
        private final AtomicInteger violations = new AtomicInteger();

        public AbstractPacketLimitHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            this.player = player;
            this.setting = setting;

            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public HoneyPlayer getPlayer() {
            return player;
        }

        public ConfigurationOptions.VlSetting getSetting() {
            return setting;
        }

        public void doFormatStuff(HoneyCheck check, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (developmentData != null) {
                AbstractPacketListener.applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            if (check != null) {
                if (ConfigurationOptions.PacketDump.VLS_ENABLED) {

                    PacketBundleFilter filter = check.getDumpFilter();
                    if (filter != null && filter.getAcceptedPacketTypes() != null) {

                        List<PacketContainer<?>> dumpedPackets = player.packetBundleSaver
                                .get(Sets.newHashSet(filter.getAcceptedPacketTypes()), filter.size(), true);

                        if (dumpedPackets != null) {
                            HoneyPlugin.get().getLoggerService().dumpPackets(player, dumpedPackets);
                        }
                    }
                }

                HoneyPlugin.get().getNotificationService().notify(
                        player, check.getName(), honeyKey, getSetting(), this, developmentData);

                new BukkitRunnable() {

                    @Override
                    public void run() {
                        try {
                            HoneyPlugin.get().getSQLManager().postViolation(
                                    player, check.getName() + (honeyKey == null ? "" : " (" + honeyKey.getT1() + ")"),
                                    ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, AbstractPacketLimitHandler.this)), developmentData);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }.runTaskAsynchronously(HoneyPlugin.get().getPlugin());
            }
        }

        public boolean violate(CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }

            if (onViolation != null) onViolation.run();
            cancellablePacketObserver.cancel();

            violations.incrementAndGet();

            doFormatStuff(check, developmentData, honeyKey);

            if (violations.get() < getSetting().vlsAmount) {
                return false;
            }

            INSTANCE.punish(onPunish, player, check, honeyKey);
            return true;
        }

        private void applySettingsToDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
            developmentData.addEmpty();

            PacketLimitHandlerType handlerType = setting.getPacketLimitHandlerType();
            developmentData.addHeader("Settings", (handlerType == PacketLimitHandlerType.SIMPLE ? null : setting.getPacketLimitHandlerType().name().toLowerCase().replace("_", " ")));

            for (Map.Entry<String, Object> valueEntry : setting.getValues()) {

                 // add the line to the development data
                 developmentData.addLine(valueEntry.getKey() + ": " +valueEntry.getValue());
            }
        }

        public int getViolations() {
            return violations.get();
        }

        public abstract boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey);

        public abstract PacketLimitHandlerType getType();
    }

    public static class PacketChipstateThrottleHandler extends AbstractPacketLimitHandler {

        public PacketChipstateThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (amount == 1) {
                if (onFlag != null) onFlag.run();
                cancellablePacketObserver.cancel();

                return true;
            }
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.CHIPSTATE_THROTTLE;
        }
    }

    public static class PacketTriggerThrottleHandler extends AbstractPacketLimitHandler {

        private boolean notFlaggedLast;
        private boolean flagged;
        private double lastFlag;

        public PacketTriggerThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (amount < 1) {
                notFlaggedLast = true;
                flagged = false;
            }

            if (amount >= 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (onFlag != null) onFlag.run();

                if (amount >= 1) {
                    if (notFlaggedLast && !flagged) {
                        violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
                        flagged = true;
                    }

                    lastFlag = System.currentTimeMillis();
                    notFlaggedLast = true;
                }
                cancellablePacketObserver.cancel();
                return true;
            }
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.TRIGGER_THROTTLE;
        }
    }

    public static class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (onFlag != null) onFlag.run();

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public static class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private double amount;

        public PacketSingleOverflowHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (onFlag != null) onFlag.run();

            if ((this.amount = amount) < getSetting().threshold) return false;

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SINGLE_OVERFLOW;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double bufferIndex;


        public PacketBufferHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            bufferIndex -= getSetting().decayAmount;
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            hasDecayed = false;
            if (System.currentTimeMillis() - lastDecay > getSetting().decay) {
                hasDecayed = true;
                lastDecay = System.currentTimeMillis();
                decay();
            }
            if (bufferIndex < 0) bufferIndex = 0;
            bufferIndex += amount;

            if (onFlag != null) onFlag.run();

            if (bufferIndex < getSetting().threshold) return false;

            bufferIndexCopy = bufferIndex;
            if (getSetting().clearBuffer) this.bufferIndex = 0;

            return handle(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey);

    }

    public static class PacketBufferThrottleHandler extends PacketBufferHandler {


        public PacketBufferThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_THROTTLE;
        }

        @Override
        public void decay() {
            if (bufferIndex < getSetting().threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((bufferIndex - getSetting().threshold) / (getSetting().decayAmount * 1.8));
            double amount = getSetting().decayAmount * Math.pow(1.4, ratio);

            bufferIndex -= amount;
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            cancellablePacketObserver.cancel();

            if (bufferIndex > getSetting().until && getSetting().isEnabled())
                violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }
    }

    public static class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public static class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private boolean violatedForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        public double flaggedCopy;

        public PacketIntervalHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (System.currentTimeMillis() - intervalStart >= getSetting().interval)
                clearInterval();

            if (onFlag != null) onFlag.run();

            flagged += amount;
            if (flagged < getSetting().maxPerInterval) return false;

            cancellablePacketObserver.cancel();

            if (violatedForInterval) return true;

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().clearInterval) clearInterval();

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public static class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (!currentlyCancelling && onFlag != null) {
                onFlag.run();
            }

            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > getSetting().interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                }
                else cancellablePacketObserver.cancel();
            }

            if ((this.amount = amount) > getSetting().maxPerInterval) {
                intervalStart = System.currentTimeMillis();

                if (amount > getSetting().until && !currentlyCancelling) {
                    violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, honeyKey);
                }
                currentlyCancelling = true;
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL_THROTTLE;
        }

        public double getAmount() {
            return amount;
        }
    }
}
