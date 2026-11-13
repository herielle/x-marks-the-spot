package xmarks.ui;

import xmarks.model.GameSession;
import xmarks.model.MoveResult;
import xmarks.solver.BitmaskSolver;
import xmarks.storage.SaveManager;
import xmarks.util.GameTimer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

/**
 * The game screen from the mockup: the board on the left and, on the right, mistakes and time,
 * the undo, erase and pencil buttons, the number pad, and MENU and SOLVE buttons. It translates
 * clicks and keys into GameSession calls and shows the result popup when the game ends.
 *
 * Keyboard: 1 to 9 enter a digit, arrows move, Backspace or Delete erase, Ctrl/Cmd+Z undo,
 * N toggles pencil notes, Esc or P pause.
 */
public class GamePanel extends BackdropPanel {
    private final SaveManager saves;
    private final Runnable onMenu;

    private final BoardPanel board = new BoardPanel();
    private final NumberPadPanel pad = new NumberPadPanel(this::enterDigit);
    private final IconButton undoButton = new IconButton(IconButton.Glyph.UNDO,
            new String[] {"buttons/undo.png"}, "Undo", this::undo);
    private final IconButton eraseButton = new IconButton(IconButton.Glyph.ERASE,
            new String[] {"buttons/erase.png"}, "Erase", this::erase);
    private final IconButton penButton = new IconButton(IconButton.Glyph.PEN,
            new String[] {"buttons/pen_off.png", "buttons/pen_on.png"}, "Pencil notes (N)", this::toggleNotes);
    private final PauseButton pauseButton = new PauseButton(this::togglePause);
    private final GlassButton menuButton = new GlassButton("MENU");
    private final GlassButton solveButton = new GlassButton("SOLVE");
    private final ResultOverlay overlay;
    private final Timer clockTimer = new Timer(100, e -> repaint());

    private GameSession session;
    private boolean paused;
    private String toast = "";
    private long toastUntil;

    // geometry shared by doLayout and paintComponent
    private double px, py, pw, ph, colX, colW;

    /**
     * @param saves     written after every action
     * @param onMenu    called when the player goes back to the main menu
     * @param onNewGame called when the player asks for another game from the result popup
     */
    public GamePanel(SaveManager saves, Runnable onMenu, Runnable onNewGame) {
        super(false);
        this.saves = saves;
        this.onMenu = onMenu;
        setLayout(null);

        for (GlassButton b : new GlassButton[] {menuButton, solveButton}) {
            b.setFocusable(false);
            b.setForeground(Theme.INK);
        }
        menuButton.addActionListener(e -> leave());
        solveButton.addActionListener(e -> solve());

        overlay = new ResultOverlay(onNewGame, this::leave);

        add(overlay); // first child is painted last, so the popup sits above everything
        add(board);
        add(pad);
        add(undoButton);
        add(eraseButton);
        add(penButton);
        add(pauseButton);
        add(menuButton);
        add(solveButton);

        installKeys();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing()) resume();
            else halt();
        });
    }

    // ----- life cycle -----

    /** Begins (or resumes) a game. The panel takes over the session's change listener. */
    public void startGame(GameSession s) {
        session = s;
        paused = false;
        toast = "";
        s.setOnChange(this::onChange);
        board.setSession(s);
        board.setPaused(false);
        pauseButton.setPaused(false);
        penButton.setState(s.notesMode() ? 1 : 0);
        overlay.hideOverlay();
        refreshPad();
        if (isShowing()) resume();
        repaint();
    }

    /** Writes the current game to disk. Called on leaving the screen and when the window closes. */
    public void persist() {
        if (session != null && session.status() == GameSession.Status.PLAYING) saves.save(session);
    }

    private void resume() {
        if (session != null && !paused) session.start();
        clockTimer.start();
        repaint();
    }

    private void halt() {
        if (session != null) session.pause();
        persist();
        clockTimer.stop();
    }

    private void leave() {
        persist();
        onMenu.run();
    }

    private void onChange(GameSession s) {
        saves.save(s); // a finished game deletes the save instead
        refreshPad();
        board.repaint();
        repaint();
        if (s.status() != GameSession.Status.PLAYING) {
            toastUntil = 0; // a stale refusal message should not sit behind the result popup
            clockTimer.stop();
            overlay.showFor(s);
        }
    }

    private void refreshPad() {
        boolean[] done = new boolean[9];
        if (session != null) {
            int[] count = new int[10];
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) count[session.board().get(r, c)]++;
            }
            for (int d = 1; d <= 9; d++) done[d - 1] = count[d] >= 9;
        }
        pad.setCompleted(done);
    }

    // ----- actions -----

    private boolean playing() {
        return session != null && !paused && session.status() == GameSession.Status.PLAYING;
    }

    /** Selects a cell. Used by the board and by tests. */
    public void selectCell(int r, int c) {
        board.select(r, c);
    }

    public void enterDigit(int d) {
        if (!playing()) return;
        if (!board.hasSelection()) {
            showToast("Select a cell first.");
            return;
        }
        int r = board.selectedRow(), c = board.selectedCol();
        MoveResult result = session.enter(r, c, d);
        if (result.accepted()) return;
        showToast(result.message());
        if (result.rule() != null) board.flashConflict(r, c, d, result.rule());
    }

    public void erase() {
        if (playing() && board.hasSelection()) session.erase(board.selectedRow(), board.selectedCol());
    }

    public void undo() {
        if (playing()) session.undo();
    }

    public void toggleNotes() {
        if (session == null || session.status() != GameSession.Status.PLAYING) return;
        session.toggleNotesMode();
        penButton.setState(session.notesMode() ? 1 : 0);
    }

    public void togglePause() {
        if (session == null || session.status() != GameSession.Status.PLAYING) return;
        paused = !paused;
        if (paused) session.pause();
        else session.start();
        board.setPaused(paused);
        pauseButton.setPaused(paused);
        repaint();
    }

    /** Asks the solver to finish the board exactly as the player left it. */
    public void solve() {
        if (!playing()) return;
        Optional<int[][]> solution = new BitmaskSolver().solve(session.board());
        if (solution.isPresent()) session.applySolution(solution.get());
        else showToast("No solution from this board. Try undoing a few entries.");
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 2600;
        repaint();
    }

    // ----- keyboard -----

    private void installKeys() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        for (int d = 1; d <= 9; d++) {
            int digit = d;
            bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_0 + d, 0), () -> enterDigit(digit));
            bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + d, 0), () -> enterDigit(digit));
        }
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), () -> moveCursor(-1, 0));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), () -> moveCursor(1, 0));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), () -> moveCursor(0, -1));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), () -> moveCursor(0, 1));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), this::erase);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::erase);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), this::erase);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), this::toggleNotes);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::togglePause);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), this::togglePause);
        int shortcut = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut), this::undo);
    }

    private void moveCursor(int dr, int dc) {
        if (playing()) board.moveSelection(dr, dc);
    }

    private static int bindings;

    private static void bind(InputMap im, ActionMap am, KeyStroke key, Runnable action) {
        String name = "game-action-" + bindings++;
        im.put(key, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ----- layout -----

    /** The glass panel keeps the mockup's proportions and stays centered. */
    private void computeGeometry(int w, int h) {
        ph = 0.77 * h;
        pw = ph * 1.572;
        if (pw > 0.94 * w) {
            pw = 0.94 * w;
            ph = pw / 1.572;
        }
        px = (w - pw) / 2;
        py = (h - ph) / 2;
        colX = px + 0.665 * pw;
        colW = 0.30 * pw;
    }

    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        computeGeometry(w, h);
        overlay.setBounds(0, 0, w, h);

        double side = 0.91 * ph;
        board.setBounds(round(px + 0.056 * pw), round(py + (ph - side) / 2), round(side), round(side));

        double circle = 0.10 * ph, box = circle * IconButton.MARGIN;
        double rowY = py + 0.27 * ph;
        IconButton[] icons = {undoButton, eraseButton, penButton};
        for (int i = 0; i < icons.length; i++) {
            double cx = colX + colW / 2 + (i - 1) * 0.30 * colW;
            icons[i].setBounds(round(cx - box / 2), round(rowY - box / 2), round(box), round(box));
        }

        double pauseD = 0.056 * ph;
        pauseButton.setBounds(round(colX + colW - pauseD), round(py + 0.15 * ph - pauseD / 2), round(pauseD), round(pauseD));

        double gap = 0.055 * colW;
        double padSize = colW; // square cells
        double padTop = py + 0.37 * ph;
        pad.setBounds(round(colX), round(padTop), round(padSize), round(padSize));

        double pillTop = padTop + padSize + 0.03 * ph, pillH = 0.07 * ph, pillW = (colW - gap) / 2;
        menuButton.setBounds(round(colX), round(pillTop), round(pillW), round(pillH));
        solveButton.setBounds(round(colX + pillW + gap), round(pillTop), round(pillW), round(pillH));
        Font small = Theme.body((float) (pillH * 0.38), Font.PLAIN);
        menuButton.setFont(small);
        solveButton.setFont(small);
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }

    // ----- painting -----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // the night sky
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        computeGeometry(w, h);

        Graphics2D g2 = Theme.smooth(g);
        GlassPanel.paintGlass(g2, (float) px, (float) py, (float) pw, (float) ph, (int) (ph * 0.16), 0);

        if (session != null) {
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.body((float) (ph * 0.028), Font.PLAIN));
            g2.drawString("Mistakes", (float) colX, (float) (py + 0.13 * ph));
            g2.drawString("Time", (float) timeX(g2, "Time"), (float) (py + 0.13 * ph));

            g2.setFont(Theme.body((float) (ph * 0.038), Font.BOLD));
            g2.drawString(session.mistakes() + "/" + GameSession.MAX_MISTAKES, (float) colX, (float) (py + 0.18 * ph));
            String clock = GameTimer.clock(session.elapsedSeconds());
            g2.drawString(clock, (float) timeX(g2, clock), (float) (py + 0.18 * ph));
        }

        if (System.currentTimeMillis() < toastUntil) paintToast(g2);
        g2.dispose();
    }

    /** Right-aligns text so it ends just before the pause circle. */
    private double timeX(Graphics2D g2, String text) {
        double right = colX + colW - 0.056 * ph - 0.03 * ph;
        return right - g2.getFontMetrics().stringWidth(text);
    }

    /** Red pill above the panel naming the rule that refused the entry. */
    private void paintToast(Graphics2D g2) {
        g2.setFont(Theme.body((float) Math.max(13, ph * 0.034), Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        double tw = fm.stringWidth(toast) + ph * 0.07, th = fm.getHeight() + ph * 0.025;
        double tx = (getWidth() - tw) / 2, ty = Math.max(8, py - th - ph * 0.02);
        g2.setColor(new Color(190, 62, 62, 228));
        g2.fill(new RoundRectangle2D.Double(tx, ty, tw, th, th, th));
        g2.setColor(Color.WHITE);
        g2.drawString(toast, (float) (tx + (tw - fm.stringWidth(toast)) / 2), (float) (ty + (th - fm.getHeight()) / 2 + fm.getAscent()));
    }
}