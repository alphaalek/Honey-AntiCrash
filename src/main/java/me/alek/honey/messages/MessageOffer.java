package me.alek.honey.messages;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.services.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class MessageOffer {

    private static final NotificationService notificationService = HoneyPlugin.get().getNotificationService();
    private static final Map<String, Supplier<String>> globalOffers = new HashMap<>();

    static {
        globalOffers.put(Placeholders.PREFIX.getText(), notificationService::getPrefix);
    }

    private final Map<String, Supplier<String>> offers = new HashMap<>();

    public void offer(String key, Supplier<String> value) {
        offers.put(key, value);
    }

    public String get(String key) {
        return Optional.ofNullable(offers.get(key)).map(Supplier::get).orElse(getGlobal(key));
    }

    public String getGlobal(String key) {
        return Optional.ofNullable(globalOffers.get(key)).map(Supplier::get).orElse("???");
    }

    public Builder builder() {
        Builder builder = new Builder();
        for (Map.Entry<String, Supplier<String>> offerEntry : offers.entrySet()) {
            builder.offer(offerEntry.getKey(), offerEntry.getValue());
        }
        return builder;
    }

    public static class Builder {

        private final MessageOffer offer = new MessageOffer();

        public Builder offer(String key, Supplier<String> value) {
            offer.offer(key, value);
            return this;
        }

        public Builder offer(Placeholders placeholder, Supplier<String> value) {
            return offer(placeholder.getText(), value);
        }

        public MessageOffer build() {
            return offer;
        }
    }
}
