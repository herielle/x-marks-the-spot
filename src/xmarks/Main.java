package xmarks;

import xmarks.model.Board;
import xmarks.model.MoveResult;
import xmarks.solver.BitmaskSolver;

import java.util.Optional;

/** Throwaway console check. Replace with the real launcher once the UI exists. */
public class Main {
    public static void main(String[] args) {
        // 1. Player moves are checked against all four rules.
        Board board = new Board();
        System.out.println("Place 5 at (0,0): " + board.place(0, 0, 5).accepted());
        MoveResult r = board.place(0, 8, 5);
        System.out.println("Place 5 at (0,8): " + r.message());
        r = board.place(8, 0, 5);
        System.out.println("Place 5 at (8,0): " + r.message());
        r = board.place(4, 4, 5);
        System.out.println("Place 5 at (4,4): " + r.message());
        r = board.place(1, 1, 3);
        System.out.println("Place 3 at (1,1): " + r.accepted());
        r = board.place(8, 8, 3);
        System.out.println("Place 3 at (8,8): " + r.message());
        r = board.place(1, 7, 3);
        System.out.println("Place 3 at (1,7): " + r.message() + "");

        // 2. The solver starts from the board exactly as the player left it.
        BitmaskSolver solver = new BitmaskSolver();
        long t = System.nanoTime();
        Optional<int[][]> solved = solver.solve(board);
        double ms = (System.nanoTime() - t) / 1e6;
        System.out.println("\nSolved: " + solved.isPresent() + " in " + ms + " ms, "
                + solver.placementsTried() + " placements");
        solved.ifPresent(g -> {
            Board b = new Board();
            b.load(g);
            System.out.print(b.toText());
            verify(g);
        });

        // 3. A contradictory board gives no solution.
        Board bad = Board.parse("5".repeat(1) + "0".repeat(7) + "5" + "0".repeat(72));
        System.out.println("Contradictory board solvable: " + solver.solve(bad).isPresent());
    }

    private static void verify(int[][] g) {
        boolean ok = true;
        for (int i = 0; i < 9; i++) {
            int row = 0, col = 0, box = 0;
            for (int j = 0; j < 9; j++) {
                row |= 1 << (g[i][j] - 1);
                col |= 1 << (g[j][i] - 1);
                box |= 1 << (g[i / 3 * 3 + j / 3][i % 3 * 3 + j % 3] - 1);
            }
            ok &= row == 511 && col == 511 && box == 511;
        }
        int d1 = 0, d2 = 0;
        for (int i = 0; i < 9; i++) {
            d1 |= 1 << (g[i][i] - 1);
            d2 |= 1 << (g[i][8 - i] - 1);
        }
        ok &= d1 == 511 && d2 == 511;
        System.out.println("Independent check of all rows, columns, boxes, diagonals: " + ok);
    }
}