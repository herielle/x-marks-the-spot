package xmarks.storage;

import xmarks.model.Board;
import xmarks.model.GameSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes the latest game as plain text. The file lives in the user's home folder,
 * because the folder an installed .exe sits in is often read-only on Windows.
 *
 * File layout (open it in any text editor):
 *   # comment line
 *   nine lines of nine digits   the board, 0 for empty
 *   nine lines of nine digits   the given mask, 1 for a locked cell
 *   mistakes=1
 *   seconds=24
 *
 * Every method swallows IO problems and reports them through its return value, so a full
 * disk or a damaged file cannot crash the game.
 */
public final class SaveManager {
    private final Path file;

    public SaveManager() {
        this(Path.of(System.getProperty("user.home"), ".xmarks", "save.txt"));
    }

    public SaveManager(Path file) {
        this.file = file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /**
     * Writes the session while a game is in progress. A finished game deletes the file
     * instead, so the next Play starts clean. Returns true when the file is up to date.
     */
    public boolean save(GameSession session) {
        if (session.status() != GameSession.Status.PLAYING) {
            return clear();
        }
        Board board = session.board();
        StringBuilder sb = new StringBuilder("# X Marks The Spot save file\n");
        sb.append(board.toText());
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) sb.append(board.isGiven(r, c) ? '1' : '0');
            sb.append('\n');
        }
        sb.append("mistakes=").append(session.mistakes()).append('\n');
        sb.append("seconds=").append(session.elapsedSeconds()).append('\n');

        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            // Write to a temporary file first so a crash mid-write cannot damage the old save.
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Returns the saved game, or empty when there is no file or its content is damaged. */
    public Optional<GameSession> load() {
        if (!exists()) return Optional.empty();
        try {
            List<String> grids = new ArrayList<>();
            int mistakes = 0;
            long seconds = 0;

            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.matches("[0-9]{9}")) grids.add(line);
                else if (line.startsWith("mistakes=")) mistakes = Integer.parseInt(line.substring(9).trim());
                else if (line.startsWith("seconds=")) seconds = Long.parseLong(line.substring(8).trim());
            }
            if (grids.size() != 9 && grids.size() != 18) return Optional.empty();

            Board board = Board.parse(String.join("", grids.subList(0, 9)));
            if (board == null) return Optional.empty();

            if (grids.size() == 18) {
                for (int r = 0; r < Board.SIZE; r++) {
                    for (int c = 0; c < Board.SIZE; c++) {
                        if (grids.get(9 + r).charAt(c) == '1') board.lock(r, c);
                    }
                }
            }
            // A hand-edited file could hold a board that already breaks the rules.
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    int d = board.get(r, c);
                    if (d != 0 && board.findConflict(r, c, d) != null) return Optional.empty();
                }
            }
            return Optional.of(GameSession.restore(board, mistakes, seconds));
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean clear() {
        try {
            Files.deleteIfExists(file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
