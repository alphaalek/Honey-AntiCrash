package me.alek.honey.util.discord;

import com.google.gson.JsonObject;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.types.HoneyValidation;
import me.alek.honey.configuration.ConfigurationOptions;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordWebhook {

    private static final ExecutorService SUBMIT_SERVICE = Executors.newSingleThreadExecutor();

    public static boolean enabled() {
        return !ConfigurationOptions.Webhook.URL.isEmpty();
    }

    public static void send(JsonObject message) {
        SUBMIT_SERVICE.submit(() -> send0(message));
    }

    public static void postViolation(HoneyPlayer player, HoneyValidation<?> check, String context, ValidationPipelineContext.DevelopmentData dev) {
        EmbedObject object = new EmbedObject()
                .setColor(Color.ORANGE)
                .setImage("http://cravatar.eu/head/" + player.name + ".png")
                .setFooter(new Date().toString(), "https://cdn.discordapp.com/icons/1114316101136953535/1d85a2cdd1b3e6908f991d9fa9a98ec1.webp")
                .setTitle(":lock: Honey-AntiCrash log :lock:")
                .addField("", "", false)
                .addField(":octagonal_sign: Player Violation", "``" + player.name + "`` (" + player.uuid + ") failed **" + check.getName() + "**/ " + context, false)
                .addField("", "> " + check.getDescription(), false);

        if (ConfigurationOptions.Webhook.DETAILED) {
            object.addField("Information", "", false);
            for (String devData : dev.getInternalData()) {
                object.addField("", "> " + devData, false);
            }
        }

        JsonObject json = new MessageBuilder()
                .setUsername("Honey")
                .setAvatarUrl("https://cdn.discordapp.com/icons/1114316101136953535/1d85a2cdd1b3e6908f991d9fa9a98ec1.webp")
                .addEmbed(object)
                .toJson();
        send(json);
    }

    private static void send0(JsonObject message) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(ConfigurationOptions.Webhook.URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11");
            connection.setDoOutput(true);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(message.toString().getBytes(StandardCharsets.UTF_8));
            }
            connection.getInputStream().close();
            connection.disconnect();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
