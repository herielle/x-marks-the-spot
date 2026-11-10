package xmarks.model;

/**
 * The 9×9 grid plus the "given" flags. Contains no GUI code.
 * Digits are 1 to 9, and 0 marks an empty cell.
 */
public final class Board {
    public static final int SIZE = 9;
    public static final int BOX = 3;

    private final int[][] cells = new int[SIZE][SIZE];
    private final boolean[][] given = new boolean[SIZE][SIZE];

    public int get(int r, int c) {
        return cells[r][c];
    }

    public boolean isGiven(int r, int c) {
        return given[r][c];
    }

    public static boolean onMainDiagonal(int r, int c) {
        return r == c;
    }

    public static boolean onAntiDiagonal(int r, int c) {
        return r + c == SIZE - 1;
    }

    /**
     * Returns the first rule that digit d would break at (r, c), or null when the entry is legal.
     * The cell's own current value is ignored, so replacing a digit works.
     */
    public Rule findConflict(int r, int c, int d) {
        for (int i = 0; i < SIZE; i++) {
            if (i != c && cells[r][i] == d) return Rule.ROW;
        }
        for (int i = 0; i < SIZE; i++) {
            if (i != r && cells[i][c] == d) return Rule.COLUMN;
        }
        int br = r / BOX * BOX, bc = c / BOX * BOX;
        for (int i = br; i < br + BOX; i++) {
            for (int j = bc; j < bc + BOX; j++) {
                if ((i != r || j != c) && cells[i][j] == d) return Rule.BOX;
            }
        }
        if (onMainDiagonal(r, c)) {
            for (int i = 0; i < SIZE; i++) {
                if (i != r && cells[i][i] == d) return Rule.DIAGONAL;
            }
        }
        if (onAntiDiagonal(r, c)) {
            for (int i = 0; i < SIZE; i++) {
                if (i != r && cells[i][SIZE - 1 - i] == d) return Rule.DIAGONAL;
            }
        }
        return null;
    }

    /** Attempts an entry. A refused entry leaves the board untouched. */
    public MoveResult place(int r, int c, int d) {
        if (given[r][c]) return MoveResult.locked();
        Rule broken = findConflict(r, c, d);
        if (broken != null) return MoveResult.broke(broken, d);
        cells[r][c] = d;
        return MoveResult.ok();
    }

    /** Empties a cell unless it is a given. Returns true when something changed. */
    public boolean clear(int r, int c) {
        if (given[r][c] || cells[r][c] == 0) return false;
        cells[r][c] = 0;
        return true;
    }

    public boolean isComplete() {
        for (int[] row : cells) {
            for (int v : row) if (v == 0) return false;
        }
        return true;
    }

    /** Independent copy that keeps the given flags. */
    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) {
            b.cells[r] = cells[r].clone();
            b.given[r] = given[r].clone();
        }
        return b;
    }

    /** Overwrites every cell with a solved grid, leaving the given flags alone. */
    public void load(int[][] grid) {
        for (int r = 0; r < SIZE; r++) cells[r] = grid[r].clone();
    }

    /** Locks every filled cell. Useful when a generated puzzle becomes the starting point. */
    public void lockFilledCells() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) given[r][c] = cells[r][c] != 0;
        }
    }

    /**
     * Reads 81 digits (0 or '.' for empty) and ignores whitespace, so the same method
     * handles one long line or nine lines of nine. Returns null on malformed text.
     */
    public static Board parse(String text) {
        String s = text.replaceAll("\\s+", "").replace('.', '0');
        if (s.length() != SIZE * SIZE) return null;
        Board b = new Board();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return null;
            b.cells[i / SIZE][i % SIZE] = ch - '0';
        }
        return b;
    }

    /** Nine lines of nine digits, the format SaveManager will write. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) sb.append(cells[r][c]);
            sb.append('\n');
        }
        return sb.toString();
    }
}