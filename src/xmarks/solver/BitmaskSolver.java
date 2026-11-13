package xmarks.solver;

import xmarks.model.Board;

import java.util.Optional;
import java.util.Random;

/**
 * Backtracking solver for Sudoku X with two changes to the textbook version.
 * 1. Bitmasks track the digits used in each row, column, box and both diagonals,
 *    and an UndoStack reverses placements while backtracking.
 * 2. Each step fills the empty cell with the fewest legal digits (minimum remaining values).
 *
 * Bit (d - 1) of a mask is set when digit d is already used.
 *
 * Besides solving, the same search counts solutions (so the generator can check that a puzzle
 * has exactly one) and can try digits in random order (so the generator can build a fresh grid).
 */
public final class BitmaskSolver {
    private static final int ALL = 0x1FF; // nine low bits
    private static final int FULL = -1;   // pickCell result: no empty cell left
    private static final int DEAD = -2;   // pickCell result: some empty cell has no legal digit

    private final int[] grid = new int[81];
    private final int[] rowMask = new int[9];
    private final int[] colMask = new int[9];
    private final int[] boxMask = new int[9];
    private int mainMask;
    private int antiMask;
    private final UndoStack undo = new UndoStack();
    private Random rnd; // null means ascending digit order

    /** Candidates of the cell returned by the latest pickCell call. */
    private int pickedCandidates;

    /** Number of placements tried in the last run. Handy for comparing against reading order. */
    private long placements;

    /**
     * Solves from the board exactly as it stands. The board itself is never modified.
     * Returns empty when the current entries contradict each other or no solution exists.
     */
    public Optional<int[][]> solve(Board board) {
        return run(board, null);
    }

    /** Same as solve, but tries digits in random order, so an empty board gives a different grid each time. */
    public Optional<int[][]> solveRandomly(Board board, Random random) {
        return run(board, random);
    }

    /** Counts solutions of the board as it stands, stopping once the count reaches limit. */
    public int countSolutions(Board board, int limit) {
        rnd = null;
        if (!load(board)) return 0;
        return count(limit);
    }

    public long placementsTried() {
        return placements;
    }

    private Optional<int[][]> run(Board board, Random random) {
        rnd = random;
        if (!load(board) || !search()) return Optional.empty();
        int[][] out = new int[9][9];
        for (int i = 0; i < 81; i++) out[i / 9][i % 9] = grid[i];
        return Optional.of(out);
    }

    /** Resets all state and places the board's digits. Returns false when they already clash. */
    private boolean load(Board board) {
        java.util.Arrays.fill(grid, 0);
        java.util.Arrays.fill(rowMask, 0);
        java.util.Arrays.fill(colMask, 0);
        java.util.Arrays.fill(boxMask, 0);
        mainMask = 0;
        antiMask = 0;
        placements = 0;
        while (undo.size() > 0) undo.pop();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int d = board.get(r, c);
                if (d == 0) continue;
                if ((usedMask(r, c) & bit(d)) != 0) return false;
                place(r * 9 + c, d);
            }
        }
        return true;
    }

    /** Chooses the empty cell with the fewest candidates. Returns FULL, DEAD, or the cell index. */
    private int pickCell() {
        int best = FULL;
        int bestCount = 10;
        for (int cell = 0; cell < 81; cell++) {
            if (grid[cell] != 0) continue;
            int candidates = ~usedMask(cell / 9, cell % 9) & ALL;
            int n = Integer.bitCount(candidates);
            if (n == 0) return DEAD; // dead end, backtrack right away
            if (n < bestCount) {
                best = cell;
                bestCount = n;
                pickedCandidates = candidates;
                if (n == 1) break; // cannot do better than a forced cell
            }
        }
        return best;
    }

    private boolean search() {
        int cell = pickCell();
        if (cell == FULL) return true;
        if (cell == DEAD) return false;

        int mark = undo.size();
        for (int digit : digitsOf(pickedCandidates)) {
            place(cell, digit);
            undo.push(cell, digit);
            placements++;
            if (search()) return true;
            rollbackTo(mark);
        }
        return false;
    }

    private int count(int limit) {
        int cell = pickCell();
        if (cell == FULL) return 1;
        if (cell == DEAD) return 0;

        int total = 0;
        int mark = undo.size();
        for (int digit : digitsOf(pickedCandidates)) {
            place(cell, digit);
            undo.push(cell, digit);
            total += count(limit - total);
            rollbackTo(mark);
            if (total >= limit) break;
        }
        return total;
    }

    /** The digits of a candidate mask, shuffled when a Random was supplied. */
    private int[] digitsOf(int candidates) {
        int[] digits = new int[Integer.bitCount(candidates)];
        int i = 0;
        for (int remaining = candidates; remaining != 0; remaining &= remaining - 1) {
            digits[i++] = Integer.numberOfTrailingZeros(remaining) + 1;
        }
        if (rnd != null) {
            for (int j = digits.length - 1; j > 0; j--) {
                int k = rnd.nextInt(j + 1);
                int tmp = digits[j];
                digits[j] = digits[k];
                digits[k] = tmp;
            }
        }
        return digits;
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