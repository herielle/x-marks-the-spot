package xmarks.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Pill-shaped glass button used on the menu and the info screens. */
public class GlassButton extends JButton {

    public GlassButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setForeground(Theme.TEXT);
        setFont(Theme.body(16f, Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(240, 46));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int w = getWidth(), h = getHeight();

        int boost = getModel().isPressed() ? -30 : getModel().isRollover() ? 40 : 0;
        paintPill(g2, getText(), getFont(), getForeground(), w, h, boost);

        if (isFocusOwner()) { // thin ring so keyboard users can see where they are
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.draw(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, h - 8, h - 8));
        }
        g2.dispose();
    }

    /** Glass pill with centered text, drawn at the origin of g2. */
    static void paintPill(Graphics2D g2, String text, Font font, Color fg, int w, int h, int boost) {
        GlassPanel.paintGlass(g2, 0, 0, w, h, h, boost);
        g2.setFont(font);
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }
}