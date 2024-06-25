package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.HoneyValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.util.UnsafeChannelCloser;
import me.alek.honey.util.Array;
import me.alek.honey.util.BungeeMessenger;
import me.alek.honey.util.discord.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        return player.violationContext.getDistributor(honeyKey, setting);
    }

    public boolean flag(HoneyPlayer player,
                        Cancellable cancellable,
                        HoneyKey limitsKey,
                        HoneyKey honeyKey,
                        double amount,
                        Runnable onPunish,
                        Runnable onViolation,
                        Runnable onFlag,
                        HoneyValidation<?> check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData
    ) {
        return getDistributor(player, limitsKey, setting)
                .flag(honeyKey, cancellable, check, amount, onPunish, onViolation, onFlag, developmentData);
    }

    public boolean flag(HoneyPlayer player,
                        Cancellable cancellable,
                        HoneyKey limitsKey,
                        HoneyKey honeyKey,
                        double amount,
                        HoneyValidation<?> check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData
    ) {
        return flag(player, cancellable, limitsKey, honeyKey, amount, null, null, null, check, setting, developmentData);
    }

    public synchronized void punish(Runnable onPunish, HoneyPlayer player, HoneyValidation<?> check, HoneyKey honeyKey) {
        if (onPunish != null) onPunish.run();

        if (player.punished) return;

        player.channel.eventLoop().execute(() -> {
            if (player.channel.isActive()) {
                UnsafeChannelCloser.closeForcibly(player.channel);
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
        private final Array<ViolationDistributor> distributors = new Array<>();

        public ViolationContext(HoneyPlayer player) {
            this.player = player;
        }

        public ViolationDistributor getDistributor(HoneyKey key, ConfigurationOptions.VlSetting setting) {
            return distributors.get(key.getRegistryKey(), () -> {
                if (setting.keyedAnnotation != null) {
                    return new ViolationDistributor(player,
                            new KeyedViolationLookupGenerator(key, setting));
                } else {
                    final AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
                    if (packetLimitHandler == null) {
                        throw new RuntimeException("No packet limit handler could be created.");
                    }

                    return new ViolationDistributor(player, packetLimitHandler);
                }
            });
        }

        public void setSetting(HoneyKey key, HoneyKey violationKey, ConfigurationOptions.VlSetting setting) {
            if (distributors.has(key.getRegistryKey())) {
                distributors.get(key.getRegistryKey()).setSetting(violationKey, setting);
            }
        }

        public Array<ViolationDistributor> getViolationDistributors() {
            return distributors;
        }

        public ViolationDistributor getHandler(HoneyKey key) {
            return distributors.get(key.getRegistryKey(), null);
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

    public static class HoneyKey implements Cloneable {

        private final int key;
        private final String identifier;

        private HoneyKey(int key, String identifier) {
            this.key = key;
            this.identifier = identifier;
        }

        public static HoneyKey of(int key) {
            return new HoneyKey(key, null);
        }

        public static HoneyKey of(int key, String identifier) {
            return new HoneyKey(key, identifier);
        }

        public static HoneyKey of(HoneyValidation<?> check) {
            return of(check.getID());
        }

        public int getRegistryKey() {
            return key;
        }

        public String getIdentifier() {
            return identifier;
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
            return "id=" + key + ",identifier=" + identifier;
        }
    }

    public static class KeyedViolationLookupGenerator {

        private final ConfigurationOptions.VlSetting setting;
        private final HoneyKey limitsKey;

        public KeyedViolationLookupGenerator(HoneyKey limitsKey, ConfigurationOptions.VlSetting setting) {
            this.setting = setting;
            this.limitsKey = limitsKey;
        }

        public Map<HoneyKey, AbstractPacketLimitHandler> generateMap(HoneyPlayer player) {
            if (setting.keyedAnnotation == null || setting.keyedAnnotation.amount() <= 0) {
                return new HashMap<HoneyKey, AbstractPacketLimitHandler>(){{
                    put(HoneyKey.of(limitsKey.getRegistryKey()), generateHandler(player));
                }};
            }

            final Map<HoneyKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (int i = 0; i < setting.keyedAnnotation.amount(); i++) {
                map.put(HoneyKey.of(i), generateHandler(player));
            }
            return map;
        }

        private AbstractPacketLimitHandler generateHandler(HoneyPlayer player) {
            return generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
        }
    }

    public static class ViolationDistributor {

        private final HoneyPlayer player;
        private final Array<AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new Array<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(HoneyPlayer player, KeyedViolationLookupGenerator generator) {
            this.player = player;

            for (Map.Entry<HoneyKey, AbstractPacketLimitHandler> entry : generator.generateMap(player).entrySet()) {
                packetLimitHandlerDelegateLookup.set(entry.getKey().getRegistryKey(), entry.getValue());
            }
        }

        public ViolationDistributor(HoneyPlayer player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.set(0, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) return null;

            return Arrays.stream(packetLimitHandlerDelegateLookup.toRealArray())
                    .filter(Objects::nonNull)
                    .map(o -> (AbstractPacketLimitHandler) o)
                    .findAny()
                    .orElse(null);
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(HoneyKey key, boolean orJustAny) {
            AbstractPacketLimitHandler handler = packetLimitHandlerDelegateLookup.get(key.getRegistryKey());

            if (handler != null) {
                return handler;
            }

            if (orJustAny) {
                return getAnyPacketLimitHandler();
            }

            return null;
        }

        public Array<AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        private AbstractPacketLimitHandler getHandler(HoneyKey key) {
            if (packetLimitHandlerDelegateLookup.isEmpty() && cachedDefaultPacketLimitHandler == null) {
                return null;
            }

            if (key == null) {
                return cachedDefaultPacketLimitHandler == null
                        ? (cachedDefaultPacketLimitHandler = getAnyPacketLimitHandler())
                        : cachedDefaultPacketLimitHandler;
            }

            return getPacketLimitHandler(key, false);
        }

        public AbstractPacketLimitHandler createHandler(HoneyKey key, ConfigurationOptions.VlSetting setting) {
            if (packetLimitHandlerDelegateLookup.has(key.getRegistryKey())) {
                return null;
            }

            AbstractPacketLimitHandler handler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
            packetLimitHandlerDelegateLookup.set(key.getRegistryKey(), handler);
            return handler;
        }

        public void setSetting(HoneyKey key, ConfigurationOptions.VlSetting setting) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return;
            }

            handler.setting = setting;
        }

        public boolean flag(HoneyKey key, Cancellable cancellable,
                            HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag,
                            CheckContextContainer.DevelopmentData developmentData) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return false;
            }

            return handler.flag(cancellable, check, amount, onPunish, onViolation, onFlag, developmentData, key);
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

        public void doFormatStuff(HoneyValidation<?> check, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (developmentData != null) {
                HoneyPlugin.get().getNotificationService().applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            if (check != null) {
                HoneyPlugin.get().getNotificationService().notify(
                        player, check.getName(), honeyKey, getSetting(), this, developmentData);

                if (DiscordWebhook.enabled()) {
                    DiscordWebhook.violation(
                            player, check,
                            ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, AbstractPacketLimitHandler.this)),
                            developmentData);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            HoneyPlugin.get().getSQLManager().postViolation(player,
                                    check.getName() + (honeyKey != null && honeyKey.getIdentifier() != null ? " (" + honeyKey.getIdentifier() + ")" : ""),
                                    ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, AbstractPacketLimitHandler.this)), developmentData);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }.runTaskAsynchronously(HoneyPlugin.get().getPlugin());
            }
        }

        public boolean violate(Cancellable cancellable, HoneyValidation<?> check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }

            if (onViolation != null) onViolation.run();
            cancellable.cancel();

            if (violations.incrementAndGet() >= setting.minVls) {
                doFormatStuff(check, developmentData, honeyKey);
            }

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
                 developmentData.addLine(valueEntry.getKey() + ": " +valueEntry.getValue());
            }
        }

        public int getViolations() {
            return violations.get();
        }

        public abstract boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey);

        public abstract PacketLimitHandlerType getType();
    }

    public static class PacketChipstateThrottleHandler extends AbstractPacketLimitHandler {

        public PacketChipstateThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (amount == 1) {
                if (onFlag != null) onFlag.run();
                cancellable.cancel();

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
        public boolean flag(Cancellable cancellable, HoneyValidation check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (amount < 1) {
                notFlaggedLast = true;
                flagged = false;
            }

            if (amount >= 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (onFlag != null) onFlag.run();

                if (amount >= 1) {
                    if (notFlaggedLast && !flagged) {
                        violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
                        flagged = true;
                    }

                    lastFlag = System.currentTimeMillis();
                    notFlaggedLast = true;
                }
                cancellable.cancel();
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
        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (onFlag != null) onFlag.run();

            violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
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
        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (onFlag != null) onFlag.run();

            if ((this.amount = amount) < getSetting().threshold) return false;

            violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
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
        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
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

            return handle(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Cancellable cancellable, HoneyValidation<?> check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey);

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
        public boolean handle(Cancellable cancellable, HoneyValidation<?> check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            cancellable.cancel();

            if (bufferIndex > getSetting().until && getSetting().isEnabled())
                violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
            return true;
        }
    }

    public static class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Cancellable cancellable, HoneyValidation<?> check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
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

        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (System.currentTimeMillis() - intervalStart >= getSetting().interval)
                clearInterval();

            if (onFlag != null) onFlag.run();

            flagged += amount;
            if (flagged < getSetting().maxPerInterval) return false;

            cancellable.cancel();

            if (violatedForInterval) return true;

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().clearInterval) clearInterval();

            violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
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
        public boolean flag(Cancellable cancellable, HoneyValidation<?> check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, HoneyKey honeyKey) {
            if (!currentlyCancelling && onFlag != null) {
                onFlag.run();
            }

            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > getSetting().interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                }
                else cancellable.cancel();
            }

            if ((this.amount = amount) > getSetting().maxPerInterval) {
                intervalStart = System.currentTimeMillis();

                if (amount > getSetting().until && !currentlyCancelling) {
                    violate(cancellable, check, onPunish, onViolation, developmentData, honeyKey);
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
