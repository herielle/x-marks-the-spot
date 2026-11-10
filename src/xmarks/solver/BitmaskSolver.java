package xmarks.solver;

import xmarks.model.Board;

import java.util.Optional;

/**
 * Backtracking solver for Sudoku X with two changes to the textbook version.
 * 1. Bitmasks track the digits used in each row, column, box and both diagonals,
 *    and an UndoStack reverses placements while backtracking.
 * 2. Each step fills the empty cell with the fewest legal digits (minimum remaining values).
 *
 * Bit (d - 1) of a mask is set when digit d is already used.
 */
public final class BitmaskSolver {
    private static final int ALL = 0x1FF; // nine low bits

    private final int[] grid = new int[81];
    private final int[] rowMask = new int[9];
    private final int[] colMask = new int[9];
    private final int[] boxMask = new int[9];
    private int mainMask;
    private int antiMask;
    private final UndoStack undo = new UndoStack();

    /** Number of placements tried in the last run. Handy for comparing against reading order. */
    private long placements;

    /**
     * Solves from the board exactly as it stands. The board itself is never modified.
     * Returns empty when the current entries contradict each other or no solution exists.
     */
    public Optional<int[][]> solve(Board board) {
        reset();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int d = board.get(r, c);
                if (d == 0) continue;
                if ((usedMask(r, c) & bit(d)) != 0) return Optional.empty();
                place(r * 9 + c, d);
            }
        }
        if (!search()) return Optional.empty();
        int[][] out = new int[9][9];
        for (int i = 0; i < 81; i++) out[i / 9][i % 9] = grid[i];
        return Optional.of(out);
    }

    public long placementsTried() {
        return placements;
    }

    private void reset() {
        java.util.Arrays.fill(grid, 0);
        java.util.Arrays.fill(rowMask, 0);
        java.util.Arrays.fill(colMask, 0);
        java.util.Arrays.fill(boxMask, 0);
        mainMask = 0;
        antiMask = 0;
        placements = 0;
        while (undo.size() > 0) undo.pop();
    }

    private boolean search() {
        int best = -1;
        int bestCandidates = 0;
        int bestCount = 10;

        for (int cell = 0; cell < 81; cell++) {
            if (grid[cell] != 0) continue;
            int candidates = ~usedMask(cell / 9, cell % 9) & ALL;
            int count = Integer.bitCount(candidates);
            if (count == 0) return false; // dead end, backtrack right away
            if (count < bestCount) {
                best = cell;
                bestCandidates = candidates;
                bestCount = count;
                if (count == 1) break; // cannot do better than a forced cell
            }
        }
        if (best == -1) return true; // no empty cell left

        int mark = undo.size();
        int remaining = bestCandidates;
        while (remaining != 0) {
            int low = remaining & -remaining;
            int digit = Integer.numberOfTrailingZeros(low) + 1;
            remaining ^= low;

            place(best, digit);
            undo.push(best, digit);
            placements++;
            if (search()) return true;
            rollbackTo(mark);
        }
        return false;
    }

    private void rollbackTo(int mark) {
        while (undo.size() > mark) {
            int packed = undo.pop();
            unplace(UndoStack.cellOf(packed), UndoStack.digitOf(packed));
        }
    }

    private int usedMask(int r, int c) {
        int used = rowMask[r] | colMask[c] | boxMask[r / 3 * 3 + c / 3];
        if (Board.onMainDiagonal(r, c)) used |= mainMask;
        if (Board.onAntiDiagonal(r, c)) used |= antiMask;
        return used;
    }

    private void place(int cell, int d) {
        int r = cell / 9, c = cell % 9, b = bit(d);
        grid[cell] = d;
        rowMask[r] |= b;
        colMask[c] |= b;
        boxMask[r / 3 * 3 + c / 3] |= b;
        if (Board.onMainDiagonal(r, c)) mainMask |= b;
        if (Board.onAntiDiagonal(r, c)) antiMask |= b;
    }

    private void unplace(int cell, int d) {
        int r = cell / 9, c = cell % 9, b = bit(d);
        grid[cell] = 0;
        rowMask[r] &= ~b;
        colMask[c] &= ~b;
        boxMask[r / 3 * 3 + c / 3] &= ~b;
        if (Board.onMainDiagonal(r, c)) mainMask &= ~b;
        if (Board.onAntiDiagonal(r, c)) antiMask &= ~b;
    }

    private static int bit(int digit) {
        return 1 << (digit - 1);
    }
}