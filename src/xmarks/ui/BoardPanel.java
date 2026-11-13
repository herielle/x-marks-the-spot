package xmarks.ui;

import xmarks.model.Board;
import xmarks.model.GameSession;
import xmarks.model.Rule;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The 9×9 grid, drawn entirely in code. It shows the session's board, the selected cell with
 * its row, column, box and diagonal, every cell holding the same digit, pencil notes, and a red
 * flash on the cells involved in a refused entry. It changes nothing itself; GamePanel does that.
 */
public class BoardPanel extends JComponent {
    private static final long FLASH_MILLIS = 1100;

    private GameSession session;
    private int selRow = -1;
    private int selCol = -1;
    private boolean paused;

    private final List<int[]> flashCells = new ArrayList<>();
    private int flashRow = -1;
    private int flashCol = -1;
    private long flashUntil;
    private final Timer flashTimer;

    public BoardPanel() {
        setOpaque(false);
        flashTimer = new Timer(40, e -> {
            if (System.currentTimeMillis() >= flashUntil) {
                flashCells.clear();
                flashRow = -1;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (paused) return;
                double pitch = pitch();
                int c = (int) Math.floor((e.getX() - frame()) / pitch);
                int r = (int) Math.floor((e.getY() - frame()) / pitch);
                if (r >= 0 && r < 9 && c >= 0 && c < 9) select(r, c);
            }
        });
    }

    // ----- state -----

    public void setSession(GameSession session) {
        this.session = session;
        selRow = -1;
        selCol = -1;
        flashCells.clear();
        flashRow = -1;
        repaint();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        repaint();
    }

    public boolean hasSelection() {
        return selRow >= 0;
    }

    public int selectedRow() {
        return selRow;
    }

    public int selectedCol() {
        return selCol;
    }

    public void select(int r, int c) {
        selRow = r;
        selCol = c;
        repaint();
    }

    /** Moves the selection, starting from the center when nothing is selected yet. */
    public void moveSelection(int dr, int dc) {
        if (!hasSelection()) {
            select(4, 4);
            return;
        }
        select(Math.floorMod(selRow + dr, 9), Math.floorMod(selCol + dc, 9));
    }

    /** Briefly marks the entered cell and the cells whose digit broke the rule. */
    public void flashConflict(int r, int c, int digit, Rule rule) {
        if (session == null) return;
        Board b = session.board();
        flashCells.clear();
        switch (rule) {
            case ROW -> {
                for (int i = 0; i < 9; i++) if (i != c && b.get(r, i) == digit) flashCells.add(new int[] {r, i});
            }
            case COLUMN -> {
                for (int i = 0; i < 9; i++) if (i != r && b.get(i, c) == digit) flashCells.add(new int[] {i, c});
            }
            case BOX -> {
                for (int i = r / 3 * 3; i < r / 3 * 3 + 3; i++) {
                    for (int j = c / 3 * 3; j < c / 3 * 3 + 3; j++) {
                        if ((i != r || j != c) && b.get(i, j) == digit) flashCells.add(new int[] {i, j});
                    }
                }
            }
            case DIAGONAL -> {
                if (Board.onMainDiagonal(r, c)) {
                    for (int i = 0; i < 9; i++) if (i != r && b.get(i, i) == digit) flashCells.add(new int[] {i, i});
                }
                if (Board.onAntiDiagonal(r, c)) {
                    for (int i = 0; i < 9; i++) if (i != r && b.get(i, 8 - i) == digit) flashCells.add(new int[] {i, 8 - i});
                }
            }
        }
        flashRow = r;
        flashCol = c;
        flashUntil = System.currentTimeMillis() + FLASH_MILLIS;
        flashTimer.restart();
        repaint();
    }

    // ----- geometry -----

    private double side() {
        return Math.min(getWidth(), getHeight());
    }

    private double frame() {
        return side() * 0.014;
    }

    private double pitch() {
        return (side() - 2 * frame()) / 9;
    }

    // ----- painting -----

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        double s = side();
        double frame = frame(), pitch = pitch();
        double thick = s * 0.014, thin = Math.max(1.0, s * 0.0022);
        Board board = session == null ? null : session.board();

        g2.setColor(Theme.FRAME);
        g2.fill(new Rectangle2D.Double(0, 0, s, s));

        // cell backgrounds
        double flash = flashRow >= 0 ? Math.max(0, (flashUntil - System.currentTimeMillis()) / (double) FLASH_MILLIS) : 0;
        int selDigit = (board != null && hasSelection() && !paused) ? board.get(selRow, selCol) : 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                g2.setColor(cellColor(board, r, c, selDigit, flash));
                g2.fill(new Rectangle2D.Double(frame + c * pitch, frame + r * pitch, pitch, pitch));
            }
        }

        // grid lines, thick between boxes and around the edge
        for (int i = 0; i <= 9; i++) {
            boolean heavy = i % 3 == 0;
            double w = heavy ? thick : thin;
            double pos = frame + i * pitch;
            double ext = heavy ? thick / 2 : 0;
            g2.setColor(heavy ? Theme.GRID_THICK : Theme.GRID_THIN);
            g2.fill(new Rectangle2D.Double(pos - w / 2, frame - thick / 2, w, 9 * pitch + thick));
            g2.fill(new Rectangle2D.Double(frame - thick / 2, pos - w / 2, 9 * pitch + thick, w));
        }

        if (board != null && !paused) paintDigits(g2, board, pitch, frame);
        if (paused) paintPausedVeil(g2, frame, pitch, s);
        g2.dispose();
    }

    private Color cellColor(Board board, int r, int c, int selDigit, double flash) {
        Color base = (Board.onMainDiagonal(r, c) || Board.onAntiDiagonal(r, c)) ? Theme.CELL_DIAGONAL : Theme.CELL;
        if (paused) return base;

        if (hasSelection()) {
            boolean peer = r == selRow || c == selCol
                    || (r / 3 == selRow / 3 && c / 3 == selCol / 3)
                    || (Board.onMainDiagonal(selRow, selCol) && Board.onMainDiagonal(r, c))
                    || (Board.onAntiDiagonal(selRow, selCol) && Board.onAntiDiagonal(r, c));
            if (peer) base = Theme.mix(base, Theme.CELL_PEER, 0.45);
            if (selDigit != 0 && board != null && board.get(r, c) == selDigit) base = Theme.CELL_SAME;
            if (r == selRow && c == selCol) base = Theme.CELL_SELECTED;
        }
        if (flash > 0) {
            boolean hit = r == flashRow && c == flashCol;
            for (int[] cell : flashCells) if (cell[0] == r && cell[1] == c) hit = true;
            if (hit) base = Theme.mix(base, Theme.CELL_ERROR, 0.8 * flash);
        }
        return base;
    }

    private void paintDigits(Graphics2D g2, Board board, double pitch, double frame) {
        Font given = Theme.body((float) (pitch * 0.56), Font.BOLD);
        Font entry = Theme.body((float) (pitch * 0.56), Font.PLAIN);
        Font note = Theme.body((float) (pitch * 0.26), Font.PLAIN);

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                double x = frame + c * pitch, y = frame + r * pitch;
                int d = board.get(r, c);
                if (d != 0) {
                    g2.setFont(board.isGiven(r, c) ? given : entry);
                    g2.setColor(board.isGiven(r, c) ? Theme.DIGIT_GIVEN : Theme.DIGIT_ENTRY);
                    drawCentered(g2, String.valueOf(d), x, y, pitch, pitch);
                } else {
                    int mask = session.notesMask(r, c);
                    if (mask == 0) continue;
                    g2.setFont(note);
                    g2.setColor(Theme.NOTE);
                    double sub = pitch / 3;
                    for (int n = 1; n <= 9; n++) {
                        if ((mask & (1 << (n - 1))) == 0) continue;
                        drawCentered(g2, String.valueOf(n), x + ((n - 1) % 3) * sub, y + ((n - 1) / 3) * sub, sub, sub);
                    }
                }
            }
        }
    }

    private void paintPausedVeil(Graphics2D g2, double frame, double pitch, double s) {
        g2.setColor(new Color(246, 230, 224, 244));
        g2.fill(new Rectangle2D.Double(frame, frame, 9 * pitch, 9 * pitch));
        g2.setColor(Theme.GRID_THICK);
        g2.setFont(Theme.title((float) (s * 0.085)));
        drawCentered(g2, "PAUSED", 0, s * 0.36, s, s * 0.14);
        g2.setColor(Theme.NOTE);
        g2.setFont(Theme.body((float) (s * 0.034), Font.PLAIN));
        drawCentered(g2, "Click the circle or press Esc to resume", 0, s * 0.52, s, s * 0.06);
    }

    private static void drawCentered(Graphics2D g2, String text, double x, double y, double w, double h) {
        FontMetrics fm = g2.getFontMetrics();
        float tx = (float) (x + (w - fm.stringWidth(text)) / 2);
        float ty = (float) (y + (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.drawString(text, tx, ty);
    }
}