package xmarks.ui;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * An image with transparent margins removed, plus the location of its solid part.
 *
 * Exported art such as title.png is a full 1920×1080 canvas with the card somewhere in the
 * middle. Positioning by the solid "core" (pixels that are mostly opaque) puts the card
 * exactly where the code says, and the faint shadow around it still gets drawn.
 */
final class Sprite {
    private static final Map<String, Sprite> cache = new HashMap<>();

    final BufferedImage image; // cropped to the faintest visible pixels
    final Rectangle core;      // the solid part, in image coordinates
    final int offsetX;         // where the crop started inside the original file
    final int offsetY;
    final int sourceW;         // size of the original file
    final int sourceH;

    private Sprite(BufferedImage image, Rectangle core, int offsetX, int offsetY, int sourceW, int sourceH) {
        this.image = image;
        this.core = core;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.sourceW = sourceW;
        this.sourceH = sourceH;
    }

    /** Loads a sprite from the resources root, or returns null when the file is missing. */
    static Sprite load(String path) {
        if (cache.containsKey(path)) return cache.get(path);
        BufferedImage src = Theme.image(path);
        Sprite sprite = src == null ? null : from(src);
        cache.put(path, sprite);
        return sprite;
    }

    double coreAspect() {
        return core.width / (double) core.height;
    }

    /** Image resized so its core measures coreWidth logical pixels, at dpr device pixels per logical pixel. */
    BufferedImage prescaled(double coreWidth, double dpr) {
        return prescaledScale(coreWidth / core.width, dpr);
    }

    /** Image resized by k logical pixels per source pixel, at dpr device pixels per logical pixel. */
    BufferedImage prescaledScale(double k, double dpr) {
        int w = Math.max(1, (int) Math.round(image.getWidth() * k * dpr));
        int h = Math.max(1, (int) Math.round(image.getHeight() * k * dpr));
        return Theme.scale(image, w, h);
    }

    private static Sprite from(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        int[] soft = bounds(px, w, h, 8);
        if (soft == null) return null; // fully transparent
        int[] solid = bounds(px, w, h, 128);
        if (solid == null) solid = soft;

        int sw = soft[2] - soft[0] + 1, sh = soft[3] - soft[1] + 1;
        BufferedImage cropped = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        cropped.setRGB(0, 0, sw, sh, px, soft[1] * w + soft[0], w);
        Rectangle core = new Rectangle(solid[0] - soft[0], solid[1] - soft[1],
                solid[2] - solid[0] + 1, solid[3] - solid[1] + 1);
        return new Sprite(cropped, core, soft[0], soft[1], w, h);
    }

    /** Smallest box {minX, minY, maxX, maxY} holding every pixel with alpha above the threshold. */
    private static int[] bounds(int[] px, int w, int h, int threshold) {
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) > threshold) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return maxX < 0 ? null : new int[] {minX, minY, maxX, maxY};
    }
}