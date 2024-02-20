package me.alek.honey.utility.pastebin;

import java.util.HashMap;
import java.util.Map;

public class PasteRequest {
    private final String content;
    private final Format format;
    private final Visibility visibility;
    private final String name;
    private final Expiration expiration;
    private final String folderKey;

    private PasteRequest(String content, Format format, Visibility visibility, String name, Expiration expiration, String folderKey) {
        this.content = content;
        this.format = format;
        this.visibility = visibility;
        this.name = name;
        this.expiration = expiration;
        this.folderKey = folderKey;
    }

    public static Builder content(String content) {
        return new Builder(content);
    }

    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap();
        parameters.put("api_option", "paste");
        parameters.put("api_paste_code", this.content);
        if (this.format != null) {
            parameters.put("api_paste_format", this.format.getCode());
        }

        if (this.visibility != null) {
            parameters.put("api_paste_private", String.valueOf(this.visibility.getCode()));
        }

        if (this.name != null) {
            parameters.put("api_paste_name", this.name);
        }

        if (this.expiration != null) {
            parameters.put("api_paste_expire_date", this.expiration.getCode());
        }

        if (this.folderKey != null) {
            parameters.put("api_folder_key", this.folderKey);
        }

        return parameters;
    }

    public static class Builder {
        private final String content;
        private Format format;
        private Visibility visibility;
        private String name;
        private Expiration expiration;
        private String folderKey;

        Builder(String content) {
            this.content = content;
        }

        public Builder format(Format format) {
            this.format = format;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder expiration(Expiration expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder folderKey(String folderKey) {
            this.folderKey = folderKey;
            return this;
        }

        public PasteRequest build() {
            return new PasteRequest(this.content, this.format, this.visibility, this.name, this.expiration, this.folderKey);
        }
    }
}