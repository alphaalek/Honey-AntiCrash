package me.alek.honey.services;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.function.Function;

public class MessageFormat {

    public static MessageFormat load(String message) {
        List<MessageIndex> indices = new ArrayList<>();

        for (Placeholders placeholder : Placeholders.values()) {
            String text = placeholder.getText();

            int index;
            int startIndex = 0;

            while ((index = message.indexOf("%" + text + "%", startIndex)) != -1) {
                indices.add(new MessageIndex(index, index + text.length() + 1, placeholder));
                startIndex = index + text.length();
            }
        }
        int[] missingIndicesBits = new int[message.length()];

        for (MessageIndex index : indices) {
            for (int betweenIndex = index.getStartIndex(); betweenIndex <= index.getEndIndex(); betweenIndex++) {
                missingIndicesBits[betweenIndex] = 1;
            }
        }

        int currentStartIntervalIndex = -1;
        for (int index = 0; index < missingIndicesBits.length; index++) {

            if (missingIndicesBits[index] == 1 && currentStartIntervalIndex != -1) {
                indices.add(MessageIndex.of(currentStartIntervalIndex, index, message));
                currentStartIntervalIndex = -1;

            } else if (missingIndicesBits[index] == 0 && currentStartIntervalIndex == -1) {
                currentStartIntervalIndex = index;
            }
        }

        if (currentStartIntervalIndex != -1) {
            indices.add(MessageIndex.of(currentStartIntervalIndex, missingIndicesBits.length - 1, message));
        }

        return new MessageFormat(indices);
    }

    public enum Placeholders {

        PREFIX("prefix"),
        NETWORK("network"),
        CHECK("check"),
        CONTEXT("context"),
        PLAYER("player");

        private final String placeholder;

        Placeholders(String placeholder) {
            this.placeholder = placeholder;
        }

        public String getText() {
            return placeholder;
        }
    }

    public static class MessageIndex {

        private final int startIndex;
        private final int endIndex;
        private Function<MessageOffer, String> messageFunction;

        private MessageIndex(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public MessageIndex(int startIndex, int endIndex, Placeholders placeholder) {
            this(startIndex, endIndex);
            messageFunction = (offer) -> offer.get(placeholder.getText());
        }

        public MessageIndex(int startIndex, int endIndex, String message) {
            this(startIndex, endIndex);
            messageFunction = (offer) -> message;
        }

        public static MessageIndex of(int startIndex, int endIndex, String message) {
            if (startIndex == endIndex) return new MessageIndex(startIndex, endIndex, " ");

            return new MessageIndex(startIndex, endIndex, message.substring(startIndex, endIndex));
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public String getMessage(MessageOffer offer) {
            return messageFunction.apply(offer);
        }
    }

    private final List<MessageIndex> indices;

    public MessageFormat(List<MessageIndex> indices) {
        this.indices = indices;

        Collections.sort(indices, new Comparator<MessageIndex>() {
            @Override
            public int compare(MessageIndex o1, MessageIndex o2) {
                return Integer.compare(o1.getStartIndex(), o2.getStartIndex());
            }
        });
    }

    public String getMessage(MessageOffer offer) {
        StringBuilder builder = new StringBuilder();

        for (MessageIndex index : indices) {
            builder.append(index.getMessage(offer));
        }
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

}
