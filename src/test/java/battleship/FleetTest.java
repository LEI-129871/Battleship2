package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockStatic;

/**
 * Test class for class Fleet.
 * Author: ${user.name}
 * Date: 2026-11-07
 * Cyclomatic Complexity (assert statements are not counted as decisions):
 * - constructor: 1
 * - createRandom(): 4   (while, if, &&)
 * - getShips(): 1
 * - addShip(): 4        (if, &&, &&)
 * - getShipsLike(): 3   (for, if)
 * - getFloatingShips(): 3 (for, if)
 * - getSunkShips(): 3   (for, if)
 * - shipAt(): 3         (for, if)
 * - colisionRisk(): 3   (for, if)  [private, tested via reflection]
 * - printShips(): 2     (for)
 * - printStatus(): 1
 * - printShipsByCategory(): 1
 * - printFloatingShips(): 1
 * - printAllShips(): 1
 *
 * Notes:
 * - Tests named *Null() verify the `assert x != null` guards and need assertions enabled (-ea,
 *   which is Maven Surefire's default). They are skipped automatically otherwise.
 * - createRandom2()/createRandom3() use Mockito static mocking (mockito-core 5+).
 * - Assumes a 10x10 board and that Position(int, int) / Barge / Caravel behave as in your
 *   previous FleetTest.
 */
class FleetTest {

	private Fleet fleet;

	@BeforeEach
	void setUp() {
		fleet = new Fleet();
	}

	@AfterEach
	void tearDown() {
		fleet = null;
	}

	// ---------------------------------------------------------------- helpers

	private static boolean assertionsEnabled() {
		boolean enabled = false;
		assert enabled = true; // intentional side effect: only runs with -ea
		return enabled;
	}

	/** Adds n barges spaced 2 cells apart so none of them collide. */
	private void fillWithBarges(int n) {
		for (int i = 0; i < n; i++) {
			IShip barge = new Barge(Compass.NORTH, new Position(2 * (i % 5), 2 * (i / 5)));
			assertTrue(fleet.addShip(barge), "Error: helper expected to add barge #" + i + " but it was rejected");
		}
	}

	private static void sink(IShip ship) {
		for (IPosition p : ship.getPositions()) {
			p.shoot();
		}
	}

	private static String captureOut(Runnable action) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream original = System.out;
		System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
		try {
			action.run();
		} finally {
			System.setOut(original);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	// ------------------------------------------------------------ constructor

	@Test
	void constructor() {
		assertAll("Fleet constructor",
				() -> assertNotNull(fleet, "Error: expected a non-null Fleet but got null"),
				() -> assertNotNull(fleet.getShips(), "Error: expected a non-null ships list but got null"),
				() -> assertTrue(fleet.getShips().isEmpty(),
						"Error: expected an empty ships list but got size " + fleet.getShips().size())
		);
	}

	// ----------------------------------------------------------- createRandom

	/** Path 1: normal run, loop finishes, every ship is built and added. */
	@Test
	void createRandom1() {
		IFleet random = Fleet.createRandom();

		assertAll("createRandom normal path",
				() -> assertNotNull(random, "Error: expected a non-null fleet but got null"),
				() -> assertEquals(11, random.getShips().size(),
						"Error: expected 11 ships (size of shipTypes) but got " + random.getShips().size())
		);
	}

	/** Path 2: Ship.buildShip returns null once -> ship == null branch, retried. */
	@Test
	void createRandom2() {
		try (MockedStatic<Ship> mocked = mockStatic(Ship.class)) {
			mocked.when(() -> Ship.buildShip(anyString(), any(), any()))
					.thenReturn(null)
					.thenCallRealMethod();

			IFleet random = Fleet.createRandom();

			assertAll("createRandom with a null ship",
					() -> assertEquals(11, random.getShips().size(),
							"Error: expected 11 ships after retrying but got " + random.getShips().size()),
					() -> mocked.verify(() -> Ship.buildShip(anyString(), any(), any()), atLeast(12))
			);
		}
	}

	/** Path 3: ship != null but addShip returns false (collision) -> retried. */
	@Test
	void createRandom3() {
		Ship first = new Barge(Compass.NORTH, new Position(0, 0));
		Ship colliding = new Barge(Compass.NORTH, new Position(0, 0));

		try (MockedStatic<Ship> mocked = mockStatic(Ship.class)) {
			mocked.when(() -> Ship.buildShip(anyString(), any(), any()))
					.thenReturn(first, colliding)
					.thenCallRealMethod();

			IFleet random = Fleet.createRandom();

			assertAll("createRandom with a colliding ship",
					() -> assertEquals(11, random.getShips().size(),
							"Error: expected 11 ships but got " + random.getShips().size()),
					() -> assertTrue(random.getShips().stream().anyMatch(s -> s == first),
							"Error: expected the first ship to be in the fleet"),
					() -> assertFalse(random.getShips().stream().anyMatch(s -> s == colliding),
							"Error: expected the colliding ship to be rejected")
			);
		}
	}

	/** Path 4: loop exit with the expected composition of the fleet. */
	@Test
	void createRandom4() {
		IFleet random = Fleet.createRandom();

		assertAll("createRandom fleet composition",
				() -> assertEquals(1, random.getShipsLike("Galeao").size(),
						"Error: expected 1 Galeao"),
				() -> assertEquals(1, random.getShipsLike("Fragata").size(),
						"Error: expected 1 Fragata"),
				() -> assertEquals(2, random.getShipsLike("Nau").size(),
						"Error: expected 2 Nau"),
				() -> assertEquals(3, random.getShipsLike("Caravela").size(),
						"Error: expected 3 Caravela"),
				() -> assertEquals(4, random.getShipsLike("Barca").size(),
						"Error: expected 4 Barca")
		);
	}

	// --------------------------------------------------------------- getShips

	@Test
	void getShips() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		assertTrue(fleet.getShips().isEmpty(), "Error: expected an initially empty list");
		fleet.addShip(ship);

		assertAll("getShips",
				() -> assertEquals(1, fleet.getShips().size(),
						"Error: expected size 1 but got " + fleet.getShips().size()),
				() -> assertSame(ship, fleet.getShips().get(0),
						"Error: expected the added ship to be returned")
		);
	}

	// ---------------------------------------------------------------- addShip

	/** Path 1: all three conditions true -> ship added. */
	@Test
	void addShip1() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));

		assertAll("addShip valid ship",
				() -> assertTrue(fleet.addShip(ship), "Error: expected true for a valid ship but got false"),
				() -> assertEquals(1, fleet.getShips().size(),
						"Error: expected 1 ship but got " + fleet.getShips().size())
		);
	}

	/** Path 2: size condition false (note the code uses <=, so the limit is FLEET_SIZE + 1 ships). */
	@Test
	void addShip2() {
		fillWithBarges(Fleet.FLEET_SIZE + 1);
		IShip extra = new Barge(Compass.NORTH, new Position(9, 9));

		assertAll("addShip over the size limit",
				() -> assertFalse(fleet.addShip(extra),
						"Error: expected false when the fleet is full but got true"),
				() -> assertEquals(Fleet.FLEET_SIZE + 1, fleet.getShips().size(),
						"Error: expected the fleet size to stay at " + (Fleet.FLEET_SIZE + 1))
		);
	}

	/** Path 3: size ok, isInsideBoard false. */
	@Test
	void addShip3() {
		IShip outside = new Barge(Compass.NORTH, new Position(99, 99));

		assertAll("addShip outside the board",
				() -> assertFalse(fleet.addShip(outside),
						"Error: expected false for a ship outside the board but got true"),
				() -> assertTrue(fleet.getShips().isEmpty(),
						"Error: expected an empty fleet but got size " + fleet.getShips().size())
		);
	}

	/** Path 4: size ok, inside board, colisionRisk true. */
	@Test
	void addShip4() {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		IShip overlapping = new Barge(Compass.NORTH, new Position(1, 1));

		assertAll("addShip with collision risk",
				() -> assertFalse(fleet.addShip(overlapping),
						"Error: expected false for an overlapping ship but got true"),
				() -> assertEquals(1, fleet.getShips().size(),
						"Error: expected size 1 but got " + fleet.getShips().size())
		);
	}

	@Test
	void addShipNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> fleet.addShip(null),
				"Error: expected AssertionError for a null ship but nothing was thrown");
	}

	// ------------------------------------------------------------ getShipsLike

	/** Path 1: empty fleet, loop body never runs. */
	@Test
	void getShipsLike1() {
		assertTrue(fleet.getShipsLike("Barca").isEmpty(),
				"Error: expected an empty list for an empty fleet");
	}

	/** Path 2: category matches. */
	@Test
	void getShipsLike2() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(barge);
		fleet.addShip(new Caravel(Compass.NORTH, new Position(4, 4)));

		List<IShip> result = fleet.getShipsLike("Barca");

		assertAll("getShipsLike matching category",
				() -> assertEquals(1, result.size(), "Error: expected 1 Barca but got " + result.size()),
				() -> assertSame(barge, result.get(0), "Error: expected the barge to be returned")
		);
	}

	/** Path 3: ships exist but none matches. */
	@Test
	void getShipsLike3() {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		assertTrue(fleet.getShipsLike("Galeao").isEmpty(),
				"Error: expected an empty list for a category with no ships");
	}

	@Test
	void getShipsLikeNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> fleet.getShipsLike(null),
				"Error: expected AssertionError for a null category but nothing was thrown");
	}

	// ------------------------------------------------------- getFloatingShips

	/** Path 1: empty fleet. */
	@Test
	void getFloatingShips1() {
		assertTrue(fleet.getFloatingShips().isEmpty(),
				"Error: expected an empty list for an empty fleet");
	}

	/** Path 2: floating ship is included. */
	@Test
	void getFloatingShips2() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);

		List<IShip> result = fleet.getFloatingShips();

		assertAll("getFloatingShips with a floating ship",
				() -> assertEquals(1, result.size(), "Error: expected 1 floating ship but got " + result.size()),
				() -> assertSame(ship, result.get(0), "Error: expected the floating ship to be returned")
		);
	}

	/** Path 3: sunk ship is excluded. */
	@Test
	void getFloatingShips3() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);
		sink(ship);

		assertTrue(fleet.getFloatingShips().isEmpty(),
				"Error: expected no floating ships after sinking the only one");
	}

	// ------------------------------------------------------------ getSunkShips

	/** Path 1: empty fleet. */
	@Test
	void getSunkShips1() {
		assertTrue(fleet.getSunkShips().isEmpty(),
				"Error: expected an empty list for an empty fleet");
	}

	/** Path 2: sunk ship is included. */
	@Test
	void getSunkShips2() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);
		sink(ship);

		List<IShip> result = fleet.getSunkShips();

		assertAll("getSunkShips with a sunk ship",
				() -> assertEquals(1, result.size(), "Error: expected 1 sunk ship but got " + result.size()),
				() -> assertSame(ship, result.get(0), "Error: expected the sunk ship to be returned")
		);
	}

	/** Path 3: floating ship is excluded. */
	@Test
	void getSunkShips3() {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		assertTrue(fleet.getSunkShips().isEmpty(),
				"Error: expected no sunk ships when everything is floating");
	}

	// ----------------------------------------------------------------- shipAt

	/** Path 1: empty fleet, loop body never runs. */
	@Test
	void shipAt1() {
		assertNull(fleet.shipAt(new Position(1, 1)),
				"Error: expected null for an empty fleet");
	}

	/** Path 2: a ship occupies the position. */
	@Test
	void shipAt2() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);

		assertSame(ship, fleet.shipAt(new Position(1, 1)),
				"Error: expected the ship occupying (1,1)");
	}

	/** Path 3: ships exist but none occupies the position. */
	@Test
	void shipAt3() {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		assertNull(fleet.shipAt(new Position(5, 5)),
				"Error: expected null for a position without ships");
	}

	@Test
	void shipAtNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> fleet.shipAt(null),
				"Error: expected AssertionError for a null position but nothing was thrown");
	}

	// ------------------------------------------------ colisionRisk (private)

	private boolean invokeColisionRisk(IShip ship) throws Exception {
		Method method = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
		method.setAccessible(true);
		return (Boolean) method.invoke(fleet, ship);
	}

	/** Path 1: empty fleet, loop body never runs. */
	@Test
	void colisionRisk1() throws Exception {
		assertFalse(invokeColisionRisk(new Barge(Compass.NORTH, new Position(1, 1))),
				"Error: expected no collision risk in an empty fleet");
	}

	/** Path 2: tooCloseTo returns true. */
	@Test
	void colisionRisk2() throws Exception {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		assertTrue(invokeColisionRisk(new Barge(Compass.NORTH, new Position(1, 1))),
				"Error: expected a collision risk for overlapping ships");
	}

	/** Path 3: ships exist but none is too close. */
	@Test
	void colisionRisk3() throws Exception {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		assertFalse(invokeColisionRisk(new Barge(Compass.NORTH, new Position(5, 5))),
				"Error: expected no collision risk for distant ships");
	}

	@Test
	void colisionRiskNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		InvocationTargetException ex = assertThrows(InvocationTargetException.class,
				() -> invokeColisionRisk(null),
				"Error: expected InvocationTargetException for a null ship but nothing was thrown");
		assertInstanceOf(AssertionError.class, ex.getCause(),
				"Error: expected the cause to be AssertionError but got " + ex.getCause());
	}

	// ------------------------------------------------------------- printShips

	/** Path 1: empty list, nothing printed. */
	@Test
	void printShips1() {
		String out = captureOut(() -> fleet.printShips(new ArrayList<>()));

		assertEquals("", out, "Error: expected no output for an empty list but got '" + out + "'");
	}

	/** Path 2: one line per ship. */
	@Test
	void printShips2() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		IShip caravel = new Caravel(Compass.NORTH, new Position(4, 4));
		String expected = barge + System.lineSeparator() + caravel + System.lineSeparator();

		String out = captureOut(() -> fleet.printShips(List.of(barge, caravel)));

		assertEquals(expected, out, "Error: expected one line per ship but got '" + out + "'");
	}

	@Test
	void printShipsNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> fleet.printShips(null),
				"Error: expected AssertionError for a null list but nothing was thrown");
	}

	// ------------------------------------------------------------ printStatus

	@Test
	void printStatus() {
		IShip floating = new Barge(Compass.NORTH, new Position(1, 1));
		IShip sunk = new Barge(Compass.NORTH, new Position(5, 5));
		fleet.addShip(floating);
		fleet.addShip(sunk);
		sink(sunk);

		String out = captureOut(() -> fleet.printStatus());

		assertEquals("Estado da Frota: 1 a flutuar, 1 afundados!" + System.lineSeparator(), out,
				"Error: expected '1 a flutuar, 1 afundados' status line but got '" + out + "'");
	}

	// ---------------------------------------------------- printShipsByCategory

	@Test
	void printShipsByCategory() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(barge);
		fleet.addShip(new Caravel(Compass.NORTH, new Position(4, 4)));

		String out = captureOut(() -> fleet.printShipsByCategory("Barca"));

		assertEquals(barge + System.lineSeparator(), out,
				"Error: expected only the barge to be printed but got '" + out + "'");
	}

	@Test
	void printShipsByCategoryNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null),
				"Error: expected AssertionError for a null category but nothing was thrown");
	}

	// ------------------------------------------------------ printFloatingShips

	@Test
	void printFloatingShips() {
		IShip floating = new Barge(Compass.NORTH, new Position(1, 1));
		IShip sunk = new Barge(Compass.NORTH, new Position(5, 5));
		fleet.addShip(floating);
		fleet.addShip(sunk);
		sink(sunk);

		String out = captureOut(() -> fleet.printFloatingShips());

		assertEquals(floating + System.lineSeparator(), out,
				"Error: expected only the floating ship to be printed but got '" + out + "'");
	}

	// ---------------------------------------------------------- printAllShips

	@Test
	void printAllShips() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		IShip caravel = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(barge);
		fleet.addShip(caravel);

		String out = captureOut(() -> fleet.printAllShips());

		assertEquals(barge + System.lineSeparator() + caravel + System.lineSeparator(), out,
				"Error: expected every ship to be printed but got '" + out + "'");
	}
}