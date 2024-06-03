package me.alek.honey.lang;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandManager;
import me.alek.honey.services.NotificationService;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Lang {

    private static final Map<String, String> messages = new HashMap<>();
    private static final Map<String, String> defaultMessages = new HashMap<>();

    public static String getFormatted(String key, Object... values) {
        return String.format(
                    get(key)
                    .replaceAll("\\{prefix}", NotificationService.PREFIX)
                , values);

    }

    public static String get(String key) {
        return Optional.ofNullable(messages.get(key))
                .orElse(
                        Optional.ofNullable(defaultMessages.get(key))
                                .orElseThrow(NoSuchElementException::new)
                );
    }

    public static void load() {
        messages.clear();
        defaultMessages.clear();

        String language = HoneyPlugin.get().getPlugin().getConfig().getString("lang");

        File folder = new File(Lang.class.getClassLoader().getResource("lang/").getFile());
        if (!folder.exists() || folder.listFiles() == null) {
            return;
        }

        loadMessages(getDefaultMessagesFile(folder), true);

        for (File file : folder.listFiles()) {
           if (file.getName().split("\\.")[0].equals(language)) {
               loadMessages(file, false);
               return;
           }
        }

        for (Map.Entry<String, String> messageEntry : defaultMessages.entrySet()) {
            messages.put(messageEntry.getKey(), messageEntry.getValue());
        }
    }

    private static File getDefaultMessagesFile(File folder) {
        return new File(folder.getAbsolutePath() + File.separator + "en.lang");
    }

    private static void loadMessages(File file, boolean isDefault) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)));

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.split("=")[0];
                String value = line.split("=")[1];

                if (isDefault) {
                    defaultMessages.put(key, value);
                } else {
                    messages.put(key, value);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            HoneyPlugin.get().getPlugin().getLogger().info("Failed to load messages for language file " + file.getName());
        }
    }
}

