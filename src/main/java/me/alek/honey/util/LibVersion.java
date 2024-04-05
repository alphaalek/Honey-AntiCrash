package me.alek.honey.util;

import java.util.Arrays;


public class LibVersion {

    private final int[] versionIntArray;


    public LibVersion(final int... version) {
        this.versionIntArray = version;
    }

    public LibVersion(final String version) {
        String[] versionIntegers = version.split("\\.");
        int length = versionIntegers.length;
        this.versionIntArray = new int[length];
        for (int i = 0; i < length; i++) {
            versionIntArray[i] = Integer.parseInt(versionIntegers[i]);
        }
    }

    public int compareTo(LibVersion version) {
        int localLength = versionIntArray.length;
        int oppositeLength = version.versionIntArray.length;
        int length = Math.max(localLength, oppositeLength);
        for (int i = 0; i < length; i++) {
            int localInteger = i < localLength ? versionIntArray[i] : 0;
            int oppositeInteger = i < oppositeLength ? version.versionIntArray[i] : 0;
            if (localInteger > oppositeInteger) {
                return 1;
            } else if (localInteger < oppositeInteger) {
                return -1;
            }
        }
        return 0;
    }

    public boolean isNewerThan(LibVersion version) {
        return compareTo(version) == 1;
    }

    public boolean isOlderThan(LibVersion version) {
        return compareTo(version) == -1;
    }

    public int[] asArray() {
        return versionIntArray;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof LibVersion) {
            return Arrays.equals(versionIntArray, ((LibVersion) obj).versionIntArray);
        }
        return false;
    }

    @Override
    public LibVersion clone() {
        return new LibVersion(versionIntArray);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(versionIntArray.length * 2 - 1).append(versionIntArray[0]);
        for (int i = 1; i < versionIntArray.length; i++) {
            sb.append(".").append(versionIntArray[i]);
        }
        return sb.toString();
    }
}