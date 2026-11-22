
package battleship;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Main.
 * Author: LEI-129833
 * Date: 2026-11-22
 * Target: Main.main(String[] args)
 */
public class MainTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;
    private ByteArrayOutputStream output;
    private ByteArrayOutputStream errors;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        originalIn = System.in;

        output = new ByteArrayOutputStream();
        errors = new ByteArrayOutputStream();

        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    @Test
    @DisplayName("main: sem argumentos inicia o menu")
    void main1() {
        System.setIn(new ByteArrayInputStream(
                "desisto\n".getBytes(StandardCharsets.UTF_8)));

        Main.main(new String[]{});

        assertAll(
                () -> assertTrue(output.toString()
                                .contains("***  Battleship  ***"),
                        "Error: expected game banner"),
                () -> assertTrue(output.toString()
                                .contains("Bons ventos!"),
                        "Error: expected goodbye message")
        );
    }

    @Test
    @DisplayName("main: --help apresenta instruções")
    void main2() {
        Main.main(new String[]{"--help"});

        assertTrue(output.toString().contains("--server"),
                "Error: expected server option in help");
    }

    @Test
    @DisplayName("main: -h apresenta instruções")
    void main3() {
        Main.main(new String[]{"-h"});

        assertTrue(output.toString().contains("--gui"),
                "Error: expected GUI option in help");
    }

    @Test
    @DisplayName("main: rejeita porta inferior a 1024")
    void main4() {
        Main.main(new String[]{"--port", "1023"});

        assertTrue(errors.toString()
                        .contains("porta num range nao permitido"),
                "Error: expected invalid port message");
    }

    @Test
    @DisplayName("main: rejeita porta superior a 65535")
    void main5() {
        Main.main(new String[]{"--port", "65536"});

        assertTrue(errors.toString()
                        .contains("porta num range nao permitido"),
                "Error: expected invalid port message");
    }

    @Test
    @DisplayName("main: rejeita porta não numérica")
    void main6() {
        Main.main(new String[]{"--port", "abc"});

        assertTrue(output.toString()
                        .contains("Erro inesperado"),
                "Error: expected invalid numeric port handling");
    }

    @Test
    @DisplayName("main: trata ausência do valor da porta")
    void main7() {
        Main.main(new String[]{"--port"});

        assertTrue(output.toString()
                        .contains("Erro inesperado"),
                "Error: expected missing port handling");
    }

    @Test
    @DisplayName("main: aceita uma opção desconhecida sem iniciar o servidor")
    void main8() {
        assertDoesNotThrow(
                () -> Main.main(new String[]{"--unknown"}),
                "Error: expected unknown option not to throw"
        );

        assertFalse(output.toString().contains("Erro inesperado"),
                "Error: unexpected error for unknown option");
    }
}

