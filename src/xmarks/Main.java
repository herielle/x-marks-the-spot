package xmarks;

import xmarks.ui.AppWindow;

import javax.swing.SwingUtilities;

/** Launches the game window. Swing components must be created on the event dispatch thread. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppWindow().open());
    }
}