package xmarks.ui;

import xmarks.model.GameSession;
import xmarks.solver.PuzzleGenerator;
import xmarks.storage.SaveManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/** The single window. Screens are cards in one CardLayout, and showScreen swaps between them. */
public final class AppWindow extends JFrame {
    public static final String MENU = "menu";
    public static final String HOW_TO_PLAY = "howto";
    public static final String CREDITS = "credits";
    public static final String GAME = "game";

    /**
     * How many digits a new puzzle starts with. More clues make an easier game, and 0 gives an
     * empty board to fill from scratch.
     */
    private static final int PUZZLE_CLUES = 34;

    private final SaveManager saves = new SaveManager();
    private final GamePanel game;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MainMenuPanel menu;

    public AppWindow() {
        super("X Marks The Spot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        menu = new MainMenuPanel(this::startGame, () -> showScreen(HOW_TO_PLAY),
                () -> showScreen(CREDITS), this::exit);
        game = new GamePanel(saves, () -> showScreen(MENU), this::newGame);
        root.add(menu, MENU);
        root.add(game, GAME);
        root.add(new HowToPlayPanel(() -> showScreen(MENU)), HOW_TO_PLAY);
        root.add(new CreditsPanel(() -> showScreen(MENU)), CREDITS);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                game.persist();
            }
        });

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

    /** Resumes the saved game when there is one, and otherwise starts a fresh puzzle. */
    private void startGame() {
        game.startGame(saves.load().orElseGet(AppWindow::freshSession));
        showScreen(GAME);
    }

    private void newGame() {
        saves.clear();
        game.startGame(freshSession());
    }

    private static GameSession freshSession() {
        return new GameSession(PuzzleGenerator.generate(PUZZLE_CLUES, new Random()));
    }

    private void exit() {
        game.persist();
        dispose();
        System.exit(0);
    }
}