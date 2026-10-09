
package battleship;

import javazoom.jl.player.Player;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundPlayer {

    // Uma única thread reproduz os sons pela ordem recebida
    private static final ExecutorService audioQueue =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "AudioPlayer");
                thread.setDaemon(true);
                return thread;
            });

    public static void playSound(String fileName) {

        audioQueue.submit(() -> {
            try (InputStream sound =
                         SoundPlayer.class.getResourceAsStream(
                                 "/sounds/" + fileName)) {

                if (sound == null) {
                    System.out.println("Som não encontrado: " + fileName);
                    return;
                }

                Player player = new Player(sound);
                player.play();
                player.close();

            } catch (Exception e) {
                System.out.println("Erro ao reproduzir som: "
                        + e.getMessage());
            }
        });
    }

    public static void waitForSounds() {
        try {
            audioQueue.submit(() -> {}).get();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void playVolley() {
        // Reproduzir três disparos consecutivos
        for (int i = 0; i < 3; i++) {
            playSound("Tiro.mp3");
        }
    }

    public static void main(String[] args) {
        playSound("Tiro.mp3");

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


