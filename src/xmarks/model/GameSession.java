package xmarks.model;

import xmarks.util.GameTimer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * One play-through: the board, the mistake counter (0/3 on the game screen), the timer,
 * undo history and pencil notes. The UI reads state from here and changes it only through
 * enter, erase, undo and applySolution.
 *
 * The session never calls the solver. The caller runs BitmaskSolver and hands the answer
 * to applySolution, which keeps the model package free of any dependency on the solver.
 */
public final class GameSession {
    public static final int MAX_MISTAKES = 3;

    // Score for a won game: start from the base, lose points for time and for each mistake.
    private static final int SCORE_BASE = 100_000;
    private static final int SCORE_PER_SECOND = 100;
    private static final int SCORE_PER_MISTAKE = 5_000;

    public enum Status { PLAYING, WON, LOST, SOLVED_BY_PROGRAM }

    private record Move(int r, int c, int previous, int next) {}

    private final Board board;
    private final GameTimer timer = new GameTimer();
    private final Deque<Move> history = new ArrayDeque<>();
    private final int[][] notes = new int[Board.SIZE][Board.SIZE]; // bit (d - 1) set = note for d
    private int mistakes;
    private boolean notesMode;
    private Status status = Status.PLAYING;
    private Consumer<GameSession> onChange = s -> {};

    public GameSession(Board board) {
        this.board = board;
    }

    /** Rebuilds a session from a save file. Saves are only written while a game is in progress. */
    public static GameSession restore(Board board, int mistakes, long seconds) {
        GameSession s = new GameSession(board);
        s.mistakes = Math.max(0, Math.min(mistakes, MAX_MISTAKES - 1));
        s.timer.setElapsedSeconds(seconds);
        return s;
    }

    /** Called after every action. SaveManager.save is wired here by the launcher. */
    public void setOnChange(Consumer<GameSession> listener) {
        this.onChange = listener == null ? s -> {} : listener;
    }

    // ----- actions -----

    public MoveResult enter(int r, int c, int d) {
        if (status != Status.PLAYING) return MoveResult.over();

        if (notesMode) {
            if (board.get(r, c) == 0 && !board.isGiven(r, c)) {
                notes[r][c] ^= 1 << (d - 1);
                onChange.accept(this);
            }
            return MoveResult.ok();
        }

        int previous = board.get(r, c);
        MoveResult result = board.place(r, c, d);

        if (result.accepted()) {
            if (previous != d) {
                history.push(new Move(r, c, previous, d));
                notes[r][c] = 0;
            }
            if (board.isComplete()) finish(Status.WON);
        } else if (result.rule() != null) {
            mistakes++;
            if (mistakes >= MAX_MISTAKES) finish(Status.LOST);
        }
        onChange.accept(this);
        return result;
    }

    /** Empties a cell, or wipes its pencil notes when it is already empty. */
    public boolean erase(int r, int c) {
        if (status != Status.PLAYING) return false;
        int previous = board.get(r, c);
        boolean changed;
        if (board.clear(r, c)) {
            history.push(new Move(r, c, previous, 0));
            changed = true;
        } else if (previous == 0 && notes[r][c] != 0) {
            notes[r][c] = 0;
            changed = true;
        } else {
            changed = false;
        }
        if (changed) onChange.accept(this);
        return changed;
    }

    /**
     * Reverses the latest entry or erase. Mistakes stay counted. Moves are undone in reverse
     * order, so the earlier state was legal when it was made and place() accepts it again.
     */
    public boolean undo() {
        if (status != Status.PLAYING || history.isEmpty()) return false;
        Move m = history.pop();
        if (m.previous() == 0) board.clear(m.r(), m.c());
        else board.place(m.r(), m.c(), m.previous());
        onChange.accept(this);
        return true;
    }

    /** Accepts a grid produced by the solver. Ends the game without a win. */
    public void applySolution(int[][] grid) {
        if (status != Status.PLAYING) return;
        board.load(grid);
        history.clear();
        finish(Status.SOLVED_BY_PROGRAM);
        onChange.accept(this);
    }

    // ----- timer and notes -----

    public void start() {
        if (status == Status.PLAYING) timer.start();
    }

    public void pause() {
        timer.pause();
    }

    public void toggleNotesMode() {
        notesMode = !notesMode;
    }

    private void finish(Status end) {
        status = end;
        timer.pause();
    }

    // ----- state for the UI -----

    public Board board() { return board; }
    public int mistakes() { return mistakes; }
    public Status status() { return status; }
    public boolean notesMode() { return notesMode; }
    public int notesMask(int r, int c) { return notes[r][c]; }
    public long elapsedSeconds() { return timer.elapsedSeconds(); }

    /** Points for a finished game. Only a win scores; a loss or a solver finish gives 0. */
    public int score() {
        if (status != Status.WON) return 0;
        long raw = SCORE_BASE - SCORE_PER_SECOND * elapsedSeconds() - (long) SCORE_PER_MISTAKE * mistakes;
        return (int) Math.max(0, raw);
    }
}