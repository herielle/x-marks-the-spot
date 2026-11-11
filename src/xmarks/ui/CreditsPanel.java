package xmarks.ui;

/** Author and course details. */
public class CreditsPanel extends InfoPanel {
    private static final String BODY =
            "X Marks The Spot: Diagonal Sudoku\n\n"
            + "Developed by Herielle G. Margallo\n"
            + "CMSC 122, Department of Computer Science and Mathematics\n"
            + "College of Science, University of the Philippines Tacloban\n"
            + "First Semester AY 2026 – 2027\n\n"
            + "Built with Java and Swing.";

    public CreditsPanel(Runnable onBack) {
        super("Credits", BODY, onBack);
    }
}