package xmarks.model;

/** The four Sudoku X rules. Each one knows how to describe itself in a refusal message. */
public enum Rule {
    ROW("in this row"),
    COLUMN("in this column"),
    BOX("in this 3×3 box"),
    DIAGONAL("on this diagonal");

    private final String phrase;

    Rule(String phrase) {
        this.phrase = phrase;
    }

    /** Text such as "in this row", ready to be placed after "already appears". */
    public String phrase() {
        return phrase;
    }
}