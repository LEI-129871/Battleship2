package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for class Move.
 * Author: 129869
 * Date: 2026-11-07
 * Cyclomatic Complexity (&&, || and ?: are counted as decisions):
 * - constructor: 1
 * - toString(): 1
 * - getNumber(): 1
 * - getShots(): 1
 * - getShotResults(): 1
 * - processEnemyFire(): 37
 *     for results, if !valid, if repeated, if ship == null, if sunk, if verbose,
 *     if (valid == 0 && repeated > 0) [2], 2 ternaries (repeated > 1), if valid > 0, 2 ternaries (valid > 1),
 *     if !sunk.isEmpty, for, ternary (count > 1), if !hits.isEmpty, for, if !sunk.containsKey, ternary (hits > 1),
 *     if missed > 0, ternary (missed > 1), else-if (!sunk.isEmpty || !hits.isEmpty) [2],
 *     if repeated > 0, if valid > 0, 2 ternaries (repeated > 1), if outside > 0, if !output.isEmpty,
 *     2 ternaries (outside > 1), for sunkBoats, for boatHits, if !sunk.containsKey, catch.
 */
class MoveTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final IGame.ShotResult INVALID = new IGame.ShotResult(false, false, null, false);
    private static final IGame.ShotResult REPEATED = new IGame.ShotResult(true, true, null, false);
    private static final IGame.ShotResult WATER = new IGame.ShotResult(true, false, null, false);

    private MockedStatic<SoundPlayer> sound;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        sound = mockStatic(SoundPlayer.class);
        originalOut = System.out;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        sound.close();
        sound = null;
    }

    // ---------------------------------------------------------------- helpers

    private record Outcome(String json, String line) { }

    private static IShip ship(String category) {
        IShip ship = mock(IShip.class);
        when(ship.getCategory()).thenReturn(category);
        return ship;
    }

    private static IGame.ShotResult hit(IShip ship) {
        return new IGame.ShotResult(true, false, ship, false);
    }

    private static IGame.ShotResult sunk(IShip ship) {
        return new IGame.ShotResult(true, false, ship, true);
    }

    /** Runs processEnemyFire on move #1 and returns the JSON plus the "Jogada" console line (null if absent). */
    private Outcome run(boolean verbose, IGame.ShotResult... results) {
        Move move = new Move(1, new ArrayList<>(), new ArrayList<>(Arrays.asList(results)));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        String json;
        try {
            json = move.processEnemyFire(verbose);
        } finally {
            System.setOut(originalOut);
        }
        String line = buffer.toString(StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("Jogada"))
                .findFirst().orElse(null);
        return new Outcome(json, line);
    }

    private static void assertCounts(String json, int valid, int outside, int repeated, int missed) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        assertAll("JSON counters of " + json,
                () -> assertEquals(valid, node.get("validShots").asInt(),
                        "Error: unexpected validShots in " + json),
                () -> assertEquals(outside, node.get("outsideShots").asInt(),
                        "Error: unexpected outsideShots in " + json),
                () -> assertEquals(repeated, node.get("repeatedShots").asInt(),
                        "Error: unexpected repeatedShots in " + json),
                () -> assertEquals(missed, node.get("missedShots").asInt(),
                        "Error: unexpected missedShots in " + json)
        );
    }

    private static Map<String, Integer> typeCounts(String json, String arrayName, String valueField) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        for (JsonNode item : MAPPER.readTree(json).get(arrayName))
            result.put(item.get("type").asText(), item.get(valueField).asInt());
        return result;
    }

    private static void assertContainsAll(String line, String... fragments) {
        assertNotNull(line, "Error: expected a 'Jogada' console line but none was printed");
        for (String fragment : fragments)
            assertTrue(line.contains(fragment), "Error: expected '" + fragment + "' in '" + line + "'");
    }

    // ---------------------------------------------------- simple methods (CC 1)

    @Test
    void constructor() {
        List<IPosition> shots = new ArrayList<>();
        List<IGame.ShotResult> results = new ArrayList<>();

        Move move = new Move(5, shots, results);

        assertAll("Move constructor",
                () -> assertEquals(5, move.getNumber(), "Error: expected move number 5"),
                () -> assertSame(shots, move.getShots(), "Error: expected the shots list given to the constructor"),
                () -> assertSame(results, move.getShotResults(), "Error: expected the results list given to the constructor")
        );
    }

    @Test
    void toStringTest() {
        List<IPosition> shots = new ArrayList<>(List.of(mock(IPosition.class), mock(IPosition.class), mock(IPosition.class)));
        List<IGame.ShotResult> results = new ArrayList<>(List.of(WATER, REPEATED));

        assertEquals("Move{number=7, shots=3, results=2}", new Move(7, shots, results).toString(),
                "Error: expected 'Move{number=7, shots=3, results=2}'");
    }

    @Test
    void getNumber() {
        assertEquals(3, new Move(3, new ArrayList<>(), new ArrayList<>()).getNumber(),
                "Error: expected move number 3");
    }

    @Test
    void getShots() {
        List<IPosition> shots = new ArrayList<>(List.of(mock(IPosition.class)));

        assertSame(shots, new Move(1, shots, new ArrayList<>()).getShots(),
                "Error: expected the same shots list");
    }

    @Test
    void getShotResults() {
        List<IGame.ShotResult> results = new ArrayList<>(List.of(WATER));

        assertSame(results, new Move(1, new ArrayList<>(), results).getShotResults(),
                "Error: expected the same results list");
    }

    // -------------------------------------- processEnemyFire: not verbose (1-6)

    /** Loop with zero iterations, verbose false. */
    @Test
    void processEnemyFire1() throws Exception {
        Outcome o = run(false);

        assertAll("no results",
                () -> assertNull(o.line(), "Error: expected no console summary when verbose is false"),
                () -> assertCounts(o.json(), 0, 3, 0, 0),
                () -> assertTrue(typeCounts(o.json(), "sunkBoats", "count").isEmpty(), "Error: expected no sunk boats"),
                () -> assertTrue(typeCounts(o.json(), "hitsOnBoats", "hits").isEmpty(), "Error: expected no boat hits"),
                () -> sound.verifyNoInteractions()
        );
    }

    /** !valid -> continue. */
    @Test
    void processEnemyFire2() throws Exception {
        Outcome o = run(false, INVALID);

        assertAll("invalid shot",
                () -> assertCounts(o.json(), 0, 3, 0, 0),
                () -> sound.verifyNoInteractions()
        );
    }

    /** repeated shot. */
    @Test
    void processEnemyFire3() throws Exception {
        Outcome o = run(false, REPEATED);

        assertAll("repeated shot",
                () -> assertCounts(o.json(), 0, 2, 1, 0),
                () -> sound.verifyNoInteractions()
        );
    }

    /** ship == null -> water. */
    @Test
    void processEnemyFire4() throws Exception {
        Outcome o = run(false, WATER);

        assertAll("water shot",
                () -> assertCounts(o.json(), 1, 2, 0, 1),
                () -> sound.verify(() -> SoundPlayer.playSound("Agua.mp3"), times(1))
        );
    }

    /** ship != null, not sunk. */
    @Test
    void processEnemyFire5() throws Exception {
        Outcome o = run(false, hit(ship("Barca")));

        assertAll("hit without sinking",
                () -> assertCounts(o.json(), 1, 2, 0, 0),
                () -> assertEquals(Map.of("Barca", 1), typeCounts(o.json(), "hitsOnBoats", "hits"),
                        "Error: expected one hit on a Barca"),
                () -> assertTrue(typeCounts(o.json(), "sunkBoats", "count").isEmpty(), "Error: expected no sunk boats"),
                () -> sound.verify(() -> SoundPlayer.playSound("NavioAtingido.mp3"), times(1))
        );
    }

    /** ship != null, sunk. */
    @Test
    void processEnemyFire6() throws Exception {
        Outcome o = run(false, sunk(ship("Barca")));

        assertAll("sunk ship",
                () -> assertCounts(o.json(), 1, 2, 0, 0),
                () -> assertEquals(Map.of("Barca", 1), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected one sunk Barca"),
                () -> assertTrue(typeCounts(o.json(), "hitsOnBoats", "hits").isEmpty(),
                        "Error: a sunk boat must not appear in hitsOnBoats"),
                () -> sound.verify(() -> SoundPlayer.playSound("NavioAfundado.mp3"), times(1))
        );
    }

    // ------------------------- processEnemyFire: verbose, no valid shots (7-11)

    /** verbose, no results: only outside shots. */
    @Test
    void processEnemyFire7() throws Exception {
        Outcome o = run(true);

        assertAll("verbose without results",
                () -> assertEquals("Jogada nº1 -> 3 tiros exteriores", o.line(),
                        "Error: expected '3 tiros exteriores' but got '" + o.line() + "'"),
                () -> assertCounts(o.json(), 0, 3, 0, 0)
        );
    }

    /** valid == 0 && repeated > 0, singular repeated, plural outside. */
    @Test
    void processEnemyFire8() throws Exception {
        Outcome o = run(true, REPEATED);

        assertAll("one repeated shot",
                () -> assertEquals("Jogada nº1 -> 1 tiro repetido, 2 tiros exteriores", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 0, 2, 1, 0)
        );
    }

    /** plural repeated, singular outside. */
    @Test
    void processEnemyFire9() throws Exception {
        Outcome o = run(true, REPEATED, REPEATED);

        assertAll("two repeated shots",
                () -> assertEquals("Jogada nº1 -> 2 tiros repetidos, 1 tiro exterior", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 0, 1, 2, 0)
        );
    }

    /** all repeated: no outside shots. */
    @Test
    void processEnemyFire10() throws Exception {
        Outcome o = run(true, REPEATED, REPEATED, REPEATED);

        assertAll("three repeated shots",
                () -> assertEquals("Jogada nº1 -> 3 tiros repetidos", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 0, 0, 3, 0)
        );
    }

    /** all invalid: valid == 0 and repeated == 0 (&& short-circuits on the right operand). */
    @Test
    void processEnemyFire11() throws Exception {
        Outcome o = run(true, INVALID, INVALID, INVALID);

        assertAll("three invalid shots",
                () -> assertEquals("Jogada nº1 -> 3 tiros exteriores", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 0, 3, 0, 0)
        );
    }

    // ---------------------------------- processEnemyFire: verbose, water (12-15)

    @Test
    void processEnemyFire12() throws Exception {
        Outcome o = run(true, WATER);

        assertAll("one water shot",
                () -> assertEquals("Jogada nº1 -> 1 tiro válido: 1 tiro na água, 2 tiros exteriores", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 2, 0, 1)
        );
    }

    @Test
    void processEnemyFire13() throws Exception {
        Outcome o = run(true, WATER, WATER, WATER);

        assertAll("three water shots",
                () -> assertEquals("Jogada nº1 -> 3 tiros válidos: 3 tiros na água", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 3, 0, 0, 3)
        );
    }

    /** valid + repeated + outside, all singular. */
    @Test
    void processEnemyFire14() throws Exception {
        Outcome o = run(true, WATER, REPEATED, INVALID);

        assertAll("water + repeated + invalid",
                () -> assertEquals("Jogada nº1 -> 1 tiro válido: 1 tiro na água, 1 tiro repetido, 1 tiro exterior", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 1, 1, 1)
        );
    }

    /** valid + plural repeated, no outside. */
    @Test
    void processEnemyFire15() throws Exception {
        Outcome o = run(true, WATER, REPEATED, REPEATED);

        assertAll("water + two repeated",
                () -> assertEquals("Jogada nº1 -> 1 tiro válido: 1 tiro na água, 2 tiros repetidos", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 0, 2, 1)
        );
    }

    // ---------------------------- processEnemyFire: verbose, hits and sunk (16-30)

    /** hit only: hits map non-empty, missed == 0, else-if on the second operand. */
    @Test
    void processEnemyFire16() throws Exception {
        Outcome o = run(true, hit(ship("Barca")));

        assertAll("one hit",
                () -> assertContainsAll(o.line(), "1 tiro válido: 1 tiro num(a) Barca", "2 tiros exteriores"),
                () -> assertFalse(o.line().contains("na água"), "Error: expected no water shots in '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 2, 0, 0)
        );
    }

    /** two hits on the same type -> plural. */
    @Test
    void processEnemyFire17() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), hit(ship("Barca")));

        assertAll("two hits",
                () -> assertContainsAll(o.line(), "2 tiros válidos: 2 tiros num(a) Barca", "1 tiro exterior"),
                () -> assertEquals(Map.of("Barca", 2), typeCounts(o.json(), "hitsOnBoats", "hits"),
                        "Error: expected two hits on a Barca")
        );
    }

    /** sunk only: sunk map non-empty (else-if first operand), count == 1. */
    @Test
    void processEnemyFire18() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")));

        assertAll("one sunk",
                () -> assertContainsAll(o.line(), "1 tiro válido: 1 Barca ao fundo", "2 tiros exteriores"),
                () -> assertFalse(o.line().contains("num(a)"),
                        "Error: a sunk boat must not be listed as merely hit in '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 2, 0, 0)
        );
    }

    /** two sunk of the same type -> count > 1. */
    @Test
    void processEnemyFire19() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), sunk(ship("Barca")));

        assertAll("two sunk",
                () -> assertContainsAll(o.line(), "2 tiros válidos: 2 Barcas ao fundo", "1 tiro exterior"),
                () -> assertEquals(Map.of("Barca", 2), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected two sunk Barcas")
        );
    }

    /** hit + water: missed > 0 with a hit entry. */
    @Test
    void processEnemyFire20() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), WATER);

        assertAll("hit + water",
                () -> assertEquals("Jogada nº1 -> 2 tiros válidos: 1 tiro num(a) Barca + 1 tiro na água, 1 tiro exterior",
                        o.line(), "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 2, 1, 0, 1)
        );
    }

    /** sunk + water: missed > 0 with a sunk entry. */
    @Test
    void processEnemyFire21() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), WATER);

        assertAll("sunk + water",
                () -> assertEquals("Jogada nº1 -> 2 tiros válidos: 1 Barca ao fundo + 1 tiro na água, 1 tiro exterior",
                        o.line(), "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 2, 1, 0, 1)
        );
    }

    /** sunk Barca + hit Caravela: the hit loop skips the sunk type and lists the other. */
    @Test
    void processEnemyFire22() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), hit(ship("Caravela")));

        assertAll("sunk + hit of another type",
                () -> assertContainsAll(o.line(), "2 tiros válidos: ", "1 Barca ao fundo + ", "1 tiro num(a) Caravela"),
                () -> assertFalse(o.line().contains("num(a) Barca"),
                        "Error: the sunk Barca must not be listed as hit in '" + o.line() + "'"),
                () -> assertEquals(Map.of("Barca", 1), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected one sunk Barca"),
                () -> assertEquals(Map.of("Caravela", 1), typeCounts(o.json(), "hitsOnBoats", "hits"),
                        "Error: expected only the Caravela in hitsOnBoats")
        );
    }

    /** hit + repeated: repeated branch with valid > 0 after an else-if strip. */
    @Test
    void processEnemyFire23() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), REPEATED);

        assertAll("hit + repeated",
                () -> assertContainsAll(o.line(), "1 tiro válido: 1 tiro num(a) Barca", "1 tiro repetido", "1 tiro exterior"),
                () -> assertCounts(o.json(), 1, 1, 1, 0)
        );
    }

    /** sunk + two repeated, no outside. */
    @Test
    void processEnemyFire24() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), REPEATED, REPEATED);

        assertAll("sunk + two repeated",
                () -> assertContainsAll(o.line(), "1 tiro válido: 1 Barca ao fundo", "2 tiros repetidos"),
                () -> assertFalse(o.line().contains("exterior"), "Error: expected no outside shots in '" + o.line() + "'"),
                () -> assertCounts(o.json(), 1, 0, 2, 0)
        );
    }

    /** hit + water + repeated. */
    @Test
    void processEnemyFire25() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), WATER, REPEATED);

        assertAll("hit + water + repeated",
                () -> assertEquals("Jogada nº1 -> 2 tiros válidos: 1 tiro num(a) Barca + 1 tiro na água, 1 tiro repetido",
                        o.line(), "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 2, 0, 1, 1)
        );
    }

    /** three hits on three different types: the hit loop runs several times. */
    @Test
    void processEnemyFire26() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), hit(ship("Caravela")), hit(ship("Galeao")));

        assertAll("three different hits",
                () -> assertContainsAll(o.line(), "3 tiros válidos: ", "1 tiro num(a) Barca",
                        "1 tiro num(a) Caravela", "1 tiro num(a) Galeao"),
                () -> assertFalse(o.line().contains("exterior"), "Error: expected no outside shots in '" + o.line() + "'"),
                () -> assertEquals(Map.of("Barca", 1, "Caravela", 1, "Galeao", 1),
                        typeCounts(o.json(), "hitsOnBoats", "hits"), "Error: expected one hit per type")
        );
    }

    /** three sunk of three different types: the sunk loops run several times. */
    @Test
    void processEnemyFire27() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), sunk(ship("Caravela")), sunk(ship("Nau")));

        assertAll("three different sunk",
                () -> assertContainsAll(o.line(), "3 tiros válidos: ", "1 Barca ao fundo",
                        "1 Caravela ao fundo", "1 Nau ao fundo"),
                () -> assertEquals(Map.of("Barca", 1, "Caravela", 1, "Nau", 1),
                        typeCounts(o.json(), "sunkBoats", "count"), "Error: expected one sunk boat per type"),
                () -> assertTrue(typeCounts(o.json(), "hitsOnBoats", "hits").isEmpty(),
                        "Error: sunk boats must not appear in hitsOnBoats")
        );
    }

    /** two hits on the same type + water. */
    @Test
    void processEnemyFire28() throws Exception {
        Outcome o = run(true, hit(ship("Barca")), hit(ship("Barca")), WATER);

        assertEquals("Jogada nº1 -> 3 tiros válidos: 2 tiros num(a) Barca + 1 tiro na água", o.line(),
                "Error: unexpected summary '" + o.line() + "'");
    }

    /** two sunk of the same type + water. */
    @Test
    void processEnemyFire29() throws Exception {
        Outcome o = run(true, sunk(ship("Barca")), sunk(ship("Barca")), WATER);

        assertEquals("Jogada nº1 -> 3 tiros válidos: 2 Barcas ao fundo + 1 tiro na água", o.line(),
                "Error: unexpected summary '" + o.line() + "'");
    }

    /** invalid + two water shots. */
    @Test
    void processEnemyFire30() throws Exception {
        Outcome o = run(true, INVALID, WATER, WATER);

        assertAll("invalid + two water",
                () -> assertEquals("Jogada nº1 -> 2 tiros válidos: 2 tiros na água, 1 tiro exterior", o.line(),
                        "Error: unexpected summary '" + o.line() + "'"),
                () -> assertCounts(o.json(), 2, 1, 0, 2)
        );
    }

    // ------------------------------ processEnemyFire: JSON-focused, silent (31-34)

    /** water + hit Barca + sunk Caravela, not verbose. */
    @Test
    void processEnemyFire31() throws Exception {
        Outcome o = run(false, WATER, hit(ship("Barca")), sunk(ship("Caravela")));

        assertAll("mixed volley",
                () -> assertNull(o.line(), "Error: expected no console summary when verbose is false"),
                () -> assertCounts(o.json(), 3, 0, 0, 1),
                () -> assertEquals(Map.of("Caravela", 1), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected one sunk Caravela"),
                () -> assertEquals(Map.of("Barca", 1), typeCounts(o.json(), "hitsOnBoats", "hits"),
                        "Error: expected one hit on a Barca")
        );
    }

    /** repeated + invalid + water, not verbose. */
    @Test
    void processEnemyFire32() throws Exception {
        Outcome o = run(false, REPEATED, INVALID, WATER);

        assertAll("repeated + invalid + water",
                () -> assertNull(o.line(), "Error: expected no console summary when verbose is false"),
                () -> assertCounts(o.json(), 1, 1, 1, 1)
        );
    }

    /** three sunk of the same type, not verbose. */
    @Test
    void processEnemyFire33() throws Exception {
        Outcome o = run(false, sunk(ship("Barca")), sunk(ship("Barca")), sunk(ship("Barca")));

        assertAll("three sunk Barcas",
                () -> assertCounts(o.json(), 3, 0, 0, 0),
                () -> assertEquals(Map.of("Barca", 3), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected three sunk Barcas"),
                () -> assertTrue(typeCounts(o.json(), "hitsOnBoats", "hits").isEmpty(),
                        "Error: expected no entries in hitsOnBoats")
        );
    }

    /** hits on two different types, not verbose. */
    @Test
    void processEnemyFire34() throws Exception {
        Outcome o = run(false, hit(ship("Barca")), hit(ship("Caravela")));

        assertAll("hits on two types",
                () -> assertCounts(o.json(), 2, 1, 0, 0),
                () -> assertEquals(Map.of("Barca", 1, "Caravela", 1), typeCounts(o.json(), "hitsOnBoats", "hits"),
                        "Error: expected one hit per type")
        );
    }

    // ------------------------------------------------- sounds and edge cases (35-37)

    /** each kind of result plays its own sound exactly once. */
    @Test
    void processEnemyFire35() {
        run(false, WATER, hit(ship("Barca")), sunk(ship("Caravela")));

        assertAll("sounds",
                () -> sound.verify(() -> SoundPlayer.playSound("Agua.mp3"), times(1)),
                () -> sound.verify(() -> SoundPlayer.playSound("NavioAtingido.mp3"), times(1)),
                () -> sound.verify(() -> SoundPlayer.playSound("NavioAfundado.mp3"), times(1))
        );
    }

    /** a ship hit and then sunk in the same volley is reported as sunk only. */
    @Test
    void processEnemyFire36() throws Exception {
        IShip barca = ship("Barca");
        Outcome o = run(true, hit(barca), sunk(barca));

        assertAll("hit then sunk",
                () -> assertContainsAll(o.line(), "2 tiros válidos: ", "1 Barca ao fundo", "1 tiro exterior"),
                () -> assertFalse(o.line().contains("num(a)"),
                        "Error: the sunk boat must not be listed as merely hit in '" + o.line() + "'"),
                () -> assertEquals(Map.of("Barca", 1), typeCounts(o.json(), "sunkBoats", "count"),
                        "Error: expected one sunk Barca"),
                () -> assertTrue(typeCounts(o.json(), "hitsOnBoats", "hits").isEmpty(),
                        "Error: expected no entries in hitsOnBoats")
        );
    }

    /** serialization failure is wrapped in a RuntimeException (catch branch). */
    @Test
    void processEnemyFire37() {
        try (MockedConstruction<ObjectMapper> ignored = mockConstruction(ObjectMapper.class, (mapper, context) ->
                when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { }))) {

            RuntimeException ex = assertThrows(RuntimeException.class, () -> run(false, WATER),
                    "Error: expected a RuntimeException when serialization fails");

            assertInstanceOf(JsonProcessingException.class, ex.getCause(),
                    "Error: expected the cause to be a JsonProcessingException but got " + ex.getCause());
        }
    }
}