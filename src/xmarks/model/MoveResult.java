package xmarks.model;

/**
 * Outcome of one attempted entry. When refused because of a Sudoku rule, {@code rule} names it.
 * When refused for another reason (a locked cell), {@code rule} is null.
 */
public record MoveResult(boolean accepted, Rule rule, String message) {

    public static MoveResult ok() {
        return new MoveResult(true, null, "");
    }

    public static MoveResult broke(Rule rule, int digit) {
        return new MoveResult(false, rule, digit + " already appears " + rule.phrase() + ".");
    }

    public static MoveResult locked() {
        return new MoveResult(false, null, "That cell belongs to the puzzle and cannot be changed.");
    }
}