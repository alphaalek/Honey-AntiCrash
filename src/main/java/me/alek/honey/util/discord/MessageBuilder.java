package me.alek.honey.util.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MessageBuilder {

    private final JsonObject object = new JsonObject();
    private final JsonArray embeds = new JsonArray();

    public MessageBuilder setContent(String content) {
        object.addProperty("content", content);
        return this;
    }

    public MessageBuilder setUsername(String username) {
        object.addProperty("username", username);
        return this;
    }

    public MessageBuilder setAvatarUrl(String avatarUrl) {
        object.addProperty("avatar_url", avatarUrl);
        return this;
    }

    public MessageBuilder setTts(boolean tts) {
        object.addProperty("tts", tts);
        return this;
    }

    public MessageBuilder addEmbed(EmbedObject embed) {
        JsonObject embedObject = new JsonObject();
        embed.apply(embedObject);

        embeds.add(embedObject);

        if (!object.has("embeds")) {
            object.add("embeds", embeds);
        }

        return this;
    }

    public JsonObject toJson() {
        return object;
    }
}
