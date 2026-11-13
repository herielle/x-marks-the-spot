package xmarks.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Night-sky background shared by all screens.
 *
 * With art present it shows resources/screens/background.png and lays shadow.png (the soft
 * spotlight vignette) over it. Without art it paints a gradient, light beams, stars and, on the
 * menu-style screens, rolling green hills. The finished image is cached, so a repaint during an
 * animation costs one drawImage.
 */
public class BackdropPanel extends JPanel {
    private static final int STARS = 30;

    private final boolean hills;
    private final float[] starX = new float[STARS];
    private final float[] starY = new float[STARS];
    private final float[] starSize = new float[STARS];
    private final int[] starAlpha = new int[STARS];

    private BufferedImage composed;
    private int composedW, composedH;
    private double composedDpr;

    public BackdropPanel(boolean hills) {
        this.hills = hills;
        setOpaque(true);
        Random rnd = new Random(11); // fixed seed keeps the sky identical on every launch
        for (int i = 0; i < STARS; i++) {
            starX[i] = rnd.nextFloat();
            starY[i] = rnd.nextFloat() * 0.72f;
            starSize[i] = 0.008f + rnd.nextFloat() * 0.026f;
            starAlpha[i] = 110 + rnd.nextInt(120);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        BufferedImage art = Theme.image(hills ? "screens/background.png" : "screens/gameplay_bg.png");
        if (art != null) {
            double dpr = Math.max(1.0, ((Graphics2D) g).getTransform().getScaleX());
            if (composed == null || composedW != w || composedH != h || composedDpr != dpr) {
                composed = compose(art, w, h, dpr);
                composedW = w;
                composedH = h;
                composedDpr = dpr;
            }
            g.drawImage(composed, 0, 0, w, h, null);
            return;
        }

        Graphics2D g2 = Theme.smooth(g);
        paintFallback(g2, w, h);
        g2.dispose();
    }

    /** Background scaled to cover the panel, with the shadow overlay on top. */
    private BufferedImage compose(BufferedImage art, int w, int h, double dpr) {
        int pw = (int) Math.ceil(w * dpr), ph = (int) Math.ceil(h * dpr);
        BufferedImage out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        drawCover(g, art, pw, ph);
        BufferedImage shadow = hills ? Theme.image("screens/shadow.png") : null;
        if (shadow != null) drawCover(g, shadow, pw, ph);
        g.dispose();
        return out;
    }

    private static void drawCover(Graphics2D g, BufferedImage img, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) Math.round(img.getWidth() * scale), dh = (int) Math.round(img.getHeight() * scale);
        g.drawImage(Theme.scale(img, dw, dh), (w - dw) / 2, (h - dh) / 2, null);
    }

    private void paintFallback(Graphics2D g2, int w, int h) {
        g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[] {0f, 0.5f, 1f},
                new Color[] {Theme.NAVY, Theme.TEAL, Theme.CYAN}));
        g2.fillRect(0, 0, w, h);

        beam(g2, w * 0.30f, w, h);
        beam(g2, w * 0.72f, w, h);

        for (int i = 0; i < STARS; i++) {
            Color c = Theme.STAR;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), starAlpha[i]));
            star(g2, starX[i] * w, starY[i] * h, starSize[i] * h);
        }

        if (hills) {
            paintHills(g2, w, h);
        } else {
            g2.setPaint(new RadialGradientPaint(w / 2f, h, w * 0.55f, new float[] {0f, 1f},
                    new Color[] {new Color(120, 255, 240, 110), new Color(120, 255, 240, 0)}));
            g2.fillRect(0, 0, w, h);
        }
    }

    private static void beam(Graphics2D g2, float cx, int w, int h) {
        Path2D beam = new Path2D.Float();
        beam.moveTo(cx - w * 0.04f, 0);
        beam.lineTo(cx + w * 0.04f, 0);
        beam.lineTo(cx + w * 0.17f, h);
        beam.lineTo(cx - w * 0.17f, h);
        beam.closePath();
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 44), 0, h * 0.9f, new Color(255, 255, 255, 0)));
        g2.fill(beam);
    }

    /** Four-pointed sparkle centered on (x, y). */
    private static void star(Graphics2D g2, float x, float y, float r) {
        Path2D s = new Path2D.Float();
        s.moveTo(x, y - r);
        s.quadTo(x, y, x + r, y);
        s.quadTo(x, y, x, y + r);
        s.quadTo(x, y, x - r, y);
        s.quadTo(x, y, x, y - r);
        s.closePath();
        g2.fill(s);
    }

    private static void paintHills(Graphics2D g2, int w, int h) {
        Path2D left = new Path2D.Float();
        left.moveTo(0, h * 0.80f);
        left.curveTo(w * 0.18f, h * 0.66f, w * 0.40f, h * 0.78f, w * 0.58f, h);
        left.lineTo(0, h);
        left.closePath();
        g2.setPaint(new GradientPaint(0, h * 0.66f, Theme.HILL_TOP, 0, h, Theme.HILL_BOTTOM));
        g2.fill(left);

        Path2D right = new Path2D.Float();
        right.moveTo(w * 0.36f, h);
        right.curveTo(w * 0.55f, h * 0.80f, w * 0.80f, h * 0.68f, w, h * 0.74f);
        right.lineTo(w, h);
        right.closePath();
        g2.setPaint(new GradientPaint(0, h * 0.68f, Theme.HILL_TOP, 0, h, Theme.HILL_BOTTOM));
        g2.fill(right);
    }
}