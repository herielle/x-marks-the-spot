package xmarks;

import xmarks.model.Board;
import xmarks.model.GameSession;
import xmarks.model.MoveResult;
import xmarks.solver.BitmaskSolver;
import xmarks.storage.SaveManager;
import xmarks.util.GameTimer;

import java.nio.file.Files;
import java.nio.file.Path;

/** Throwaway console check. Replace with the real launcher once the UI exists. */
public class ConsoleCheck {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("xmarks-test");
        SaveManager saves = new SaveManager(dir.resolve("save.txt"));

        // 1. Session basics: entries, mistakes, undo, erase, notes.
        GameSession s = new GameSession(new Board());
        int[] changes = {0};
        s.setOnChange(x -> { changes[0]++; saves.save(x); }); // the wiring the launcher will use
        s.start();

        System.out.println("enter 5 at (0,0): " + s.enter(0, 0, 5).accepted());
        MoveResult bad = s.enter(0, 8, 5);
        System.out.println("enter 5 at (0,8): " + bad.message() + "  mistakes=" + s.mistakes() + "/" + GameSession.MAX_MISTAKES);
        s.enter(4, 4, 7);
        s.undo();
        System.out.println("after enter + undo, (4,4) holds: " + s.board().get(4, 4));
        s.toggleNotesMode();
        s.enter(2, 2, 4);
        s.enter(2, 2, 6);
        System.out.println("notes mask at (2,2): " + s.notesMask(2, 2) + " (expected 40)");
        s.toggleNotesMode();
        System.out.println("change events fired: " + changes[0]);

        // 2. Save file round trip.
        System.out.println("\n--- save file ---");
        System.out.print(Files.readString(dir.resolve("save.txt")));
        GameSession back = saves.load().orElseThrow();
        System.out.println("--- loaded: board identical = " + back.board().toText().equals(s.board().toText())
                + ", mistakes = " + back.mistakes());

        // Damaged file is refused.
        Files.writeString(dir.resolve("save.txt"), "garbage\n");
        System.out.println("damaged file loads: " + saves.load().isPresent());

        // 3. Solver result goes through the session.
        saves.save(s);
        new BitmaskSolver().solve(s.board()).ifPresent(s::applySolution);
        System.out.println("\nstatus after solve: " + s.status() + ", board complete = " + s.board().isComplete());
        System.out.println("save file left after finished game: " + saves.exists());
        System.out.println("entry after the game ended: " + s.enter(1, 1, 1).message());

        // 4. Three rule breaks end the game.
        GameSession g = new GameSession(new Board());
        g.enter(0, 0, 1);
        for (int i = 0; i < 3; i++) g.enter(0, 1, 1);
        System.out.println("\nafter 3 mistakes: " + g.status() + ", mistakes = " + g.mistakes());

        // 5. Timer.
        GameSession t = new GameSession(new Board());
        t.start();
        Thread.sleep(1100);
        t.pause();
        long secs = t.elapsedSeconds();
        System.out.println("timer after ~1.1 s: " + GameTimer.clock(secs) + " / " + GameTimer.words(secs));
    }
}