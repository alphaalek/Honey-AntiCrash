package me.alek.honey.util.messages;

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

            int i;
            int start = 0;
            while ((i = message.indexOf("%" + text + "%", start)) != -1) {
                indices.add(new MessageIndex(i, i + text.length() + 1, placeholder));
                start = i + text.length();
            }
        }

        int[] mib = new int[message.length()];
        for (MessageIndex messageIndex : indices) {
            for (int betweenIndex = messageIndex.getStartIndex(); betweenIndex <= messageIndex.getEndIndex(); betweenIndex++) {
                mib[betweenIndex] = 1;
            }
        }

        int startIndex = -1;
        for (int i = 0; i < mib.length; i++) {
            if (mib[i] == 1 && startIndex != -1) {
                indices.add(MessageIndex.asSubstring(startIndex, i, message));
                startIndex = -1;
            } else if (mib[i] == 0 && startIndex == -1) {
                startIndex = i;
            }
        }

        if (startIndex != -1) {
            indices.add(MessageIndex.asSubstring(startIndex, mib.length, message));
        }

        return new MessageFormat(indices);
    }

    private final List<MessageIndex> indices;

    public MessageFormat(List<MessageIndex> indices) {
        this.indices = indices;

        indices.sort(Comparator.comparingInt(MessageIndex::getStartIndex));
    }

    public String getMessage(MessageOffer offer) {
        StringBuilder builder = new StringBuilder();

        for (MessageIndex index : indices) {
            builder.append(index.getMessage(offer));
        }
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }
}
