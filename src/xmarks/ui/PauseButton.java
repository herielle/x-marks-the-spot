package xmarks.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** The white circle beside the timer. Shows pause bars while the clock runs and a play triangle while paused. */
public class PauseButton extends JComponent {
    private boolean paused, hover;

    public PauseButton(Runnable action) {
        setFocusable(false);
        setToolTipText("Pause");
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
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (contains(e.getPoint())) action.run();
            }
        });
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        setToolTipText(paused ? "Resume" : "Pause");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        double d = Math.min(getWidth(), getHeight()) - 1;
        double x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
        g2.setColor(hover ? Color.WHITE : new Color(0xF4F6FB));
        g2.fill(new Ellipse2D.Double(x, y, d, d));

        g2.setColor(Theme.GRID_THICK);
        if (paused) {
            Path2D tri = new Path2D.Double();
            tri.moveTo(x + d * 0.38, y + d * 0.28);
            tri.lineTo(x + d * 0.38, y + d * 0.72);
            tri.lineTo(x + d * 0.74, y + d * 0.5);
            tri.closePath();
            g2.fill(tri);
        } else {
            double bw = d * 0.12, bh = d * 0.42;
            g2.fill(new RoundRectangle2D.Double(x + d * 0.34, y + d * 0.29, bw, bh, 2, 2));
            g2.fill(new RoundRectangle2D.Double(x + d * 0.54, y + d * 0.29, bw, bh, 2, 2));
        }
        g2.dispose();
    }
}