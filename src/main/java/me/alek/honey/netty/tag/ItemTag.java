package me.alek.honey.netty.tag;

import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;

public interface ItemTag {

    ItemTag EMPTY = new ItemTag() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }
    };

    boolean isEmpty();

    int size();

    default boolean isSimilar(ItemTag tag) {
        return false;
    }

    @SuppressWarnings("unchecked")
    default Representation<CompoundTag> asNBT() {
        return (Representation<CompoundTag>) Representation.EMPTY;
    }

    @SuppressWarnings("unchecked")
    default Representation<ComponentMap> asComponentMap() {
        return (Representation<ComponentMap>) Representation.EMPTY;
    }

    interface Representation<T> {

        Representation<?> EMPTY = new Representation<Object>() {
            @Override
            public boolean isPresent() {
                return false;
            }

            @Override
            public Object get() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object orElse(Object val) {
                return val;
            }
        };

        boolean isPresent();

        T get();

        T orElse(T val);

        static <T> Representation<T> as(T val) {
            return new Representation<T>() {
                @Override
                public boolean isPresent() {
                    return true;
                }

                @Override
                public T get() {
                    return val;
                }

                @Override
                public T orElse(T or) {
                    if (isPresent()) {
                        return val;
                    } else {
                        return or;
                    }
                }
            };
        }
    }
}
