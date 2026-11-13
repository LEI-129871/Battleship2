package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

import static battleship.GameTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the printing methods of class Game.
 * Author: 129869
 * Date: 2026-11-07
 * Cyclomatic Complexity (assert statements and lambdas without decisions are not counted):
 * - printBoard(): 5            (2 nested for, if show_shots, if showLegend)
 * - printShips() [private]: 5  (2 nested for, if !stillFloating, for adjacent)
 * - printBoardShots(): 7       (for, for, if isInside, if SHIP, if EMPTY, ||)
 * - printBoardFrame() [private]: 6 (5 for loops with constant bounds)
 * - printMyBoard(), printAlienBoard(), over(): 1 each
 */
class GamePrintTest {

    private Game game;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        game = new Game(new Fleet());
        originalOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream())); // keep the test log quiet
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        game = null;
    }

    // ------------------------------------------------------------ printBoard

    /** Path 1: no shots, no legend. */
    @Test
    void printBoard1() {
        Fleet fleet = new Fleet();
        fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

        String out = captureOut(() -> Game.printBoard(fleet, new ArrayList<>(), false, false));

        assertAll("printBoard without shots or legend",
                () -> assertTrue(out.contains(boardRow(1, ".#........")),
                        "Error: expected the ship drawn on row 1 but got:\n" + out),
                () -> assertTrue(out.contains(boardRow(0, "..........")),
                        "Error: expected an empty row 0 but got:\n" + out),
                () -> assertFalse(out.contains("LEGENDA"), "Error: expected no legend")
        );
    }

    /** Path 2: shots shown, no legend. */
    @Test
    void printBoard2() {
        Fleet fleet = new Fleet();
        fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
        List<IMove> moves = List.of(moveWithShots(volley(new Position(1, 1), new Position(0, 0), new Position(5, 5))));

        String out = captureOut(() -> Game.printBoard(fleet, moves, true, false));

        assertAll("printBoard with shots",
                () -> assertTrue(out.contains(boardRow(0, "o.........")),
                        "Error: expected a water shot on (0,0) but got:\n" + out),
                () -> assertTrue(out.contains(boardRow(1, ".*........")),
                        "Error: expected a ship hit on (1,1) but got:\n" + out),
                () -> assertFalse(out.contains("LEGENDA"), "Error: expected no legend")
        );
    }

    /** Path 3: legend shown, no shots. */
    @Test
    void printBoard3() {
        String out = captureOut(() -> Game.printBoard(new Fleet(), new ArrayList<>(), false, true));

        assertAll("printBoard with legend",
                () -> assertTrue(out.contains("LEGENDA"), "Error: expected the legend title"),
                () -> assertTrue(out.contains("->navio"), "Error: expected the ship legend line"),
                () -> assertTrue(out.contains("->Tiro certeiro"), "Error: expected the shots legend line")
        );
    }

    /** Path 4: shots and legend. */
    @Test
    void printBoard4() {
        Fleet fleet = new Fleet();
        fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
        List<IMove> moves = List.of(moveWithShots(volley(new Position(1, 1), new Position(0, 0), new Position(5, 5))));

        String out = captureOut(() -> Game.printBoard(fleet, moves, true, true));

        assertAll("printBoard with shots and legend",
                () -> assertTrue(out.contains(boardRow(1, ".*........")),
                        "Error: expected a ship hit on (1,1) but got:\n" + out),
                () -> assertTrue(out.contains("LEGENDA"), "Error: expected the legend")
        );
    }

    /** Path 5: empty fleet with a water shot (all cells still drawn). */
    @Test
    void printBoard5() {
        List<IMove> moves = List.of(moveWithShots(volley(new Position(5, 5), new Position(5, 6), new Position(5, 7))));

        String out = captureOut(() -> Game.printBoard(new Fleet(), moves, true, false));

        assertAll("printBoard with an empty fleet",
                () -> assertTrue(out.contains(boardRow(5, ".....ooo..")),
                        "Error: expected three water shots on row 5 but got:\n" + out),
                () -> assertFalse(out.contains("#"), "Error: expected no ship markers")
        );
    }

    @Test
    void printBoardNullFleet() {
        Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
        assertThrows(AssertionError.class, () -> Game.printBoard(null, new ArrayList<>(), false, false),
                "Error: expected AssertionError for a null fleet");
    }

    @Test
    void printBoardNullMoves() {
        Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
        assertThrows(AssertionError.class, () -> Game.printBoard(new Fleet(), null, false, false),
                "Error: expected AssertionError for null moves");
    }

    // ------------------------------------------------- printShips (private)

    private void invokePrintShips(IFleet fleet, char[][] map) throws Exception {
        invokeStatic("printShips", new Class<?>[]{IFleet.class, char[][].class}, fleet, map);
    }

    /** Path 1: empty fleet, nothing drawn. */
    @Test
    void printShips1() throws Exception {
        char[][] map = emptyMap();
        invokePrintShips(new Fleet(), map);

        assertArrayEquals(emptyMap(), map, "Error: expected an untouched map for an empty fleet");
    }

    /** Path 2: one floating ship, only its positions are drawn. */
    @Test
    void printShips2() throws Exception {
        Fleet fleet = new Fleet();
        IShip barge = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(barge);
        char[][] map = emptyMap();

        invokePrintShips(fleet, map);

        assertAll("floating ship",
                () -> assertEquals('#', map[4][4], "Error: expected the ship marker '#' on (4,4)"),
                () -> {
                    for (IPosition adj : barge.getAdjacentPositions())
                        assertEquals('.', map[adj.getRow()][adj.getColumn()],
                                "Error: expected no adjacent marker around a floating ship");
                }
        );
    }

    /** Path 3: one sunk ship, adjacent positions are drawn too. */
    @Test
    void printShips3() throws Exception {
        Fleet fleet = new Fleet();
        IShip barge = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(barge);
        sink(barge);
        char[][] map = emptyMap();

        invokePrintShips(fleet, map);

        assertAll("sunk ship",
                () -> assertEquals('#', map[4][4], "Error: expected the ship marker '#' on (4,4)"),
                () -> {
                    for (IPosition adj : barge.getAdjacentPositions())
                        assertEquals('-', map[adj.getRow()][adj.getColumn()],
                                "Error: expected the adjacent marker '-' around a sunk ship");
                }
        );
    }

    /** Path 4: one floating and one sunk ship together. */
    @Test
    void printShips4() throws Exception {
        Fleet fleet = new Fleet();
        IShip floating = new Barge(Compass.NORTH, new Position(1, 1));
        IShip sunk = new Barge(Compass.NORTH, new Position(6, 6));
        fleet.addShip(floating);
        fleet.addShip(sunk);
        sink(sunk);
        char[][] map = emptyMap();

        invokePrintShips(fleet, map);

        assertAll("mixed fleet",
                () -> assertEquals('#', map[1][1], "Error: expected '#' on the floating ship"),
                () -> assertEquals('#', map[6][6], "Error: expected '#' on the sunk ship"),
                () -> assertEquals('.', map[0][0], "Error: expected no adjacent marker near the floating ship"),
                () -> assertEquals('-', map[5][5], "Error: expected an adjacent marker near the sunk ship")
        );
    }

    /** Path 5: multi-position ship, the positions loop runs several times. */
    @Test
    void printShips5() throws Exception {
        Fleet fleet = new Fleet();
        IShip caravel = new Caravel(Compass.EAST, new Position(5, 5));
        fleet.addShip(caravel);
        char[][] map = emptyMap();

        invokePrintShips(fleet, map);

        assertAll("multi-position ship",
                () -> {
                    for (IPosition p : caravel.getPositions())
                        assertEquals('#', map[p.getRow()][p.getColumn()],
                                "Error: expected '#' on every position of the ship");
                },
                () -> assertEquals(2, caravel.getPositions().size(),
                        "Error: expected a caravel with 2 positions")
        );
    }

    // ---------------------------------------------------------- printBoardShots

    /** Path 1: no moves, map untouched. */
    @Test
    void printBoardShots1() {
        char[][] map = emptyMap();
        Game.printBoardShots(new ArrayList<>(), map);

        assertArrayEquals(emptyMap(), map, "Error: expected an untouched map without moves");
    }

    /** Path 2: shot outside the board is ignored (isInside false). */
    @Test
    void printBoardShots2() {
        char[][] map = emptyMap();
        List<IMove> moves = List.of(moveWithShots(volley(new Position(-1, 5), new Position(-1, 6), new Position(-1, 7))));

        Game.printBoardShots(moves, map);

        assertArrayEquals(emptyMap(), map, "Error: expected an untouched map for outside shots");
    }

    /** Path 3: shot on a ship -> '*'. */
    @Test
    void printBoardShots3() {
        char[][] map = emptyMap();
        map[2][2] = '#';
        List<IMove> moves = List.of(moveWithShots(volley(new Position(2, 2), new Position(-1, 0), new Position(-1, 1))));

        Game.printBoardShots(moves, map);

        assertEquals('*', map[2][2], "Error: expected '*' on a shot ship but got '" + map[2][2] + "'");
    }

    /** Path 4: shot on water -> 'o' (EMPTY_MARKER true). */
    @Test
    void printBoardShots4() {
        char[][] map = emptyMap();
        List<IMove> moves = List.of(moveWithShots(volley(new Position(3, 3), new Position(-1, 0), new Position(-1, 1))));

        Game.printBoardShots(moves, map);

        assertEquals('o', map[3][3], "Error: expected 'o' on a shot water cell but got '" + map[3][3] + "'");
    }

    /** Path 5: shot on an adjacent cell -> 'o' (EMPTY false, ADJACENT true). */
    @Test
    void printBoardShots5() {
        char[][] map = emptyMap();
        map[3][3] = '-';
        List<IMove> moves = List.of(moveWithShots(volley(new Position(3, 3), new Position(-1, 0), new Position(-1, 1))));

        Game.printBoardShots(moves, map);

        assertEquals('o', map[3][3], "Error: expected 'o' on a shot adjacent cell but got '" + map[3][3] + "'");
    }

    /** Path 6: shot on an already-shot cell -> unchanged (both conditions false). */
    @Test
    void printBoardShots6() {
        char[][] map = emptyMap();
        map[1][1] = 'o';
        map[2][2] = '*';
        List<IMove> moves = List.of(moveWithShots(volley(new Position(1, 1), new Position(2, 2), new Position(-1, 1))));

        Game.printBoardShots(moves, map);

        assertAll("already shot cells",
                () -> assertEquals('o', map[1][1], "Error: expected 'o' to stay 'o'"),
                () -> assertEquals('*', map[2][2], "Error: expected '*' to stay '*'")
        );
    }

    /** Path 7: several moves with mixed shots. */
    @Test
    void printBoardShots7() {
        char[][] map = emptyMap();
        map[0][0] = '#';
        map[9][9] = '-';
        List<IMove> moves = List.of(
                moveWithShots(volley(new Position(0, 0), new Position(5, 5), new Position(-1, 0))),
                moveWithShots(volley(new Position(9, 9), new Position(0, 0), new Position(7, 7))));

        Game.printBoardShots(moves, map);

        assertAll("several moves",
                () -> assertEquals('*', map[0][0], "Error: expected '*' on (0,0)"),
                () -> assertEquals('o', map[5][5], "Error: expected 'o' on (5,5)"),
                () -> assertEquals('o', map[9][9], "Error: expected 'o' on (9,9)"),
                () -> assertEquals('o', map[7][7], "Error: expected 'o' on (7,7)")
        );
    }

    // ------------------------------------------------ printBoardFrame (private)

    private String frameOutput(char[][] map) {
        return captureOut(() -> {
            try {
                invokeStatic("printBoardFrame", new Class<?>[]{char[][].class}, (Object) map);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Header line with the column numbers. */
    @Test
    void printBoardFrame1() {
        String out = frameOutput(emptyMap());

        assertTrue(out.contains("     1 2 3 4 5 6 7 8 9 10"),
                "Error: expected the column header but got:\n" + out);
    }

    /** Top border. */
    @Test
    void printBoardFrame2() {
        String out = frameOutput(emptyMap());

        assertTrue(out.contains("   +-" + "--".repeat(Game.BOARD_SIZE) + "+"),
                "Error: expected the top border but got:\n" + out);
    }

    /** Row labels. */
    @Test
    void printBoardFrame3() {
        String out = frameOutput(emptyMap());

        for (int r = 0; r < Game.BOARD_SIZE; r++) {
            final int row = r;
            assertTrue(out.contains(boardRow(row, "..........")),
                    "Error: expected the labelled row " + row + " but got:\n" + out);
        }
    }

    /** Cell contents come from the map. */
    @Test
    void printBoardFrame4() {
        char[][] map = emptyMap();
        map[2][3] = '#';
        map[7][0] = 'o';

        String out = frameOutput(map);

        assertAll("cell contents",
                () -> assertTrue(out.contains(boardRow(2, "...#......")),
                        "Error: expected '#' on (2,3) but got:\n" + out),
                () -> assertTrue(out.contains(boardRow(7, "o.........")),
                        "Error: expected 'o' on (7,0) but got:\n" + out)
        );
    }

    /** Bottom border. */
    @Test
    void printBoardFrame5() {
        String out = frameOutput(emptyMap());

        assertTrue(out.contains("   +" + "--".repeat(Game.BOARD_SIZE) + "-+"),
                "Error: expected the bottom border but got:\n" + out);
    }

    /** Overall number of lines: blank + header + top + rows + bottom. */
    @Test
    void printBoardFrame6() {
        String out = frameOutput(emptyMap());

        assertEquals(Game.BOARD_SIZE + 4, out.split(System.lineSeparator(), -1).length - 1,
                "Error: expected " + (Game.BOARD_SIZE + 4) + " lines but got:\n" + out);
    }

    // -------------------------------------------------------- print wrappers

    @Test
    void printMyBoard() {
        game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(1, 1)));
        game.fireShots(volley(new Position(1, 1), new Position(0, 0), new Position(5, 5)));

        String expected = captureOut(() -> Game.printBoard(game.getMyFleet(), game.getAlienMoves(), true, false));
        String actual = captureOut(() -> game.printMyBoard(true, false));

        assertEquals(expected, actual, "Error: expected printMyBoard to draw my fleet with the alien moves");
    }

    @Test
    void printAlienBoard() {
        String expected = captureOut(() -> Game.printBoard(game.getAlienFleet(), game.getMyMoves(), true, true));
        String actual = captureOut(() -> game.printAlienBoard(true, true));

        assertEquals(expected, actual, "Error: expected printAlienBoard to draw the alien fleet with my moves");
    }

    @Test
    void over() {
        String out = captureOut(() -> game.over());

        assertTrue(out.contains("Maldito sejas, Java Sparrow"),
                "Error: expected the game over message but got:\n" + out);
    }
}