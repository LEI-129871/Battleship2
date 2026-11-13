package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

import static battleship.GameTestSupport.*;
import static battleship.GamePrintTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Test class for class Game (the printing methods are in GamePrintTest and the
 * shared helpers in GameTestSupport).
 * Author: 129869
 * Date: 2026-11-07
 * Cyclomatic Complexity (assert statements and lambdas without decisions are not counted):
 * - constructor: 1
 * - getMyFleet(), getAlienFleet(), getMyMoves(), getAlienMoves(): 1 each
 * - jsonShots(): 3             (for, catch)
 * - randomEnemyFire(): 10      (2 for, if >=, 3 while, 2 if !contains, for print)
 * - readEnemyFire(): 6         (while, &&, if matches, if hasNextInt, if size != NUMBER_SHOTS)
 * - fireShots(): 3             (if size != NUMBER_SHOTS, for)
 * - fireSingleShot(): 6        (if !inside, if, ||, if ship == null, if !stillFloating)
 * - getRepeatedShots(), getInvalidShots(), getHits(), getSunkShips(), getRemainingShips(): 1 each
 * - repeatedShot(): 3          (for, if)
 * - shotsResult(): 3           (for, if)
 */
class GameTest {

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

	// ---------------------------------------------------------------- helpers

	/** Adds to alienMoves a fake move that already "shot" every position except those in keep. */
	private void blockAllExcept(Collection<? extends IPosition> keep) {
		Set<Integer> keepKeys = new HashSet<>();
		for (IPosition p : keep) keepKeys.add(p.getRow() * Game.BOARD_SIZE + p.getColumn());
		List<IPosition> blocked = new ArrayList<>();
		for (IPosition p : allPositions())
			if (!keepKeys.contains(p.getRow() * Game.BOARD_SIZE + p.getColumn())) blocked.add(p);
		game.getAlienMoves().add(moveWithShots(blocked));
	}

	private List<IPosition> lastShots() {
		return game.getAlienMoves().getLast().getShots();
	}

	// ------------------------------------------------------------ constructor

	@Test
	void constructor() {
		assertAll("Game constructor",
				() -> assertNotNull(game, "Error: expected a non-null Game but got null"),
				() -> assertNotNull(game.getMyFleet(), "Error: expected a non-null my fleet"),
				() -> assertNotNull(game.getAlienFleet(), "Error: expected a non-null alien fleet"),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Error: expected no alien moves initially"),
				() -> assertTrue(game.getMyMoves().isEmpty(), "Error: expected no own moves initially"),
				() -> assertEquals(0, game.getInvalidShots(), "Error: expected 0 invalid shots"),
				() -> assertEquals(0, game.getRepeatedShots(), "Error: expected 0 repeated shots"),
				() -> assertEquals(0, game.getHits(), "Error: expected 0 hits"),
				() -> assertEquals(0, game.getSunkShips(), "Error: expected 0 sunk ships")
		);
	}

	// ---------------------------------------------------------------- getters

	@Test
	void getMyFleet() {
		IFleet fleet = new Fleet();
		Game g = new Game(fleet);
		assertSame(fleet, g.getMyFleet(), "Error: expected the fleet given to the constructor");
	}

	@Test
	void getAlienFleet() {
		assertAll("getAlienFleet",
				() -> assertNotNull(game.getAlienFleet(), "Error: expected a non-null alien fleet"),
				() -> assertTrue(game.getAlienFleet().getShips().isEmpty(),
						"Error: expected an empty alien fleet")
		);
	}

	@Test
	void getMyMoves() {
		assertAll("getMyMoves",
				() -> assertNotNull(game.getMyMoves(), "Error: expected a non-null list"),
				() -> assertTrue(game.getMyMoves().isEmpty(), "Error: expected an empty list")
		);
	}

	@Test
	void getAlienMoves() {
		game.fireShots(volley(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		assertEquals(1, game.getAlienMoves().size(),
				"Error: expected 1 alien move but got " + game.getAlienMoves().size());
	}

	// -------------------------------------------------------------- jsonShots

	/** Path 1: empty list -> empty JSON array. */
	@Test
	void jsonShots1() throws Exception {
		String json = Game.jsonShots(new ArrayList<>());
		JsonNode node = new ObjectMapper().readTree(json);

		assertAll("jsonShots empty",
				() -> assertTrue(node.isArray(), "Error: expected a JSON array but got " + json),
				() -> assertEquals(0, node.size(), "Error: expected an empty array but got " + json)
		);
	}

	/** Path 2: shots are serialized with classic row and column. */
	@Test
	void jsonShots2() throws Exception {
		IPosition a = new Position(0, 0);
		IPosition b = new Position(2, 3);

		String json = Game.jsonShots(new ArrayList<>(List.of(a, b)));
		JsonNode node = new ObjectMapper().readTree(json);

		assertAll("jsonShots with shots",
				() -> assertEquals(2, node.size(), "Error: expected 2 elements but got " + json),
				() -> assertEquals(String.valueOf(a.getClassicRow()), node.get(0).get("row").asText(),
						"Error: unexpected row for the first shot in " + json),
				() -> assertEquals(a.getClassicColumn(), node.get(0).get("column").asInt(),
						"Error: unexpected column for the first shot in " + json),
				() -> assertEquals(String.valueOf(b.getClassicRow()), node.get(1).get("row").asText(),
						"Error: unexpected row for the second shot in " + json),
				() -> assertEquals(b.getClassicColumn(), node.get(1).get("column").asInt(),
						"Error: unexpected column for the second shot in " + json)
		);
	}

	/** Path 3: serialization failure is wrapped in a RuntimeException. */
	@Test
	void jsonShots3() {
		try (MockedConstruction<ObjectMapper> ignored = mockConstruction(ObjectMapper.class, (mapper, context) ->
				when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { }))) {

			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> Game.jsonShots(new ArrayList<>()),
					"Error: expected a RuntimeException when serialization fails");

			assertInstanceOf(JsonProcessingException.class, ex.getCause(),
					"Error: expected the cause to be a JsonProcessingException but got " + ex.getCause());
		}
	}

	@Test
	void jsonShotsNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> Game.jsonShots(null),
				"Error: expected AssertionError for a null list");
	}

	// -------------------------------------------------------- randomEnemyFire

	/** Path 1: plenty of candidates, all picks distinct. */
	@Test
	void randomEnemyFire1() throws Exception {
		String json = game.randomEnemyFire();

		assertAll("randomEnemyFire fresh game",
				() -> assertEquals(1, game.getAlienMoves().size(), "Error: expected 1 alien move"),
				() -> assertEquals(Game.NUMBER_SHOTS, lastShots().size(),
						"Error: expected " + Game.NUMBER_SHOTS + " shots"),
				() -> assertEquals(Game.NUMBER_SHOTS, keys(lastShots()).size(),
						"Error: expected all shots to be different"),
				() -> assertEquals(Game.NUMBER_SHOTS, new ObjectMapper().readTree(json).size(),
						"Error: expected the JSON to hold " + Game.NUMBER_SHOTS + " shots but got " + json)
		);
	}

	/** Path 2: plenty of candidates, one duplicate pick is skipped (mocked Random). */
	@Test
	void randomEnemyFire2() {
		try (MockedConstruction<Random> mocked = mockConstruction(Random.class, (random, context) ->
				when(random.nextInt(anyInt())).thenReturn(0, 0, 1, 2))) {

			game.randomEnemyFire();

			Random random = mocked.constructed().get(0);
			assertAll("duplicate pick",
					() -> verify(random, times(4)).nextInt(anyInt()),
					() -> assertEquals(Game.NUMBER_SHOTS, keys(lastShots()).size(),
							"Error: expected 3 different shots after skipping the duplicate")
			);
		}
	}

	/** Path 3: fewer candidates than shots (2), distinct picks, last shot repeated. */
	@Test
	void randomEnemyFire3() {
		List<IPosition> keep = List.of(new Position(0, 0), new Position(0, 1));
		blockAllExcept(keep);

		game.randomEnemyFire();

		assertAll("two candidates",
				() -> assertEquals(Game.NUMBER_SHOTS, lastShots().size(), "Error: expected 3 shots"),
				() -> assertEquals(keys(keep), keys(lastShots()),
						"Error: expected both candidates to be shot")
		);
	}

	/** Path 4: fewer candidates than shots (2), a duplicate pick is skipped (mocked Random). */
	@Test
	void randomEnemyFire4() {
		List<IPosition> keep = List.of(new Position(0, 0), new Position(0, 1));
		blockAllExcept(keep);

		try (MockedConstruction<Random> mocked = mockConstruction(Random.class, (random, context) ->
				when(random.nextInt(anyInt())).thenReturn(0, 0, 1))) {

			game.randomEnemyFire();

			Random random = mocked.constructed().get(0);
			assertAll("duplicate pick with few candidates",
					() -> verify(random, times(3)).nextInt(anyInt()),
					() -> assertEquals(keys(keep), keys(lastShots()),
							"Error: expected both candidates to be shot")
			);
		}
	}

	/** Path 5: no candidates left -> the shots hold null and firing fails. */
	@Test
	void randomEnemyFire5() {
		blockAllExcept(List.of());

		assertThrows(Throwable.class, () -> game.randomEnemyFire(),
				"Error: expected a failure (AssertionError or NullPointerException) with no candidates");
	}

	/** Path 6: adjacent positions of sunk ships are never candidates. */
	@Test
	void randomEnemyFire6() {
		IShip barge = new Barge(Compass.NORTH, new Position(4, 4));
		game.getMyFleet().addShip(barge);
		sink(barge);

		List<IPosition> safe = List.of(new Position(9, 9), new Position(9, 8), new Position(9, 7));
		List<IPosition> keep = new ArrayList<>(safe);
		keep.addAll(barge.getAdjacentPositions());
		blockAllExcept(keep);

		game.randomEnemyFire();

		assertEquals(keys(safe), keys(lastShots()),
				"Error: expected only the 3 safe positions to be shot, never the adjacent ones");
	}

	/** Path 7: positions already shot by the enemy are never candidates. */
	@Test
	void randomEnemyFire7() {
		List<IPosition> previous = volley(new Position(0, 0), new Position(0, 1), new Position(0, 2));
		game.fireShots(previous);
		List<IPosition> safe = List.of(new Position(9, 9), new Position(9, 8), new Position(9, 7));
		List<IPosition> keep = new ArrayList<>(safe);
		keep.addAll(previous);
		blockAllExcept(keep);

		game.randomEnemyFire();

		assertEquals(keys(safe), keys(lastShots()),
				"Error: expected only the 3 never-shot positions to be shot");
	}

	/** Path 8: exactly NUMBER_SHOTS candidates (boundary of the >= condition). */
	@Test
	void randomEnemyFire8() {
		List<IPosition> keep = List.of(new Position(3, 3), new Position(3, 4), new Position(3, 5));
		blockAllExcept(keep);

		game.randomEnemyFire();

		assertAll("exactly three candidates",
				() -> assertEquals(3, lastShots().size(), "Error: expected 3 shots"),
				() -> assertEquals(keys(keep), keys(lastShots()), "Error: expected all 3 candidates to be shot")
		);
	}

	/** Path 9: the returned JSON matches the shots that were fired. */
	@Test
	void randomEnemyFire9() {
		String json = game.randomEnemyFire();

		assertEquals(Game.jsonShots(lastShots()), json,
				"Error: expected the returned JSON to match the fired shots");
	}

	/** Path 10: a single candidate -> the same shot is repeated three times. */
	@Test
	void randomEnemyFire10() {
		List<IPosition> keep = List.of(new Position(6, 6));
		blockAllExcept(keep);

		game.randomEnemyFire();

		assertAll("single candidate",
				() -> assertEquals(3, lastShots().size(), "Error: expected 3 shots"),
				() -> assertEquals(keys(keep), keys(lastShots()), "Error: expected only the candidate to be shot"),
				() -> assertEquals(2, game.getRepeatedShots(),
						"Error: expected 2 repeated shots but got " + game.getRepeatedShots())
		);
	}

	// ---------------------------------------------------------- readEnemyFire

	/** Path 1: combined tokens ("A1 B2 C3"). */
	@Test
	void readEnemyFire1() throws Exception {
		String json = game.readEnemyFire(new Scanner("A1 B2 C3\n"));

		assertAll("combined tokens",
				() -> assertEquals(1, game.getAlienMoves().size(), "Error: expected 1 alien move"),
				() -> assertEquals(3, new ObjectMapper().readTree(json).size(),
						"Error: expected 3 shots in " + json)
		);
	}

	/** Path 2: split tokens ("A 1 B 2 C 3"). */
	@Test
	void readEnemyFire2() throws Exception {
		String json = game.readEnemyFire(new Scanner("A 1 B 2 C 3\n"));

		assertAll("split tokens",
				() -> assertEquals(1, game.getAlienMoves().size(), "Error: expected 1 alien move"),
				() -> assertEquals(3, new ObjectMapper().readTree(json).size(),
						"Error: expected 3 shots in " + json)
		);
	}

	/** Path 3: a column letter not followed by a number. */
	@Test
	void readEnemyFire3() {
		assertAll("incomplete position",
				() -> assertThrows(IllegalArgumentException.class,
						() -> game.readEnemyFire(new Scanner("A x B2 C3\n")),
						"Error: expected IllegalArgumentException for a letter without a row"),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Error: expected no shots to be fired")
		);
	}

	/** Path 4: fewer than NUMBER_SHOTS positions (input ends early). */
	@Test
	void readEnemyFire4() {
		assertAll("too few positions",
				() -> assertThrows(IllegalArgumentException.class,
						() -> game.readEnemyFire(new Scanner("A1 B2\n")),
						"Error: expected IllegalArgumentException for 2 positions"),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Error: expected no shots to be fired")
		);
	}

	/** Path 5: more tokens than needed -> stops after NUMBER_SHOTS. */
	@Test
	void readEnemyFire5() throws Exception {
		String json = game.readEnemyFire(new Scanner("A1 B2 C3 D4\n"));

		assertEquals(3, new ObjectMapper().readTree(json).size(),
				"Error: expected only the first 3 shots to be used but got " + json);
	}

	/** Path 6: empty line. */
	@Test
	void readEnemyFire6() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("\n")),
				"Error: expected IllegalArgumentException for an empty line");
	}

	@Test
	void readEnemyFireNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> game.readEnemyFire(null),
				"Error: expected AssertionError for a null scanner");
	}

	// -------------------------------------------------------------- fireShots

	/** Path 1: wrong number of shots. */
	@Test
	void fireShots1() {
		List<IPosition> two = new ArrayList<>(List.of(new Position(1, 1), new Position(1, 2)));

		assertAll("wrong number of shots",
				() -> assertThrows(IllegalArgumentException.class, () -> game.fireShots(two),
						"Error: expected IllegalArgumentException for 2 shots"),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Error: expected no move to be recorded")
		);
	}

	/** Path 2: valid volley is recorded. */
	@Test
	void fireShots2() {
		List<IPosition> shots = volley(new Position(2, 3), new Position(2, 4), new Position(2, 5));

		game.fireShots(shots);

		assertAll("valid volley",
				() -> assertEquals(1, game.getAlienMoves().size(), "Error: expected 1 alien move"),
				() -> assertEquals(3, lastShots().size(), "Error: expected 3 shots in the move"),
				() -> assertEquals(0, game.getRepeatedShots(), "Error: expected no repeated shots")
		);
	}

	/** Path 3: the same position twice in one volley counts as repeated. */
	@Test
	void fireShots3() {
		game.fireShots(volley(new Position(2, 3), new Position(2, 3), new Position(2, 4)));

		assertEquals(1, game.getRepeatedShots(),
				"Error: expected 1 repeated shot but got " + game.getRepeatedShots());
	}

	@Test
	void fireShotsNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> game.fireShots(null),
				"Error: expected AssertionError for a null list");
	}

	// ---------------------------------------------------------- fireSingleShot

	/** Path 1: position outside the board. */
	@Test
	void fireSingleShot1() {
		IGame.ShotResult result = game.fireSingleShot(new Position(-1, 5), false);

		assertAll("invalid shot",
				() -> assertEquals(new IGame.ShotResult(false, false, null, false), result,
						"Error: expected an invalid ShotResult but got " + result),
				() -> assertEquals(1, game.getInvalidShots(), "Error: expected 1 invalid shot")
		);
	}

	/** Path 2: flagged as repeated by the caller (|| short-circuits). */
	@Test
	void fireSingleShot2() {
		IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), true);

		assertAll("repeated flag",
				() -> assertEquals(new IGame.ShotResult(true, true, null, false), result,
						"Error: expected a repeated ShotResult but got " + result),
				() -> assertEquals(1, game.getRepeatedShots(), "Error: expected 1 repeated shot")
		);
	}

	/** Path 3: not flagged, but already shot in a previous move. */
	@Test
	void fireSingleShot3() {
		game.fireShots(volley(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);

		assertAll("previously shot",
				() -> assertEquals(new IGame.ShotResult(true, true, null, false), result,
						"Error: expected a repeated ShotResult but got " + result),
				() -> assertEquals(1, game.getRepeatedShots(), "Error: expected 1 repeated shot")
		);
	}

	/** Path 4: water. */
	@Test
	void fireSingleShot4() {
		IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);

		assertAll("water",
				() -> assertEquals(new IGame.ShotResult(true, false, null, false), result,
						"Error: expected a water ShotResult but got " + result),
				() -> assertEquals(0, game.getHits(), "Error: expected 0 hits")
		);
	}

	/** Path 5: hit, ship still floating. */
	@Test
	void fireSingleShot5() {
		IShip caravel = new Caravel(Compass.EAST, new Position(5, 5));
		game.getMyFleet().addShip(caravel);

		IGame.ShotResult result = game.fireSingleShot(caravel.getPositions().get(0), false);

		assertAll("hit without sinking",
				() -> assertEquals(new IGame.ShotResult(true, false, caravel, false), result,
						"Error: expected a hit ShotResult without sinking but got " + result),
				() -> assertEquals(1, game.getHits(), "Error: expected 1 hit"),
				() -> assertEquals(0, game.getSunkShips(), "Error: expected 0 sunk ships")
		);
	}

	/** Path 6: hit that sinks the ship. */
	@Test
	void fireSingleShot6() {
		IShip barge = new Barge(Compass.NORTH, new Position(5, 5));
		game.getMyFleet().addShip(barge);

		IGame.ShotResult result = game.fireSingleShot(new Position(5, 5), false);

		assertAll("hit that sinks",
				() -> assertEquals(new IGame.ShotResult(true, false, barge, true), result,
						"Error: expected a sinking ShotResult but got " + result),
				() -> assertEquals(1, game.getHits(), "Error: expected 1 hit"),
				() -> assertEquals(1, game.getSunkShips(), "Error: expected 1 sunk ship")
		);
	}

	@Test
	void fireSingleShotNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> game.fireSingleShot(null, false),
				"Error: expected AssertionError for a null position");
	}

	// --------------------------------------------------------------- counters

	@Test
	void getRepeatedShots() {
		game.fireSingleShot(new Position(2, 3), true);

		assertEquals(1, game.getRepeatedShots(),
				"Error: expected 1 repeated shot but got " + game.getRepeatedShots());
	}

	@Test
	void getInvalidShots() {
		game.fireSingleShot(new Position(-1, 3), false);

		assertEquals(1, game.getInvalidShots(),
				"Error: expected 1 invalid shot but got " + game.getInvalidShots());
	}

	@Test
	void getHits() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(5, 5)));
		game.fireSingleShot(new Position(5, 5), false);

		assertEquals(1, game.getHits(), "Error: expected 1 hit but got " + game.getHits());
	}

	@Test
	void getSunkShips() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(5, 5)));
		game.fireSingleShot(new Position(5, 5), false);

		assertEquals(1, game.getSunkShips(), "Error: expected 1 sunk ship but got " + game.getSunkShips());
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));
		fleet.addShip(ship1);
		fleet.addShip(ship2);
		ship2.sink();

		assertEquals(1, game.getRemainingShips(),
				"Error: expected 1 remaining ship but got " + game.getRemainingShips());
	}

	// ----------------------------------------------------------- repeatedShot

	/** Path 1: no moves at all. */
	@Test
	void repeatedShot1() {
		assertFalse(game.repeatedShot(new Position(2, 3)),
				"Error: expected false before any move");
	}

	/** Path 2: the position was shot in a previous move. */
	@Test
	void repeatedShot2() {
		game.fireShots(volley(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		assertTrue(game.repeatedShot(new Position(2, 3)),
				"Error: expected true for a position already shot");
	}

	/** Path 3: moves exist but none contains the position. */
	@Test
	void repeatedShot3() {
		game.fireShots(volley(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		assertFalse(game.repeatedShot(new Position(8, 8)),
				"Error: expected false for a position never shot");
	}

	@Test
	void repeatedShotNull() {
		Assumptions.assumeTrue(assertionsEnabled(), "Skipped: assertions (-ea) are disabled");
		assertThrows(AssertionError.class, () -> game.repeatedShot(null),
				"Error: expected AssertionError for a null position");
	}

	// ------------------------------------------------------------ shotsResult

	/** Path 1: every shot hits water -> "null,<pos>|" three times. */
	@Test
	void shotsResult1() {
		List<IPosition> shots = volley(new Position(7, 7), new Position(7, 8), new Position(7, 9));
		game.fireShots(shots);

		String expected = "null," + shots.get(0) + "|null," + shots.get(1) + "|null," + shots.get(2) + "|";

		assertEquals(expected, game.shotsResult(),
				"Error: expected only water results but got " + game.shotsResult());
	}

	/** Path 2: a shot sinks a ship -> ",true" suffix. */
	@Test
	void shotsResult2() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		List<IPosition> shots = volley(new Position(1, 1), new Position(7, 8), new Position(7, 9));
		game.fireShots(shots);

		String[] parts = game.shotsResult().split("\\|");

		assertAll("sunk ship result",
				() -> assertEquals(3, parts.length, "Error: expected 3 segments but got " + parts.length),
				() -> assertTrue(parts[0].endsWith("," + shots.get(0) + ",true"),
						"Error: expected a sunk entry for the first shot but got " + parts[0]),
				() -> assertEquals("null," + shots.get(1), parts[1],
						"Error: expected a water entry for the second shot but got " + parts[1]),
				() -> assertEquals("null," + shots.get(2), parts[2],
						"Error: expected a water entry for the third shot but got " + parts[2])
		);
	}

	/** Path 3: a shot hits but does not sink -> ",false" suffix. */
	@Test
	void shotsResult3() {
		IShip caravel = new Caravel(Compass.EAST, new Position(5, 5));
		game.getMyFleet().addShip(caravel);
		List<IPosition> shots = volley(caravel.getPositions().get(0), new Position(7, 8), new Position(7, 9));
		game.fireShots(shots);

		String[] parts = game.shotsResult().split("\\|");

		assertTrue(parts[0].endsWith("," + shots.get(0) + ",false"),
				"Error: expected a not-sunk entry for the first shot but got " + parts[0]);
	}

	@Test
	void shotsResultNoMoves() {
		assertThrows(NoSuchElementException.class, () -> game.shotsResult(),
				"Error: expected NoSuchElementException when no move has been fired");
	}
}