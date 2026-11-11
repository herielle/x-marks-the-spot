package xmarks.ui;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Colors, fonts and image loading shared by every screen.
 *
 * Art lives in the resources folder, which sits on the classpath, so paths are relative to it:
 *   resources/title.png
 *   resources/screens/background.png, shadow.png, game_background.png
 *   resources/buttons/play_button.png, howtoplay_button.png, credits_button.png, exit_button.png
 *   resources/fonts/title.ttf, body.ttf
 * Anything missing falls back to a font installed on the machine or to painting in code.
 */
public final class Theme {
    private Theme() {}

    public static final Color NAVY = new Color(0x08161F);
    public static final Color TEAL = new Color(0x1F6670);
    public static final Color CYAN = new Color(0x3FD9D0);
    public static final Color LOGO = new Color(0xF7D793);
    public static final Color LOGO_SHADOW = new Color(0x6F9068);
    public static final Color TEXT = new Color(0xF4ECC8);
    public static final Color STAR = new Color(0xE8C9A0);
    public static final Color HILL_TOP = new Color(0xC9D9A8);
    public static final Color HILL_BOTTOM = new Color(0x4A735F);

    private static final Map<String, Font> fonts = new HashMap<>();
    private static final Map<String, BufferedImage> images = new HashMap<>();
    private static Set<String> families;

    /** Chunky display font for titles. */
    public static Font title(float size) {
        Font custom = loadFont("/fonts/title.ttf");
        if (custom != null) return custom.deriveFont(size);
        String family = pick(Font.SANS_SERIF, "Arial Rounded MT Bold", "Segoe UI Black", "Trebuchet MS", "Verdana");
        return new Font(family, Font.BOLD, 12).deriveFont(size);
    }

    /** Clean sans font for buttons and text. */
    public static Font body(float size, int style) {
        Font custom = loadFont("/fonts/body.ttf");
        if (custom != null) return custom.deriveFont(style, size);
        String family = pick(Font.SANS_SERIF, "Montserrat", "Segoe UI", "Helvetica Neue", "Arial");
        return new Font(family, style, 12).deriveFont(size);
    }

    /** Loads an image such as "screens/background.png" from the resources root, or null when it is not there. */
    public static BufferedImage image(String name) {
        String path = "/" + name;
        if (images.containsKey(path)) return images.get(path);
        BufferedImage img = null;
        try (InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in != null) img = ImageIO.read(in);
        } catch (Exception ignored) {
            // a damaged image behaves like a missing one
        }
        images.put(path, img);
        return img;
    }

    /**
     * Resizes with repeated halving followed by one bicubic step. A single big jump from a
     * 1920-pixel image to a few hundred pixels looks jagged, and this does not.
     */
    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = resample(cur, cw, ch);
        }
        return (cw == w && ch == h) ? cur : resample(cur, w, h);
    }

    private static BufferedImage resample(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Copy of g with antialiasing on. The caller disposes it. */
    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    private static Font loadFont(String path) {
        if (fonts.containsKey(path)) return fonts.get(path);
        Font font = null;
        try (InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in != null) font = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception ignored) {
            // fall back to an installed font
        }
        fonts.put(path, font);
        return font;
    }

    private static String pick(String fallback, String... wanted) {
        if (families == null) {
            families = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        }
        for (String name : wanted) {
            if (families.contains(name)) return name;
        }
        return fallback;
    }
}