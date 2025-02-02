package me.alek.honey.util;

import java.util.Arrays;


public class LibraryVersion {

    private int[] versionIntArray;

    public LibraryVersion(int... version) {
        this.versionIntArray = version;
    }

    public LibraryVersion(String version) {
        String[] versionIntegers = version.split("\\.");

        int length = versionIntegers.length;
        this.versionIntArray = new int[length];
        for (int i = 0; i < length; i++) {
            this.versionIntArray[i] = Integer.parseInt(versionIntegers[i].split("-")[0]);
        }

        if (version.contains("-b")) {
            int[] copy = versionIntArray;
            this.versionIntArray = new int[copy.length + 1];
            System.arraycopy(copy, 0, this.versionIntArray, 0, copy.length);

            int val = Integer.parseInt(version.split("-b")[1]);
            this.versionIntArray[copy.length] = val;
        }
    }

    public int compareTo(LibraryVersion version) {
        int localLength = this.versionIntArray.length;
        int oppositeLength = version.versionIntArray.length;
        int length = Math.max(localLength, oppositeLength);

        for (int i = 0; i < length; i++) {
            int localInteger = i < localLength ? this.versionIntArray[i] : 0;
            int oppositeInteger = i < oppositeLength ? version.versionIntArray[i] : 0;
            if (localInteger > oppositeInteger) {
                return 1;
            } else if (localInteger < oppositeInteger) {
                return -1;
            }
        }

        return 0;
    }

    public boolean isNewerThan(LibraryVersion version) {
        return compareTo(version) == 1;
    }

    public boolean isOlderThan(LibraryVersion version) {
        return compareTo(version) == -1;
    }

    public int[] asArray() {
        return this.versionIntArray;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof LibraryVersion) {
            return Arrays.equals(this.versionIntArray, ((LibraryVersion) obj).versionIntArray);
        }
        return false;
    }

    @Override
    public LibraryVersion clone() {
        return new LibraryVersion(this.versionIntArray);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.versionIntArray.length * 2 - 1).append(this.versionIntArray[0]);
        for (int i = 1; i < this.versionIntArray.length; i++) {
            sb.append(".").append(this.versionIntArray[i]);
        }
        return sb.toString();
    }
}