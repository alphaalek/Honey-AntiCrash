package me.alek.honey.util.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmbedObject {
    private String title;
    private String description;
    private String url;
    private Color color;

    private Footer footer;
    private String thumbnail;
    private String image;
    private Author author;
    private final List<Field> fields = new ArrayList<>();

    public void apply(JsonObject jsonEmbed) {
        jsonEmbed.addProperty("title", title);
        jsonEmbed.addProperty("description", description);
        jsonEmbed.addProperty("url", url);

        if (color != null) {
            int rgb = color.getRed();
            rgb = (rgb << 8) + color.getGreen();
            rgb = (rgb << 8) + color.getBlue();

            jsonEmbed.addProperty("color", rgb);
        }

        if (footer != null) {
            JsonObject jsonFooter = new JsonObject();

            jsonFooter.addProperty("text", footer.getText());
            jsonFooter.addProperty("icon_url", footer.getIconUrl());
            jsonEmbed.add("footer", jsonFooter);
        }

        if (image != null) {
            JsonObject jsonImage = new JsonObject();

            jsonImage.addProperty("url", image);
            jsonEmbed.add("image", jsonImage);
        }

        if (thumbnail != null) {
            JsonObject jsonThumbnail = new JsonObject();

            jsonThumbnail.addProperty("url", thumbnail);
            jsonEmbed.add("thumbnail", jsonThumbnail);
        }

        if (author != null) {
            JsonObject jsonAuthor = new JsonObject();

            jsonAuthor.addProperty("name", author.getName());
            jsonAuthor.addProperty("url", author.getUrl());
            jsonAuthor.addProperty("icon_url", author.getIconUrl());
            jsonEmbed.add("author", jsonAuthor);
        }

        JsonArray jsonFields = new JsonArray();
        for (EmbedObject.Field field : fields) {
            JsonObject jsonField = new JsonObject();

            jsonField.addProperty("name", field.getName());
            jsonField.addProperty("value", field.getValue());
            jsonField.addProperty("inline", field.isInline());

            jsonFields.add(jsonField);
        }

        jsonEmbed.add("fields", jsonFields);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Color getColor() {
        return color;
    }

    public Footer getFooter() {
        return footer;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getImage() {
        return image;
    }

    public Author getAuthor() {
        return author;
    }

    public List<Field> getFields() {
        return fields;
    }

    public EmbedObject setTitle(String title) {
        this.title = title;
        return this;
    }

    public EmbedObject setDescription(String description) {
        this.description = description;
        return this;
    }

    public EmbedObject setUrl(String url) {
        this.url = url;
        return this;
    }

    public EmbedObject setColor(Color color) {
        this.color = color;
        return this;
    }

    public EmbedObject setFooter(String text, String icon) {
        this.footer = new Footer(text, icon);
        return this;
    }

    public EmbedObject setThumbnail(String url) {
        this.thumbnail = url;
        return this;
    }

    public EmbedObject setImage(String url) {
        this.image = url;
        return this;
    }

    public EmbedObject setAuthor(String name, String url, String icon) {
        this.author = new Author(name, url, icon);
        return this;
    }

    public EmbedObject addField(String name, String value, boolean inline) {
        this.fields.add(new Field(name, value, inline));
        return this;
    }

    public static class Footer {
        private final String text;
        private final String iconUrl;

        private Footer(String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
        }

        private String getText() {
            return text;
        }

        private String getIconUrl() {
            return iconUrl;
        }
    }

    public static class Author {
        private final String name;
        private final String url;
        private final String iconUrl;

        private Author(String name, String url, String iconUrl) {
            this.name = name;
            this.url = url;
            this.iconUrl = iconUrl;
        }

        private String getName() {
            return name;
        }

        private String getUrl() {
            return url;
        }

        private String getIconUrl() {
            return iconUrl;
        }
    }

    public static class Field {
        private final String name;
        private final String value;
        private final boolean inline;

        private Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        private String getName() {
            return name;
        }

        private String getValue() {
            return value;
        }

        private boolean isInline() {
            return inline;
        }
    }
}
