package xmarks.ui;

import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * The opening scene and the main menu in one panel, so the loading screen flows into the menu.
 *
 * Timeline, in seconds:
 *   0.0 to 0.7   the title fades in alone, centered on the night sky
 *   1.1 to 2.1   the title glides up and shrinks into its menu position
 *   1.6 onward   the four buttons float up one after another
 *
 * Everything is drawn from a single clock, so the motion is smooth at any frame rate. Clicks
 * and key presses are ignored until the intro has finished. To retime or resize anything,
 * change the constants below.
 */
public class MainMenuPanel extends BackdropPanel {
    // ----- timeline -----
    private static final double FADE_IN = 0.7;
    private static final double RISE_START = 1.1;
    private static final double RISE_DURATION = 1.0;
    private static final double BUTTONS_START = 1.6;
    private static final double BUTTON_STAGGER = 0.12;
    private static final double BUTTON_DURATION = 0.6;

    // ----- layout, as fractions of the window height unless noted -----
    private static final double SPLASH_MAX_WIDTH = 0.62;  // of window width
    private static final double SPLASH_MAX_HEIGHT = 0.46; // of window height
    private static final double MENU_TITLE_HEIGHT = 0.26;
    private static final double MENU_TOP = 0.10;
    private static final double TITLE_BUTTON_GAP = 0.045;
    private static final double BUTTON_HEIGHT = 0.078;
    private static final double BUTTON_GAP = 0.018;
    private static final double BUTTON_FLOAT = 0.05;      // how far a button travels while arriving
    private static final double BUTTON_OVERSAMPLE = 1.12; // buttons grow up to 1.05, so prescale a little larger

    private static final String[] LABELS = {"PLAY", "HOW TO PLAY", "CREDITS", "EXIT"};
    private static final String[] FILES = {
        "buttons/play_button.png", "buttons/howtoplay_button.png",
        "buttons/credits_button.png", "buttons/exit_button.png"
    };
    private static final int N = LABELS.length;
    private static final double END = BUTTONS_START + (N - 1) * BUTTON_STAGGER + BUTTON_DURATION;

    private final Runnable[] actions;
    private final Sprite titleSprite = Sprite.load("title.png");
    private final Sprite[] buttonSprites = new Sprite[N];
    private final Timer timer = new Timer(15, e -> tick());
    private final double[] hover = new double[N];
    private final double[] press = new double[N];

    private boolean started;
    private long startNanos;
    private double clock; // seconds since the intro began, held at END once it finishes
    private int hoverIndex = -1;
    private int focusIndex = -1;
    private int pressedIndex = -1;

    // layout results and prescaled images, rebuilt when the panel size or screen scale changes
    private int cacheW, cacheH;
    private double cacheDpr;
    private BufferedImage titlePre;
    private final BufferedImage[] buttonPre = new BufferedImage[N];
    private double splashW, menuW, splashCy, menuCy, buttonH, buttonGap, buttonTop;
    private final double[] buttonW = new double[N];

    public MainMenuPanel(Runnable play, Runnable howToPlay, Runnable credits, Runnable exit) {
        super(true);
        actions = new Runnable[] {play, howToPlay, credits, exit};
        for (int i = 0; i < N; i++) buttonSprites[i] = Sprite.load(FILES[i]);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // lets Tab reach the key listener
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) requestFocusInWindow();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(hitTest(e.getX(), e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (clock < END) return; // the intro always plays through
                pressedIndex = hitTest(e.getX(), e.getY());
                ensureTicking();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int released = hitTest(e.getX(), e.getY());
                int pressed = pressedIndex;
                pressedIndex = -1;
                ensureTicking();
                if (released >= 0 && released == pressed) actions[released].run();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (clock < END) return; // the intro always plays through
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN, KeyEvent.VK_TAB -> moveFocus(1);
                    case KeyEvent.VK_UP -> moveFocus(-1);
                    case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                        if (focusIndex >= 0) actions[focusIndex].run();
                    }
                    default -> { }
                }
            }
        });
    }

    // ----- clock -----

    /** Starts the intro from the beginning. Call once the window is visible. */
    public void playIntro() {
        started = true;
        clock = 0;
        startNanos = System.nanoTime();
        timer.start();
    }

    /** Freezes the clock at the given second. Used to capture screenshots of single frames. */
    public void seek(double seconds) {
        started = true;
        timer.stop();
        clock = Math.max(0, Math.min(END, seconds));
        startNanos = System.nanoTime() - (long) (seconds * 1e9);
        repaint();
    }

    private void tick() {
        if (started && clock < END) {
            clock = Math.min(END, (System.nanoTime() - startNanos) / 1e9);
        }
        boolean settling = false;
        for (int i = 0; i < N; i++) {
            boolean lit = i == hoverIndex || i == focusIndex;
            hover[i] = approach(hover[i], lit ? 1 : 0);
            press[i] = approach(press[i], i == pressedIndex ? 1 : 0);
            if (hover[i] != (lit ? 1 : 0) || press[i] != (i == pressedIndex ? 1 : 0)) settling = true;
        }
        repaint();
        if (clock >= END && !settling) timer.stop();
    }

    /** Moves a value toward its target a fraction of the way each frame, snapping when close. */
    private static double approach(double value, double target) {
        double next = value + (target - value) * 0.28;
        return Math.abs(target - next) < 0.01 ? target : next;
    }

    private void ensureTicking() {
        if (!timer.isRunning()) timer.start();
    }

    private void setHover(int index) {
        if (index == hoverIndex) return;
        hoverIndex = index;
        setCursor(index >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        ensureTicking();
    }

    private void moveFocus(int step) {
        focusIndex = focusIndex < 0 ? (step > 0 ? 0 : N - 1) : (focusIndex + step + N) % N;
        ensureTicking();
    }

    // ----- layout -----

    private void layout(int w, int h, double dpr) {
        double titleAspect = titleSprite != null ? titleSprite.coreAspect() : FallbackArt.TITLE_ASPECT;

        splashW = Math.min(SPLASH_MAX_WIDTH * w, SPLASH_MAX_HEIGHT * h * titleAspect);
        menuW = Math.min(MENU_TITLE_HEIGHT * h * titleAspect, 0.5 * w);
        double menuH = menuW / titleAspect;
        splashCy = h * 0.5;
        menuCy = h * MENU_TOP + menuH / 2;

        buttonH = BUTTON_HEIGHT * h;
        buttonGap = BUTTON_GAP * h;
        buttonTop = h * MENU_TOP + menuH + TITLE_BUTTON_GAP * h;
        for (int i = 0; i < N; i++) {
            double aspect = buttonSprites[i] != null ? buttonSprites[i].coreAspect() : 3.9;
            buttonW[i] = buttonH * aspect;
        }

        if (w != cacheW || h != cacheH || dpr != cacheDpr) {
            cacheW = w;
            cacheH = h;
            cacheDpr = dpr;
            titlePre = titleSprite == null ? null : titleSprite.prescaled(splashW, dpr);
            for (int i = 0; i < N; i++) {
                buttonPre[i] = buttonSprites[i] == null ? null
                        : buttonSprites[i].prescaled(buttonW[i] * BUTTON_OVERSAMPLE, dpr);
            }
        }
    }

    private double buttonProgress(int i) {
        return easeOut(clamp01((clock - (BUTTONS_START + i * BUTTON_STAGGER)) / BUTTON_DURATION));
    }

    private int hitTest(int x, int y) {
        double left0 = getWidth() / 2.0;
        for (int i = 0; i < N; i++) {
            if (buttonProgress(i) < 0.85) continue;
            double left = left0 - buttonW[i] / 2;
            double top = buttonTop + i * (buttonH + buttonGap);
            if (x >= left && x <= left + buttonW[i] && y >= top && y <= top + buttonH) return i;
        }
        return -1;
    }

    // ----- painting -----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // the night sky
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        Graphics2D g2 = Theme.smooth(g);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        layout(w, h, Math.max(1.0, ((Graphics2D) g).getTransform().getScaleX()));

        double fade = easeOut(clamp01(clock / FADE_IN));
        double rise = easeInOut(clamp01((clock - RISE_START) / RISE_DURATION));
        double titleWidth = lerp(splashW, menuW, rise) * (0.95 + 0.05 * fade); // slight swell as it arrives
        drawTitle(g2, w / 2.0, lerp(splashCy, menuCy, rise), titleWidth, fade);

        for (int i = 0; i < N; i++) {
            double p = buttonProgress(i);
            if (p <= 0) continue;
            double scale = (0.94 + 0.06 * p) * (1 + 0.05 * hover[i]) * (1 - 0.04 * press[i]);
            double cy = buttonTop + i * (buttonH + buttonGap) + buttonH / 2 + (1 - p) * BUTTON_FLOAT * h;
            drawButton(g2, i, w / 2.0, cy, buttonW[i] * scale, buttonH * scale, p, hover[i]);
        }
        g2.dispose();
    }

    private void drawTitle(Graphics2D g2, double cx, double cy, double coreW, double alpha) {
        if (alpha <= 0.001) return;
        Graphics2D c = (Graphics2D) g2.create();
        c.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        if (titleSprite != null) {
            drawSprite(c, titleSprite, titlePre, cx, cy, coreW);
        } else {
            double th = coreW / FallbackArt.TITLE_ASPECT;
            c.translate(cx - coreW / 2, cy - th / 2);
            FallbackArt.paintTitle(c, (int) Math.round(coreW), (int) Math.round(th));
        }
        c.dispose();
    }

    private void drawButton(Graphics2D g2, int i, double cx, double cy, double w, double h, double alpha, double lit) {
        Graphics2D c = (Graphics2D) g2.create();
        c.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        if (buttonSprites[i] != null) {
            drawSprite(c, buttonSprites[i], buttonPre[i], cx, cy, w);
        } else {
            c.translate(cx - w / 2, cy - h / 2);
            GlassButton.paintPill(c, LABELS[i], Theme.body((float) (h * 0.34), java.awt.Font.PLAIN), Theme.TEXT,
                    (int) Math.round(w), (int) Math.round(h), (int) (40 * lit));
        }
        c.dispose();
    }

    /** Draws a prescaled sprite so the center of its solid core lands on (cx, cy) with the given core width. */
    private static void drawSprite(Graphics2D g2, Sprite s, BufferedImage pre, double cx, double cy, double coreW) {
        double k = coreW / s.core.width; // logical pixels per source pixel
        double dx = cx - (s.core.x + s.core.width / 2.0) * k;
        double dy = cy - (s.core.y + s.core.height / 2.0) * k;
        AffineTransform at = new AffineTransform();
        at.translate(dx, dy);
        at.scale(s.image.getWidth() * k / pre.getWidth(), s.image.getHeight() * k / pre.getHeight());
        g2.drawImage(pre, at, null);
    }

    // ----- easing -----

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Fast start, soft landing. */
    private static double easeOut(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    /** Soft start and soft landing. */
    private static double easeInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
}