package xmarks.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Frosted, rounded, translucent container. Also supplies the glass look to buttons and the logo. */
public class GlassPanel extends JPanel {
    private final int arc;
    private final int darken;

    public GlassPanel(int arc) {
        this(arc, 0);
    }

    /** darken (0 to 255) lays a dark tint under the glass, which keeps text readable over busy backgrounds. */
    public GlassPanel(int arc, int darken) {
        this.arc = arc;
        this.darken = darken;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (darken > 0) {
            g2.setColor(new Color(8, 22, 31, darken));
            g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));
        }
        paintGlass(g2, 0, 0, getWidth(), getHeight(), arc, 0);
        g2.dispose();
    }

    /** Draws one glass shape. A positive boost brightens it (hover), a negative one darkens it (pressed). */
    static void paintGlass(Graphics2D g2, float x, float y, float w, float h, int arc, int boost) {
        RoundRectangle2D shape = new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h - 2, arc, arc);
        g2.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, clamp(84 + boost)),
                x, y + h, new Color(255, 255, 255, clamp(40 + boost))));
        g2.fill(shape);
        g2.setStroke(new BasicStroke(1.6f));
        g2.setPaint(new Color(255, 255, 255, clamp(150 + boost)));
        g2.draw(shape);
    }

    private static int clamp(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }
}