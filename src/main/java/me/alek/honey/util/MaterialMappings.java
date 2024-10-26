package me.alek.honey.util;

import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MaterialMappings {

    STORAGE_MINECART("storage_minecart", MajorProtocolUpdate.BEFORE, "storage-minecart"),
    CHEST_MINECART("chest_minecart", MajorProtocolUpdate.AFTER, "storage-minecart"),

    FIREWORK("firework", MajorProtocolUpdate.BEFORE, "firework-rocket"),
    FIREWORK_ROCKET("firework_rocket", MajorProtocolUpdate.AFTER, "firework-rocket"),
    FIREWORK_CHARGE("firework_charge", MajorProtocolUpdate.BEFORE, "firework-star"),
    FIREWORK_STAR("firework_star", MajorProtocolUpdate.AFTER, "firework-star"),

    LEGACY_FILLED_MAP("map", MajorProtocolUpdate.BEFORE, "filled-map"),
    FILLED_MAP("filled_map", MajorProtocolUpdate.AFTER, "filled-map"),
    LEGACY_EMPTY_MAP("empty_map", MajorProtocolUpdate.BEFORE, "empty-map"),
    EMPTY_MAP("map", MajorProtocolUpdate.AFTER, "empty-map"),

    SKULL("skull", MajorProtocolUpdate.BEFORE, "skull"),
    SKULL_ITEM("skull_item", MajorProtocolUpdate.BEFORE,
            "skull-item", "skeleton-skull", "wither-skeleton-skull", "player-head", "zombie-head", "creeper-head"),
    SKELETON_SKULL("skeleton_skull", MajorProtocolUpdate.AFTER, "skeleton-skull"),
    WITHER_SKELETON_SKULL("wither_skeleton_skull", MajorProtocolUpdate.AFTER, "wither-skeleton-skull"),
    PLAYER_HEAD("player_head", MajorProtocolUpdate.AFTER, "player-head"),
    ZOMBIE_HEAD("zombie_head", MajorProtocolUpdate.AFTER, "zombie-head"),
    CREEPER_HEAD("creeper_head", MajorProtocolUpdate.AFTER, "creeper-head"),

    LEGACY_WRITABLE_BOOK("book_and_quill", MajorProtocolUpdate.BEFORE, "writable-book"),
    WRITABLE_BOOK("writable_book", MajorProtocolUpdate.AFTER, "writable-book"),

    STANDING_BANNER("standing_banner", MajorProtocolUpdate.BEFORE, "standing-banner"),
    BANNER("banner", MajorProtocolUpdate.BEFORE, "banner", "red-banner", "green-banner", "brown-banner",
            "blue-banner", "purple-banner", "cyan-banner", "light-gray-banner", "gray-banner", "pink-banner",
            "lime-banner", "yellow-banner", "light-blue-banner", "magenta-banner", "orange-banner", "white-banner"),
    WALL_BANNER("wall_banner", MajorProtocolUpdate.BEFORE, "wall-banner", "red-wall-banner",
            "green-wall-banner", "brown-wall-banner", "blue-wall-banner", "purple-wall-banner", "cyan-wall-banner",
            "light-gray-wall-banner", "gray-wall-banner", "pink-wall-banner", "lime-wall-banner", "yellow-wall-banner",
            "light-blue-wall-banner", "magenta-wall-banner", "orange-wall-banner", "white-wall-banner"),
    RED_BANNER("red_banner", MajorProtocolUpdate.AFTER, "red-banner"),
    GREEN_BANNER("green_banner", MajorProtocolUpdate.AFTER, "green-banner"),
    BROWN_BANNER("brown_banner", MajorProtocolUpdate.AFTER, "brown-banner"),
    BLUE_BANNER("blue_banner", MajorProtocolUpdate.AFTER, "blue-banner"),
    PURPLE_BANNER("purple_banner", MajorProtocolUpdate.AFTER, "purple-banner"),
    CYAN_BANNER("cyan_banner", MajorProtocolUpdate.AFTER, "cyan-banner"),
    LIGHT_GRAY_BANNER("light_gray_banner", MajorProtocolUpdate.AFTER, "light-gray-banner"),
    GRAY_BANNER("gray_banner", MajorProtocolUpdate.AFTER, "gray-banner"),
    PINK_BANNER("pink_banner", MajorProtocolUpdate.AFTER, "pink-banner"),
    LIME_BANNER("lime_banner", MajorProtocolUpdate.AFTER, "lime-banner"),
    YELLOW_BANNER("yellow_banner", MajorProtocolUpdate.AFTER, "yellow-banner"),
    LIGHT_BLUE_BANNER("light_blue_banner", MajorProtocolUpdate.AFTER, "light-blue-banner"),
    MAGENTA_BANNER("magenta_banner", MajorProtocolUpdate.AFTER, "magenta-banner"),
    ORANGE_BANNER("orange_banner", MajorProtocolUpdate.AFTER, "orange-banner"),
    WHITE_BANNER("white_banner", MajorProtocolUpdate.AFTER, "white-banner"),
    RED_WALL_BANNER("red_wall_banner", MajorProtocolUpdate.AFTER, "red-wall-banner"),
    GREEN_WALL_BANNER("green_wall_banner", MajorProtocolUpdate.AFTER, "green-wall-banner"),
    BROWN_WALL_BANNER("brown_wall_banner", MajorProtocolUpdate.AFTER, "brown-wall-banner"),
    BLUE_WALL_BANNER("blue_wall_banner", MajorProtocolUpdate.AFTER, "blue-wall-banner"),
    PURPLE_WALL_BANNER("purple_wall_banner", MajorProtocolUpdate.AFTER, "purple-wall-banner"),
    CYAN_WALL_BANNER("cyan_wall_banner", MajorProtocolUpdate.AFTER, "cyan-wall-banner"),
    LIGHT_GRAY_WALL_BANNER("light_gray_wall_banner", MajorProtocolUpdate.AFTER, "light-gray-wall-banner"),
    GRAY_WALL_BANNER("gray_wall_banner", MajorProtocolUpdate.AFTER, "gray-wall-banner"),
    PINK_WALL_BANNER("pink_wall_banner", MajorProtocolUpdate.AFTER, "pink-wall-banner"),
    LIME_WALL_BANNER("lime_wall_banner", MajorProtocolUpdate.AFTER, "lime-wall-banner"),
    YELLOW_WALL_BANNER("yellow_wall_banner", MajorProtocolUpdate.AFTER, "yellow-wall-banner"),
    LIGHT_BLUE_WALL_BANNER("light_blue_wall_banner", MajorProtocolUpdate.AFTER, "light-blue-wall-banner"),
    MAGENTA_WALL_BANNER("magenta_wall_banner", MajorProtocolUpdate.AFTER, "magenta-wall-banner"),
    ORANGE_WALL_BANNER("orange_wall_banner", MajorProtocolUpdate.AFTER, "orange-wall-banner"),
    WHITE_WALL_BANNER("white_wall_banner", MajorProtocolUpdate.AFTER, "white-wall-banner");

    // I can add more in the future

    private static class MajorProtocolUpdate {
        private static final NetProtocolRange BEFORE = NetProtocolRange.until(NetProtocol.v1_12_2);
        private static final NetProtocolRange AFTER = NetProtocolRange.since(NetProtocol.v1_13);
    }

    private static final Map<String, List<MaterialMappings>> mappingsOfKey = new HashMap<>();

    static {
        for (MaterialMappings mapping : MaterialMappings.values()) {
            for (String key : mapping.getItemKeys()) {
                mappingsOfKey.computeIfAbsent(key, (d) -> new ArrayList<>())
                        .add(mapping);
            }
        }
    }

    public static Material getTypeIfAny(String key) {
        Material material = getType(key);
        if (material == Material.AIR) {
            throw new IllegalArgumentException("Couldn't find any material matching the item key " + key);
        }

        return material;
    }

    public static Material getType(String key) {
        if (!mappingsOfKey.containsKey(key)) {
            return org.bukkit.Material.AIR;
        }

        for (MaterialMappings mapping : mappingsOfKey.get(key)) {
            if (mapping.getType() != null && mapping.getRange().match(NetProtocol.getProtocol())) {
                return mapping.getType();
            }
        }

        return Material.AIR;
    }

    private final String name;
    private final NetProtocolRange range;
    private final String[] itemKeys;

    MaterialMappings(String name, NetProtocolRange range, String... itemKeys) {
        this.name = name.toUpperCase();
        this.range = range;
        this.itemKeys = itemKeys;
    }

    public String getName() {
        return name;
    }

    public NetProtocolRange getRange() {
        return range;
    }

    public String[] getItemKeys() {
        return itemKeys;
    }

    public Material getType() {
        try {
            return Material.matchMaterial(name);
        } catch (Exception ex) {
            return Material.AIR;
        }
    }
}
