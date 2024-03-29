package me.alek.honey.messages;

import java.util.function.Function;

public class MessageIndex {

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

    public static MessageIndex asSubstring(int startIndex, int endIndex, String message) {
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
