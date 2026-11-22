package battleship;

import java.awt.HeadlessException;

public class TimeExpirationTestRunner {

    public static void main(String[] args) throws Exception {
        Time timer = new Time(null, -1);

        try {
            timer.getTemporizador();
            System.exit(1);
        } catch (HeadlessException expected) {
            System.exit(0);
        }
    }
}
