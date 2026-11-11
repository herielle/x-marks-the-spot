package xmarks.ui;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/** Title card drawn in code, used when resources/title.png is missing. */
final class FallbackArt {
    private FallbackArt() {}

    static final double TITLE_ASPECT = 580.0 / 190.0;
    private static final String[] LINES = {"X MARKS", "THE SPOT"};

    /** Glass card with a thickened outline and a green drop shadow, drawn at the origin of g2. */
    static void paintTitle(Graphics2D g2, int w, int h) {
        GlassPanel.paintGlass(g2, 0, 0, w, h, (int) (h * 0.30), 0);

        FontRenderContext frc = g2.getFontRenderContext();
        float size = h * 0.40f;
        Font font = Theme.title(size);
        while (size > 12 && widest(font, frc) > w * 0.82) { // shrink until the longer line fits the card
            size -= 2;
            font = Theme.title(size);
        }

        float lineGap = size * 0.98f;
        float capHeight = size * 0.72f;
        float firstBaseline = (h - (lineGap + capHeight)) / 2f + capHeight;
        float stroke = size * 0.07f;

        Shape[] outlines = new Shape[LINES.length];
        for (int i = 0; i < LINES.length; i++) {
            TextLayout layout = new TextLayout(LINES[i], font, frc);
            Rectangle2D b = layout.getBounds();
            double x = (w - b.getWidth()) / 2 - b.getX();
            outlines[i] = layout.getOutline(AffineTransform.getTranslateInstance(x, firstBaseline + i * lineGap));
        }

        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        AffineTransform shadow = AffineTransform.getTranslateInstance(size * 0.05, size * 0.07);
        g2.setColor(Theme.LOGO_SHADOW);
        for (Shape s : outlines) {
            Shape moved = shadow.createTransformedShape(s);
            g2.draw(moved);
            g2.fill(moved);
        }
        g2.setColor(Theme.LOGO);
        for (Shape s : outlines) {
            g2.draw(s);
            g2.fill(s);
        }
    }

    private static double widest(Font font, FontRenderContext frc) {
        double max = 0;
        for (String line : LINES) {
            max = Math.max(max, new TextLayout(line, font, frc).getBounds().getWidth());
        }
        return max;
    }
}