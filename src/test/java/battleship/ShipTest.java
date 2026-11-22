package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class for Ship.
 * Author: ${user.name}
 * Date: ${current_date}
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - getCategory: 1
 * - getSize: 1
 * - getBearing: 1
 * - getPositions: 1
 * - stillFloating: 2
 * - shoot: 2
 * - occupies: 2
 * - tooCloseTo (IShip): 2
 * - tooCloseTo (IPosition): 2
 * - getTopMostPos: 2
 * - getBottomMostPos: 2
 * - getLeftMostPos: 2
 * - getRightMostPos: 2
 */
public class ShipTest {

    private Ship ship, shipBorder;

    @BeforeEach
    void setUp() {
        // Since Ship is abstract, instantiate it with a concrete subclass (e.g., Barge)
        ship = new Barge(Compass.NORTH, new Position(5, 5));
        shipBorder = new Barge(Compass.NORTH, new Position(0, 0));
    }

    @AfterEach
    void tearDown() {
        ship = null;
    }

    /**
     * Test for the constructor.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testConstructor() {
        assertNotNull(ship, "Error: Ship instance should not be null.");
        assertEquals("Barca", ship.getCategory(), "Error: Ship category is incorrect.");
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing is incorrect.");
        assertEquals(1, ship.getSize(), "Error: Ship size is incorrect.");
        assertFalse(ship.getPositions().isEmpty(), "Error: Ship positions should not be empty.");
    }

    /**
     * Test for the getCategory method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetCategory() {
        assertEquals("Barca", ship.getCategory(), "Error: Ship category should be 'Barca'.");
    }

    /**
     * Test for the getSize method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetSize() {
        assertEquals(1, ship.getSize(), "Error: Ship size should be 1.");
    }

    /**
     * Test for the getBearing method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetBearing() {
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing should be NORTH.");
    }

    /**
     * Test for the getPositions method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetPositions() {
        List<IPosition> positions = ship.getPositions();
        assertNotNull(positions, "Error: Ship positions should not be null.");
        assertEquals(1, positions.size(), "Error: Ship should have exactly one position.");
        assertEquals(5, positions.get(0).getRow(), "Error: Position's row should be 5.");
        assertEquals(5, positions.get(0).getColumn(), "Error: Position's column should be 5.");
    }

    /**
     * Test for the stillFloating method (all positions intact).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testStillFloating1() {
        assertTrue(ship.stillFloating(), "Error: Ship should still be floating.");
    }

    /**
     * Test for the stillFloating method (all positions hit).
     */
    @Test
    void testStillFloating2() {
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "Error: Ship should no longer be floating after being hit.");
    }

    /**
     * Test for the shoot method (valid position).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testShoot1() {
        Position target = new Position(5, 5);
        ship.shoot(target);
        assertTrue(ship.getPositions().get(0).isHit(), "Error: Position should be marked as hit.");
    }

    /**
     * Test for the shoot method (invalid position).
     */
    @Test
    void testShoot2() {
        Position target = new Position(0, 0);
        ship.shoot(target); // No exception expected
        assertFalse(ship.getPositions().get(0).isHit(), "Error: Position should not be marked as hit for an invalid target.");
    }

    /**
     * Test for the occupies method (position occupied).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testOccupies1() {
        Position pos = new Position(5, 5);
        assertTrue(ship.occupies(pos), "Error: Ship should occupy position (5, 5).");
    }

    /**
     * Test for the occupies method (position not occupied).
     */
    @Test
    void testOccupies2() {
        Position pos = new Position(1, 1);
        assertFalse(ship.occupies(pos), "Error: Ship should not occupy position (1, 1).");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships too close).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testTooCloseToShip1() {
        Ship nearbyShip = new Barge(Compass.NORTH, new Position(5, 6));
        assertTrue(ship.tooCloseTo(nearbyShip), "Error: Ships should be too close.");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships not close).
     */
    @Test
    void testTooCloseToShip2() {
        Ship farShip = new Barge(Compass.NORTH, new Position(10, 10));
        assertFalse(ship.tooCloseTo(farShip), "Error: Ships should not be too close.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions adjacent).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testTooCloseToPosition1() {
        Position pos = new Position(5, 6); // Adjacent position
        assertTrue(ship.tooCloseTo(pos), "Error: Ship should be too close to the given position.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions not adjacent).
     */
    @Test
    void testTooCloseToPosition2() {
        Position pos = new Position(7, 7); // Non-adjacent position
        assertFalse(ship.tooCloseTo(pos), "Error: Ship should not be too close to the given position.");
    }

    /**
     * Test for the getTopMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetTopMostPos() {
        assertEquals(5, ship.getTopMostPos(), "Error: The topmost position should be 5.");
    }

    /**
     * Test for the getBottomMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetBottomMostPos() {
        assertEquals(5, ship.getBottomMostPos(), "Error: The bottommost position should be 5.");
    }

    /**
     * Test for the getLeftMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetLeftMostPos() {
        assertEquals(5, ship.getLeftMostPos(), "Error: The leftmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetRightMostPos() {
        assertEquals(5, ship.getRightMostPos(), "Error: The rightmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetAdjacentPositions() {
        assertEquals(8, ship.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
        assertEquals(3, shipBorder.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
    }

    @Test
    @DisplayName("buildShip cria o tipo de navio certo para cada categoria e null para uma desconhecida")
    void testBuildShip() {
        Position p = new Position(5, 5);
        assertInstanceOf(Barge.class, Ship.buildShip("barca", Compass.NORTH, p));
        assertInstanceOf(Caravel.class, Ship.buildShip("caravela", Compass.NORTH, p));
        assertInstanceOf(Carrack.class, Ship.buildShip("nau", Compass.NORTH, p));
        assertInstanceOf(Frigate.class, Ship.buildShip("fragata", Compass.NORTH, p));
        assertInstanceOf(Galleon.class, Ship.buildShip("galeao", Compass.NORTH, p));
        assertNull(Ship.buildShip("desconhecido", Compass.NORTH, p), "Uma categoria desconhecida deve devolver null.");
    }

    @Test
    @DisplayName("Os métodos com asserções falham com argumentos inválidos")
    void testBuildShipNulos() {
        Position p = new Position(5, 5);
        assertThrows(AssertionError.class, () -> Ship.buildShip(null, Compass.NORTH, p));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", null, p));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", Compass.NORTH, null));
        assertThrows(AssertionError.class, () -> ship.occupies(null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null));
        assertThrows(AssertionError.class, () -> ship.shoot(null));
        assertThrows(AssertionError.class, () -> ship.shoot(new Position(Game.BOARD_SIZE, Game.BOARD_SIZE)));
    }

    @Test
    @DisplayName("getPosition devolve a posição inicial do navio")
    void testGetPosition() {
        assertEquals(5, ship.getPosition().getRow(), "Erro: a linha da posição inicial devia ser 5.");
        assertEquals(5, ship.getPosition().getColumn(), "Erro: a coluna da posição inicial devia ser 5.");
    }

    @Test
    @DisplayName("Posições extremas num navio com várias posições desordenadas")
    void testMostPosVariasPosicoes() {
        Ship multi = new Frigate(Compass.NORTH, new Position(5, 5));
        multi.getPositions().clear();
        multi.getPositions().addAll(List.of(
                new Position(5, 5), new Position(3, 2), new Position(6, 8), new Position(4, 4)));
        assertEquals(3, multi.getTopMostPos(), "Erro: a linha mais acima devia ser 3.");
        assertEquals(6, multi.getBottomMostPos(), "Erro: a linha mais abaixo devia ser 6.");
        assertEquals(2, multi.getLeftMostPos(), "Erro: a coluna mais à esquerda devia ser 2.");
        assertEquals(8, multi.getRightMostPos(), "Erro: a coluna mais à direita devia ser 8.");
    }

    @Test
    @DisplayName("isInsideBoard é true dentro do tabuleiro e false fora de cada um dos 4 limites")
    void testIsInsideBoard() {
        assertTrue(ship.isInsideBoard());
        assertTrue(shipBorder.isInsideBoard());
        assertFalse(new Barge(Compass.NORTH, new Position(5, -1)).isInsideBoard(), "Fora à esquerda.");
        assertFalse(new Barge(Compass.NORTH, new Position(5, Game.BOARD_SIZE)).isInsideBoard(), "Fora à direita.");
        assertFalse(new Barge(Compass.NORTH, new Position(-1, 5)).isInsideBoard(), "Fora em cima.");
        assertFalse(new Barge(Compass.NORTH, new Position(Game.BOARD_SIZE, 5)).isInsideBoard(), "Fora em baixo.");
    }

    @Test
    @DisplayName("sink atinge todas as posições do navio")
    void testSink() {
        Ship frigate = new Frigate(Compass.NORTH, new Position(5, 5));
        assertTrue(frigate.stillFloating());
        frigate.sink();
        assertFalse(frigate.stillFloating(), "Erro: o navio não devia flutuar depois de sink.");
        for (IPosition p : frigate.getPositions())
            assertTrue(p.isHit(), "Erro: todas as posições deviam estar atingidas.");
    }

    @Test
    @DisplayName("toString inclui a categoria e o rumo do navio")
    void testToString() {
        String s = ship.toString();
        assertTrue(s.startsWith("[" + ship.getCategory() + " " + ship.getBearing() + " "), "Erro: formato inesperado: " + s);
        assertTrue(s.endsWith("]"), "Erro: devia terminar em ].");
    }

    @Test
    @DisplayName("getAdjacentPositions numa fragata: sem repetidas e sem posições do próprio navio")
    void testGetAdjacentPositionsVariasPosicoes() {
        Ship frigate = new Frigate(Compass.NORTH, new Position(5, 5));
        List<IPosition> adj = frigate.getAdjacentPositions();
        assertEquals(14, adj.size(), "Erro: uma fragata longe das margens tem 14 posições adjacentes.");
        for (IPosition a : adj)
            assertFalse(frigate.getPositions().contains(a), "Erro: uma adjacente não pode ser do próprio navio.");
    }


}