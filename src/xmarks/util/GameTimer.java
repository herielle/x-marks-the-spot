package xmarks.util;

/**
 * Stopwatch that counts elapsed play time. It holds no Swing code, so the model can use it
 * and a console test can check it. The game screen polls elapsedSeconds() from its own
 * javax.swing.Timer to refresh the clock label.
 */
public final class GameTimer {
    private long accumulatedNanos = 0;
    private long startedAt = 0;
    private boolean running = false;

    public void start() {
        if (running) return;
        startedAt = System.nanoTime();
        running = true;
    }

    public void pause() {
        if (!running) return;
        accumulatedNanos += System.nanoTime() - startedAt;
        running = false;
    }

    public void reset() {
        accumulatedNanos = 0;
        running = false;
    }

    /** Restores a saved time. Leaves the timer paused. */
    public void setElapsedSeconds(long seconds) {
        accumulatedNanos = Math.max(0, seconds) * 1_000_000_000L;
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long elapsedSeconds() {
        long nanos = accumulatedNanos + (running ? System.nanoTime() - startedAt : 0);
        return nanos / 1_000_000_000L;
    }

    /** Game screen format, such as 00:24. */
    public static String clock(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Result screen format, such as 02m 05s. */
    public static String words(long seconds) {
        return String.format("%02dm %02ds", seconds / 60, seconds % 60);
    }
}
