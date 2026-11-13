package xmarks.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Round icon button (undo, erase, pencil) drawn from your PNGs.
 *
 * The icon files are canvases with the circle in the same spot, and pen_off.png and pen_on.png
 * add a badge that pokes out past the circle. Every state is therefore placed using the circle
 * found in undo.png or erase.png, so the circle never jumps when the badge changes. The component
 * is larger than the circle (by MARGIN) to leave room for that badge. Without the PNGs, a plain
 * white circle with a simple drawn symbol appears instead.
 */
public class IconButton extends JComponent {
    /** Component size divided by circle diameter. */
    static final double MARGIN = 1.36;

    public enum Glyph { UNDO, ERASE, PEN }

    private static Sprite anchor;
    private static boolean anchorLoaded;

    private final Glyph glyph;
    private final Sprite[] states;
    private int state;
    private boolean hover, pressed;

    private final BufferedImage[] pre;
    private double preK, preDpr;

    /** files holds one image per state, for example {"buttons/pen_off.png", "buttons/pen_on.png"}. */
    public IconButton(Glyph glyph, String[] files, String tooltip, Runnable action) {
        this.glyph = glyph;
        this.states = new Sprite[files.length];
        this.pre = new BufferedImage[files.length];
        for (int i = 0; i < files.length; i++) states[i] = Sprite.load(files[i]);

        setFocusable(false);
        setToolTipText(tooltip);
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
                boolean fire = pressed && hover;
                pressed = false;
                repaint();
                if (fire) action.run();
            }
        });
    }

    public void setState(int state) {
        this.state = Math.max(0, Math.min(states.length - 1, state));
        repaint();
    }

    /** The circle that every icon is measured against: undo.png, or erase.png when undo is missing. */
    private static Sprite anchor() {
        if (!anchorLoaded) {
            anchorLoaded = true;
            anchor = Sprite.load("buttons/undo.png");
            if (anchor == null) anchor = Sprite.load("buttons/erase.png");
        }
        return anchor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double d = Math.min(getWidth(), getHeight()) / MARGIN; // circle diameter
        double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
        double scale = (hover ? 1.06 : 1.0) * (pressed ? 0.94 : 1.0);

        Sprite s = states[state];
        Sprite ref = anchor();
        if (s != null && ref != null) {
            double dpr = Math.max(1.0, ((Graphics2D) g).getTransform().getScaleX());
            double base = d / ref.core.width;
            if (pre[state] == null || preK != base || preDpr != dpr) {
                for (int i = 0; i < pre.length; i++) pre[i] = states[i] == null ? null : states[i].prescaledScale(base, dpr);
                preK = base;
                preDpr = dpr;
            }
            double k = base * scale;
            double dx, dy;
            boolean sameCanvas = s.sourceW == ref.sourceW && s.sourceH == ref.sourceH;
            if (sameCanvas) {
                double refCx = ref.offsetX + ref.core.x + ref.core.width / 2.0;
                double refCy = ref.offsetY + ref.core.y + ref.core.height / 2.0;
                dx = cx - (refCx - s.offsetX) * k;
                dy = cy - (refCy - s.offsetY) * k;
            } else { // different canvas sizes, so centre each icon on its own solid part
                dx = cx - (s.core.x + s.core.width / 2.0) * k;
                dy = cy - (s.core.y + s.core.height / 2.0) * k;
            }
            BufferedImage img = pre[state];
            AffineTransform at = new AffineTransform();
            at.translate(dx, dy);
            at.scale(s.image.getWidth() * k / img.getWidth(), s.image.getHeight() * k / img.getHeight());
            g2.drawImage(img, at, null);
        } else {
            paintFallback(g2, cx, cy, d * scale);
        }
        g2.dispose();
    }

    private void paintFallback(Graphics2D g2, double cx, double cy, double d) {
        g2.setColor(new Color(0xF1F3FA));
        g2.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
        g2.setColor(new Color(0x3D5FB5));
        g2.setStroke(new BasicStroke((float) (d * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double r = d * 0.2;
        switch (glyph) {
            case UNDO -> g2.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 60, 270, Arc2D.OPEN));
            case ERASE -> {
                AffineTransform old = g2.getTransform();
                g2.rotate(Math.toRadians(-35), cx, cy);
                g2.draw(new RoundRectangle2D.Double(cx - r, cy - r * 0.55, 2 * r, r * 1.1, 6, 6));
                g2.setTransform(old);
            }
            case PEN -> {
                g2.draw(new Line2D.Double(cx - r, cy + r, cx + r, cy - r));
                g2.draw(new Line2D.Double(cx - r, cy + r, cx - r * 0.4, cy + r));
            }
        }
        if (glyph == Glyph.PEN) {
            g2.setColor(state == 1 ? new Color(0x3D5FB5) : new Color(0xB0B3BA));
            double bw = d * 0.46, bh = d * 0.28;
            g2.fill(new RoundRectangle2D.Double(cx + d * 0.22, cy - d * 0.68, bw, bh, bh, bh));
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.body((float) (bh * 0.6), java.awt.Font.BOLD));
            String t = state == 1 ? "ON" : "OFF";
            g2.drawString(t, (float) (cx + d * 0.22 + (bw - g2.getFontMetrics().stringWidth(t)) / 2),
                    (float) (cy - d * 0.68 + bh * 0.72));
        }
    }
}