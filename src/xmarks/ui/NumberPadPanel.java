package xmarks.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

/** The 1 to 9 pad. A digit that already appears nine times is dimmed and stops responding. */
public class NumberPadPanel extends JPanel {
    private final PadButton[] buttons = new PadButton[9];

    public NumberPadPanel(IntConsumer onDigit) {
        setLayout(null);
        setOpaque(false);
        for (int i = 0; i < 9; i++) {
            buttons[i] = new PadButton(i + 1, onDigit);
            add(buttons[i]);
        }
    }

    /** done[d - 1] is true when digit d is fully placed. */
    public void setCompleted(boolean[] done) {
        for (int i = 0; i < 9; i++) buttons[i].setCompleted(done[i]);
    }

    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        double gapX = w * 0.055, gapY = h * 0.055;
        double cw = (w - 2 * gapX) / 3, ch = (h - 2 * gapY) / 3;
        for (int i = 0; i < 9; i++) {
            int x = (int) Math.round((i % 3) * (cw + gapX));
            int y = (int) Math.round((i / 3) * (ch + gapY));
            buttons[i].setBounds(x, y, (int) Math.round(cw), (int) Math.round(ch));
        }
    }

    private static final class PadButton extends JComponent {
        private final int digit;
        private boolean hover, pressed, completed;

        PadButton(int digit, IntConsumer onDigit) {
            this.digit = digit;
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean fire = pressed && hover && !completed;
                    pressed = false;
                    repaint();
                    if (fire) onDigit.accept(PadButton.this.digit);
                }
            });
        }

        void setCompleted(boolean completed) {
            if (this.completed == completed) return;
            this.completed = completed;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth(), h = getHeight();
            Color fill = Theme.PAD;
            if (completed) fill = Theme.mix(fill, Theme.FRAME, 0.35);
            else if (pressed) fill = Theme.mix(fill, Color.BLACK, 0.12);
            else if (hover) fill = Theme.mix(fill, Color.WHITE, 0.18);

            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 5, 5);
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(Theme.PAD_BORDER);
            g2.draw(shape);

            g2.setFont(Theme.body(h * 0.46f, Font.PLAIN));
            g2.setColor(completed ? new Color(255, 255, 255, 120) : Color.WHITE);
            String text = String.valueOf(digit);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }
}