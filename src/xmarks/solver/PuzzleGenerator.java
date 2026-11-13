package xmarks.solver;

import xmarks.model.Board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds a Sudoku X puzzle with exactly one solution.
 *
 * Step 1 fills an empty board using the solver with random digit order, which gives a complete
 * valid grid. Step 2 visits the cells in random order and blanks each one, keeping the blank only
 * while the puzzle still has a single solution. Some cells cannot be blanked without creating a
 * second solution, so very low clue counts may end a few clues above the request.
 */
public final class PuzzleGenerator {
    private PuzzleGenerator() {}

    /**
     * @param clues how many digits to leave in place (0 gives an empty board for free-form play)
     * @return a board whose remaining digits are locked, so the player cannot change them
     */
    public static Board generate(int clues, Random rnd) {
        if (clues <= 0) return new Board();

        BitmaskSolver solver = new BitmaskSolver();
        int[][] full = solver.solveRandomly(new Board(), rnd)
                .orElseThrow(() -> new IllegalStateException("an empty board always has a solution"));
        Board puzzle = new Board();
        puzzle.load(full);

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 81; i++) order.add(i);
        Collections.shuffle(order, rnd);

        int remaining = 81;
        for (int cell : order) {
            if (remaining <= clues) break;
            int r = cell / 9, c = cell % 9;
            int digit = puzzle.get(r, c);
            puzzle.clear(r, c);
            if (solver.countSolutions(puzzle, 2) == 1) {
                remaining--;
            } else {
                puzzle.place(r, c, digit); // blanking this cell would allow a second solution
            }
        }
        puzzle.lockFilledCells();
        return puzzle;
    }
}