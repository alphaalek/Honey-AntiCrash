package me.alek.honey.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class DecayingWindowCounter {

    private final int[] marks;
    private final Map<Integer, DecayingListeningCounter> delegatedCounters = new HashMap<>();

    public DecayingWindowCounter(int... marks) {
        marks = sortMarks(marks);
        for (int i = 0; i < marks.length; i++) {
            final long collectInterval = marks[i];
            delegatedCounters.put(i, new DecayingListeningCounter(() -> collectInterval));
        }
        this.marks = marks;
    }

    private int[] sortMarks(int... prevMarks) {
        int[] marks = new int[prevMarks.length];
        boolean[] marked = new boolean[prevMarks.length];

        for (int i = 0; i < prevMarks.length; i++) {
            int smallest = Integer.MAX_VALUE;
            int index = -1;

            for (int j = 0; j < prevMarks.length; j++) {
                if (!marked[j] && prevMarks[j] < smallest) {
                    smallest = prevMarks[j];
                    index = j;
                }
            }
            marked[index] = true;
            marks[i] = smallest;
        }
        return marks;
    }

    public void count(long time) {
        for (DecayingListeningCounter counter : delegatedCounters.values()) {
            counter.count(time);
        }
    }

    public void count() {
        count(System.currentTimeMillis());
    }

    public void count(int times) {
        count(times, System.currentTimeMillis());
    }

    public void count(int times, long time) {
        for (int i = 0; i < times; i++) {
            count(time);
        }
    }

    public int size() {
        return delegatedCounters.get(marks.length - 1).size();
    }

    public int size(int mark) {
        if (mark > marks.length) throw new NoSuchElementException();

        return delegatedCounters.get(mark).size();
    }

    public int size(int firstMark, int secondMark) {
        return size(Math.max(firstMark, secondMark)) - size(Math.min(firstMark, secondMark));
    }
}
