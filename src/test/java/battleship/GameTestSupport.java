package battleship;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared static helpers for the Game test classes (GameTest and GamePrintTest).
 * Author: 129869
 */
final class GameTestSupport {

    private GameTestSupport() {
    }

    static boolean assertionsEnabled() {
        boolean enabled = false;
        assert enabled = true; // intentional side effect: only runs with -ea
        return enabled;
    }

    static String captureOut(Runnable action) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(previous);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    static void sink(IShip ship) {
        for (IPosition p : ship.getPositions()) {
            p.shoot();
        }
    }

    static char[][] emptyMap() {
        char[][] map = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
        for (char[] row : map) Arrays.fill(row, '.');
        return map;
    }

    static IMove moveWithShots(List<IPosition> shots) {
        IMove move = mock(IMove.class);
        when(move.getShots()).thenReturn(shots);
        return move;
    }

    static List<IPosition> allPositions() {
        List<IPosition> all = new ArrayList<>();
        for (int r = 0; r < Game.BOARD_SIZE; r++)
            for (int c = 0; c < Game.BOARD_SIZE; c++)
                all.add(new Position(r, c));
        return all;
    }

    static List<IPosition> volley(IPosition a, IPosition b, IPosition c) {
        return new ArrayList<>(List.of(a, b, c));
    }

    static Set<Integer> keys(Collection<? extends IPosition> positions) {
        Set<Integer> keys = new HashSet<>();
        for (IPosition p : positions) keys.add(p.getRow() * Game.BOARD_SIZE + p.getColumn());
        return keys;
    }

    static String boardRow(int row, String cells) {
        StringBuilder sb = new StringBuilder(" " + new Position(row, 0).getClassicRow() + " |");
        for (char ch : cells.toCharArray()) sb.append(' ').append(ch);
        return sb.append(" |").toString();
    }

    static Object invokeStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = Game.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}