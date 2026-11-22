
package battleship;

import org.junit.jupiter.api.*;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IMove.readMove().
 * Author: LEI-129833
 * Date: 2026-11-22
 * Cyclomatic Complexity:
 * - readMove(): 2 (loop-based estimate)
 */
public class IMoveTest {

    private Scanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new Scanner("1 3 5");
    }

    @AfterEach
    void tearDown() {
        if (scanner != null) {
            scanner.close();
        }
        scanner = null;
    }

    @Test
    @DisplayName("readMove: lê um disparo válido")
    void readMove1() {
        Move move = IMove.readMove(1, scanner);

        assertAll(
                () -> assertEquals(1, move.getNumber(),
                        "Error: expected move number 1"),
                () -> assertEquals(1, move.getShots().size(),
                        "Error: expected one shot"),
                () -> assertEquals(3, move.getShots().get(0).getRow(),
                        "Error: expected row 3"),
                () -> assertEquals(5, move.getShots().get(0).getColumn(),
                        "Error: expected column 5")
        );
    }

    @Test
    @DisplayName("readMove: lê três disparos válidos")
    void readMove2() {
        scanner.close();
        scanner = new Scanner("3 1 2 3 4 5 6");

        Move move = IMove.readMove(2, scanner);

        assertAll(
                () -> assertEquals(2, move.getNumber(),
                        "Error: expected move number 2"),
                () -> assertEquals(3, move.getShots().size(),
                        "Error: expected three shots"),
                () -> assertEquals(1, move.getShots().get(0).getRow(),
                        "Error: incorrect first row"),
                () -> assertEquals(4, move.getShots().get(1).getColumn(),
                        "Error: incorrect second column"),
                () -> assertEquals(6, move.getShots().get(2).getColumn(),
                        "Error: incorrect third column")
        );
    }

    @Test
    @DisplayName("readMove: rejeita número de disparos não numérico")
    void readMoveInvalidCount() {
        assertThrows(
                InputMismatchException.class,
                () -> IMove.readMove(1, new Scanner("abc")),
                "Error: expected exception for nonnumeric count"
        );
    }

    @Test
    @DisplayName("readMove: rejeita coordenadas não numéricas")
    void readMoveInvalidPosition() {
        assertThrows(
                InputMismatchException.class,
                () -> IMove.readMove(1, new Scanner("1 A 5")),
                "Error: expected exception for invalid coordinate"
        );
    }

    @Test
    @DisplayName("readMove: rejeita dados incompletos")
    void readMoveIncomplete() {
        assertThrows(
                NoSuchElementException.class,
                () -> IMove.readMove(1, new Scanner("2 1 2 3")),
                "Error: expected exception for incomplete shots"
        );
    }
}

