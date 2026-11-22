
package battleship;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tasks.
 * Author: LEI-129833
 * Date: 2026-11-22
 *
 * Tests for menu, menuHelp, readPosition,
 * readClassicPosition and readShip.
 */
public class TasksTest {

	private PrintStream originalOut;
	private java.io.InputStream originalIn;
	private ByteArrayOutputStream output;

	@BeforeEach
	void setUp() {
		originalOut = System.out;
		originalIn = System.in;
		output = new ByteArrayOutputStream();

		System.setOut(new PrintStream(output, true,
				StandardCharsets.UTF_8));

		Tasks.game = null;
		Tasks.myFleet = null;
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		System.setIn(originalIn);

		Tasks.game = null;
		Tasks.myFleet = null;
	}

	private void simulateMenu(String commands) {
		System.setIn(new ByteArrayInputStream(
				commands.getBytes(StandardCharsets.UTF_8)));

		Tasks.menu();
	}

	@Test
	@DisplayName("menuHelp: displays available commands")
	void menuHelp() {
		Tasks.menuHelp();

		String result = output.toString(StandardCharsets.UTF_8);

		assertAll(
				() -> assertTrue(result.contains("gerafrota"),
						"Error: expected gerafrota command"),
				() -> assertTrue(result.contains("desisto"),
						"Error: expected desisto command"),
				() -> assertTrue(result.contains("rajada"),
						"Error: expected rajada command"),
				() -> assertTrue(result.contains("simula"),
						"Error: expected simula command")
		);
	}

	@Test
	@DisplayName("menu: exits when user enters desisto")
	void menu1() {
		simulateMenu("desisto");

		assertTrue(
				output.toString(StandardCharsets.UTF_8)
						.contains("Bons ventos!"),
				"Error: expected goodbye message"
		);
	}

	@Test
	@DisplayName("menu: displays help and exits")
	void menu2() {
		simulateMenu("ajuda desisto");

		assertTrue(
				output.toString(StandardCharsets.UTF_8)
						.contains("Bons ventos!"),
				"Error: expected menu to exit"
		);
	}

	@Test
	@DisplayName("menu: handles unknown commands")
	void menu3() {
		simulateMenu("comandoinvalido desisto");

		assertTrue(
				output.toString(StandardCharsets.UTF_8)
						.contains("Que comando é esse"),
				"Error: expected unknown command message"
		);
	}

	@Test
	@DisplayName("menu: ignores status and map without a fleet")
	void menu4() {
		assertDoesNotThrow(
				() -> simulateMenu("estado mapa desisto"),
				"Error: menu should handle a missing fleet"
		);
	}

	@Test
	@DisplayName("menu: ignores shooting and simulation without a game")
	void menu5() {
		assertDoesNotThrow(
				() -> simulateMenu(
						"rajada tiros simula desisto"),
				"Error: menu should handle a missing game"
		);
	}

	@Test
	@DisplayName("readPosition: reads valid numeric coordinates")
	void readPosition1() {
		Position position = Tasks.readPosition(
				new Scanner("3 5"));

		assertAll(
				() -> assertEquals(3, position.getRow(),
						"Error: expected row 3"),
				() -> assertEquals(5, position.getColumn(),
						"Error: expected column 5")
		);
	}

	@Test
	@DisplayName("readPosition: rejects nonnumeric coordinates")
	void readPosition2() {
		assertThrows(
				java.util.InputMismatchException.class,
				() -> Tasks.readPosition(new Scanner("A B")),
				"Error: expected invalid numeric input to throw"
		);
	}

	@Test
	@DisplayName("readClassicPosition: accepts compact coordinates")
	void readClassicPosition1() {
		IPosition position = Tasks.readClassicPosition(
				new Scanner("A3"));

		assertNotNull(position,
				"Error: expected a valid position for A3");
	}

	@Test
	@DisplayName("readClassicPosition: accepts separated coordinates")
	void readClassicPosition2() {
		IPosition position = Tasks.readClassicPosition(
				new Scanner("A 3"));

		assertNotNull(position,
				"Error: expected a valid position for A 3");
	}

	@Test
	@DisplayName("readClassicPosition: accepts lowercase coordinates")
	void readClassicPosition3() {
		IPosition position = Tasks.readClassicPosition(
				new Scanner("a3"));

		assertNotNull(position,
				"Error: expected lowercase coordinates to work");
	}

	@Test
	@DisplayName("readClassicPosition: rejects invalid format")
	void readClassicPosition4() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(
						new Scanner("3A")),
				"Error: expected invalid position format"
		);
	}

	@Test
	@DisplayName("readClassicPosition: rejects empty input")
	void readClassicPosition5() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(
						new Scanner("")),
				"Error: expected exception for empty input"
		);
	}

	@Test
	@DisplayName("readClassicPosition: rejects letters without numbers")
	void readClassicPosition6() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(
						new Scanner("ABC")),
				"Error: expected exception for missing row"
		);
	}

	@Test
	@DisplayName("readShip: rejects an unknown ship type")
	void readShip1() {
		assertThrows(
				AssertionError.class,
				() -> Tasks.readShip(
						new Scanner("NavioInexistente 3 5 N")
				),
				"Error: expected AssertionError for an unknown ship type"
		);
	}

}
