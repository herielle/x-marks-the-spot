package xmarks.solver;

import java.util.Arrays;

/**
 * Records every placement made during the search so backtracking can reverse them.
 * Each entry packs a cell index (0 to 80) and a digit (1 to 9) into one int.
 */
final class UndoStack {
    private int[] data = new int[96];
    private int size = 0;

    void push(int cell, int digit) {
        if (size == data.length) data = Arrays.copyOf(data, size * 2);
        data[size++] = (cell << 4) | digit;
    }

    /** Removes the newest entry and returns it packed as (cell << 4) | digit. */
    int pop() {
        return data[--size];
    }

    int size() {
        return size;
    }

    static int cellOf(int packed) {
        return packed >> 4;
    }

    static int digitOf(int packed) {
        return packed & 15;
    }
}