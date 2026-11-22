
package battleship;

import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class SoundPlayerTest {

    private SoundPlayer soundPlayer;

    @BeforeEach
    void setUp() {
        soundPlayer = new SoundPlayer();
    }

    @AfterEach
    void tearDown() {
        soundPlayer = null;
    }

    @Test
    @DisplayName("SoundPlayer: inicialização da classe")
    void constructor() {
        assertNotNull(soundPlayer,
                "Error: expected SoundPlayer instance, but got null");
    }

    @Test
    @DisplayName("playSound: ficheiro inexistente apresenta mensagem de erro")
    void playSound1() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setErr(new PrintStream(output));

            SoundPlayer.playSound("inexistente.mp3");
            SoundPlayer.waitForSounds();

            assertTrue(
                    output.toString().contains(
                            "Som não encontrado: inexistente.mp3"),
                    "Error: expected missing sound message"
            );
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("playSound: reproduz um ficheiro de som existente")
    void playSound2() {
        assertNotNull(
                SoundPlayer.class.getResource("/sounds/Tiro.mp3"),
                "Error: expected Tiro.mp3 to exist in resources"
        );

        SoundPlayer.playSound("Tiro.mp3");
        SoundPlayer.waitForSounds();
    }

    @Test
    @DisplayName("playVolley: executa uma sequência de três disparos")
    void playVolley() {
        assertDoesNotThrow(
                () -> {
                    SoundPlayer.playVolley();
                    SoundPlayer.waitForSounds();
                },
                "Error: expected volley execution without exceptions"
        );
    }
}
