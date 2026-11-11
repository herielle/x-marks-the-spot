package xmarks.ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/** The single window. Screens are cards in one CardLayout, and showScreen swaps between them. */
public final class AppWindow extends JFrame {
    public static final String MENU = "menu";
    public static final String HOW_TO_PLAY = "howto";
    public static final String CREDITS = "credits";
    public static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MainMenuPanel menu;

    public AppWindow() {
        super("X Marks The Spot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        menu = new MainMenuPanel(this::startGame, () -> showScreen(HOW_TO_PLAY),
                () -> showScreen(CREDITS), this::exit);
        root.add(menu, MENU);
        root.add(new HowToPlayPanel(() -> showScreen(MENU)), HOW_TO_PLAY);
        root.add(new CreditsPanel(() -> showScreen(MENU)), CREDITS);

        root.setPreferredSize(new Dimension(1152, 648)); // 16:9, the shape of the background art
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(960, 560));
        setLocationRelativeTo(null);
        showScreen(MENU);
    }

    /** Shows the window and starts the title animation that leads into the menu. */
    public void open() {
        setVisible(true);
        menu.playIntro();
    }

    public void showScreen(String name) {
        cards.show(root, name);
    }

    private void startGame() {
        // The game screen arrives in the next step and will be registered under GAME.
        JOptionPane.showMessageDialog(this, "The game screen comes in the next step.", "Play", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exit() {
        dispose();
        System.exit(0);
    }
}