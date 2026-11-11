package xmarks.ui;

/** Rules and controls. */
public class HowToPlayPanel extends InfoPanel {
    private static final String BODY =
            "Fill every cell with a digit from 1 to 9 so that no digit repeats in any row, column, "
            + "3×3 box, or either of the two long diagonals.\n\n"
            + "Select a cell, then click a number on the pad or press a digit on your keyboard. "
            + "An entry that breaks a rule is refused, and the message names the rule involved. "
            + "Each refusal adds a mistake, and the game ends at 3 mistakes.\n\n"
            + "Erase clears the selected cell, Undo takes back your last entry, and the pencil "
            + "switches to notes so you can jot candidate digits in a cell.\n\n"
            + "You can ask the program to solve the puzzle at any moment. It continues from the "
            + "board exactly as you left it.";

    public HowToPlayPanel(Runnable onBack) {
        super("How to play", BODY, onBack);
    }
}