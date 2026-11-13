package xmarks.ui;

import xmarks.model.GameSession;
import xmarks.util.GameTimer;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * The result popup from the mockup: a blush card over the dimmed game with the score, the time
 * and two buttons. While it is visible it swallows every mouse event, so the game underneath
 * cannot be touched.
 */
final class ResultOverlay extends JComponent {
    private final Runnable onPlayAgain;
    private final Runnable onMenu;

    private String label = "";
    private String scoreText = "";
    private String timeText = "";
    private String detailText = "";

    private RoundRectangle2D.Double againRect, menuRect;
    private int hover = -1; // 0 = play again, 1 = menu

    ResultOverlay(Runnable onPlayAgain, Runnable onMenu) {
        this.onPlayAgain = onPlayAgain;
        this.onMenu = onMenu;
        setVisible(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int h = hit(e);
                if (h != hover) {
                    hover = h;
                    setCursor(h >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int h = hit(e);
                if (h < 0) return;
                hideOverlay();
                (h == 0 ? ResultOverlay.this.onPlayAgain : ResultOverlay.this.onMenu).run();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void showFor(GameSession s) {
        long seconds = s.elapsedSeconds();
        timeText = GameTimer.words(seconds);
        switch (s.status()) {
            case WON -> {
                label = "SCORE:";
                scoreText = String.format("%,d", s.score());
                detailText = s.mistakes() == 1 ? "1 mistake" : s.mistakes() + " mistakes";
            }
            case LOST -> {
                label = "GAME OVER";
                scoreText = "0";
                detailText = "Three mistakes made";
            }
            default -> {
                label = "SOLVED BY THE PROGRAM";
                scoreText = "0";
                detailText = "No score for a solver finish";
            }
        }
        hover = -1;
        setVisible(true);
        repaint();
    }

    void hideOverlay() {
        setVisible(false);
    }

    private int hit(MouseEvent e) {
        if (againRect != null && againRect.contains(e.getPoint())) return 0;
        if (menuRect != null && menuRect.contains(e.getPoint())) return 1;
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(4, 14, 20, 150));
        g2.fillRect(0, 0, w, h);

        double cw = Math.min(0.42 * w, 0.62 * h * 1.15);
        double ch = cw * 0.92;
        double cx = (w - cw) / 2, cy = (h - ch) / 2;
        g2.setColor(Theme.CARD);
        g2.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, cw * 0.12, cw * 0.12));

        g2.setColor(new Color(0x3A3A3A));
        g2.setFont(Theme.body((float) (ch * 0.075), Font.PLAIN));
        centered(g2, label, cx, cy + ch * 0.07, cw, ch * 0.1);

        double bx = cx + cw * 0.1, by = cy + ch * 0.2, bw = cw * 0.8, bh = ch * 0.27;
        g2.setColor(Theme.SCORE_BOX);
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, bh * 0.22, bh * 0.22));
        float size = (float) (bh * 0.62);
        Font f = Theme.body(size, Font.PLAIN);
        while (size > 10 && g2.getFontMetrics(f).stringWidth(scoreText) > bw * 0.9) {
            size -= 2;
            f = Theme.body(size, Font.PLAIN);
        }
        g2.setFont(f);
        g2.setColor(Color.WHITE);
        centered(g2, scoreText, bx, by, bw, bh);

        g2.setColor(new Color(0x3A3A3A));
        g2.setFont(Theme.body((float) (ch * 0.1), Font.PLAIN));
        centered(g2, timeText, cx, cy + ch * 0.5, cw, ch * 0.13);
        g2.setColor(new Color(0x5A6B73));
        g2.setFont(Theme.body((float) (ch * 0.055), Font.PLAIN));
        centered(g2, detailText, cx, cy + ch * 0.63, cw, ch * 0.08);

        double pw = cw * 0.38, ph = ch * 0.12, py = cy + ch * 0.77, gap = cw * 0.04;
        againRect = new RoundRectangle2D.Double(cx + cw / 2 - gap / 2 - pw, py, pw, ph, ph, ph);
        menuRect = new RoundRectangle2D.Double(cx + cw / 2 + gap / 2, py, pw, ph, ph, ph);
        pill(g2, againRect, "PLAY AGAIN", hover == 0, ph);
        pill(g2, menuRect, "MENU", hover == 1, ph);
        g2.dispose();
    }

    private static void pill(Graphics2D g2, RoundRectangle2D.Double r, String text, boolean lit, double h) {
        g2.setColor(lit ? Theme.mix(Theme.PAD, Color.WHITE, 0.18) : Theme.PAD);
        g2.fill(r);
        g2.setColor(Color.WHITE);
        g2.setFont(Theme.body((float) (h * 0.38), Font.PLAIN));
        centered(g2, text, r.x, r.y, r.width, r.height);
    }

    private static void centered(Graphics2D g2, String text, double x, double y, double w, double h) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (float) (x + (w - fm.stringWidth(text)) / 2),
                (float) (y + (h - fm.getHeight()) / 2 + fm.getAscent()));
    }
}