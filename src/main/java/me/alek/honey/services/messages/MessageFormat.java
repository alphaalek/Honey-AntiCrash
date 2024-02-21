package me.alek.honey.services.messages;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
                indices.add(MessageIndex.asSubstring(currentStartIntervalIndex, index, message));
                currentStartIntervalIndex = -1;

            } else if (missingIndicesBits[index] == 0 && currentStartIntervalIndex == -1) {
                currentStartIntervalIndex = index;
            }
        }

        if (currentStartIntervalIndex != -1) {
            indices.add(MessageIndex.asSubstring(currentStartIntervalIndex, missingIndicesBits.length - 1, message));
        }

        return new MessageFormat(indices);
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
