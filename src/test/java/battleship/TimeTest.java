package battleship;

import org.junit.jupiter.api.*;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Time.
 * Author: LEI-129833
 * Date: 2026-11-22
 *
 * Cyclomatic Complexity (estimated):
 * - Constructor: 1
 * - getTemporizador(): 2
 * - reiniciar(): 1
 * - run(): 3
 */
public class TimeTest {

    private Time temporizador;

    @BeforeEach
    void setUp() {
        temporizador = new Time(null, 60);
    }

    @AfterEach
    void tearDown() {
        temporizador = null;
    }

    @Test
    @DisplayName("Constructor: initializes a timer with 60 seconds")
    void constructor() {
        assertNotNull(temporizador,
                "Error: expected a Time instance, but got null");
    }

    @Test
    @DisplayName("getTemporizador: returns remaining positive time")
    void getTemporizador1() throws InterruptedException {
        double restante = temporizador.getTemporizador();

        assertAll(
                () -> assertTrue(restante > 0,
                        "Error: expected positive remaining time"),
                () -> assertTrue(restante <= 60,
                        "Error: remaining time must not exceed 60 seconds")
        );
    }

    @Test
    @DisplayName("reiniciar: resets the timer to its initial value")
    void reiniciar() throws InterruptedException {
        Thread.sleep(1100);

        double antes = temporizador.getTemporizador();

        temporizador.reiniciar();

        double depois = temporizador.getTemporizador();

        assertAll(
                () -> assertTrue(antes < 60,
                        "Error: expected elapsed time before reset"),
                () -> assertTrue(depois > antes,
                        "Error: expected more remaining time after reset"),
                () -> assertEquals(60.0, depois, 0.01,
                        "Error: expected timer to reset to 60 seconds")
        );
    }


    @Test
    @DisplayName("getTemporizador: tempo expirado")
    void getTemporizador2() throws Exception {
        Process process = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-Djava.awt.headless=true",
                "-cp",
                System.getProperty("java.class.path"),
                "battleship.TimeExpirationTestRunner"
        ).start();

        boolean finished = process.waitFor(
                5, java.util.concurrent.TimeUnit.SECONDS
        );

        if (!finished) {
            process.destroyForcibly();
        }

        assertTrue(finished,
                "Error: expected subprocess to finish within 5 seconds");

        assertEquals(0, process.exitValue(),
                "Error: expected successful verification of timer expiration");
    }


}

