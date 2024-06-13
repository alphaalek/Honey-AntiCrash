package me.alek.honey.lang;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.services.NotificationService;
import me.alek.honey.session.NoSessionEncryption;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.jar.JarFile;

@NoSessionEncryption
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
                                .orElseThrow(() -> new NoSuchElementException(key))
                );
    }

    public static void load() {
        messages.clear();
        defaultMessages.clear();

        String language = HoneyPlugin.get().getPlugin().getConfig().getString("lang");

        try {
            JarFile jar = new JarFile(new File(Lang.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

            jar.stream().filter(entry -> entry.getName().startsWith("lang/")).forEach(entry -> {
                String[] packages = entry.getName().split("/");

                if (packages[packages.length - 1].split("\\.")[0].equals(language)) {
                    try {
                        loadMessages(entry.getName(), jar.getInputStream(entry), false);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            loadMessages("lang/en.lang", jar.getInputStream(jar.getJarEntry("lang/en.lang")), true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        messages.putAll(defaultMessages);
    }

    private static void loadMessages(String name, InputStream inputStream, boolean isDefault) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("=");
                if (split.length != 2) {
                    continue;
                }
                String key = split[0];
                String value = split[1];

                if (isDefault) {
                    defaultMessages.put(key, value);
                } else {
                    messages.put(key, value);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            HoneyPlugin.get().getPlugin().getLogger().info("Failed to load messages for language file " + name);
        }
    }
}

