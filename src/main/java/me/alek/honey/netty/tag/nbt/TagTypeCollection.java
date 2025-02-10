package me.alek.honey.netty.tag.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumMap;

public enum TagTypeCollection {

    COMPOUND,

    ARRAY,

    ARRAY_OF_COMPOUND,

    ARRAY_OF_ARRAY
    ;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface OfCollection {

        TagTypeCollection collection();
    }

    public static TagType getFromCollection(TagTypeCollection collection) {
        return TYPE_COLLECTION_LOOKUP.get(collection);
    }

    private static final EnumMap<TagTypeCollection, TagType> TYPE_COLLECTION_LOOKUP = new EnumMap<>(TagTypeCollection.class);

    public static void load() {
        for (TagType type : TagType.values()) {
            if (!type.isValid()) {
                continue;
            }

            if (type.hasCollection()) {
                TYPE_COLLECTION_LOOKUP.put(type.getCollection(), type);
            }
        }
    }
}
