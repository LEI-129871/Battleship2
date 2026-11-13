package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Test class for class Server.
 * Author: 129869
 * Date: 2026-11-07
 * Cyclomatic Complexity:
 * - constructor (default): 1
 * - run(): 6  (handler lambda included: for formParams, if isCli, if Tasks.game == null,
 *              for split, if gw.getGame() == null)
 * - stop(): 2
 */
class ServerTest {

    private Server server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = new Server();
        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // we want to inspect the 302 itself
                .build();
        port = findFreePort();
        Tasks.game = null;
    }

    @AfterEach
    void tearDown() {
        try {
            if (server != null) server.stop();
        } catch (Exception ignored) {
            // server may already be stopped by the test
        }
        server = null;
        client = null;
        Tasks.game = null;
    }

    // ---------------------------------------------------------------- helpers

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private HttpResponse<String> post(String formBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/rajada"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String location(HttpResponse<String> response) {
        return response.headers().firstValue("Location").orElse("");
    }

    // ------------------------------------------------------------ constructor

    @Test
    void constructor() {
        Server s = new Server();
        assertNotNull(s, "Error: expected a non-null Server instance but got null");
    }

    // ------------------------------------------------------------------ run()

    /** Path 1: isCli = true, Tasks.game == null -> redirects to "/". */
    @Test
    void run1() throws Exception {
        server.run(port, mock(GameWindow.class), mock(IGame.class), true);
        Tasks.game = null;

        HttpResponse<String> response = post("tiro=A1");

        assertAll("CLI mode without a generated fleet",
                () -> assertEquals(302, response.statusCode(),
                        "Error: expected HTTP 302 but got " + response.statusCode()),
                () -> assertEquals("/", location(response),
                        "Error: expected redirect to '/' but got '" + location(response) + "'")
        );
    }

    /** Path 2: isCli = true, Tasks.game != null, valid shots -> fires shots and redirects with result. */
    @Test
    void run2() throws Exception {
        IGame game = mock(IGame.class);
        when(game.shotsResult()).thenReturn("OK");
        Tasks.game = game;
        server.run(port, mock(GameWindow.class), mock(IGame.class), true);

        HttpResponse<String> response = post("tiro=a1&tiro=B2");

        assertAll("CLI mode with a generated fleet",
                () -> assertEquals(302, response.statusCode(),
                        "Error: expected HTTP 302 but got " + response.statusCode()),
                () -> assertEquals("/?result=OK", location(response),
                        "Error: expected redirect to '/?result=OK' but got '" + location(response) + "'"),
                () -> verify(game, times(1)).fireShots(anyList()),
                () -> verify(game, times(1)).printMyBoard(true, false)
        );
    }

    /**
     * Path 3: isCli = true, Tasks.game != null, NO "tiro" params.
     * "".split(" ") yields [""], so charAt(0) throws and Javalin answers 500
     * (the formParams loop runs zero iterations).
     */
    @Test
    void run3() throws Exception {
        IGame game = mock(IGame.class);
        Tasks.game = game;
        server.run(port, mock(GameWindow.class), mock(IGame.class), true);

        HttpResponse<String> response = post("other=x");

        assertAll("CLI mode without shots",
                () -> assertEquals(500, response.statusCode(),
                        "Error: expected HTTP 500 (empty shot string) but got " + response.statusCode()),
                () -> verify(game, never()).fireShots(anyList())
        );
    }

    /** Path 4: isCli = false, gw.getGame() == null -> redirects to "/". */
    @Test
    void run4() throws Exception {
        GameWindow gw = mock(GameWindow.class);
        when(gw.getGame()).thenReturn(null);
        server.run(port, gw, mock(IGame.class), false);

        HttpResponse<String> response = post("tiro=A1");

        assertAll("GUI mode without a generated fleet",
                () -> assertEquals(302, response.statusCode(),
                        "Error: expected HTTP 302 but got " + response.statusCode()),
                () -> assertEquals("/", location(response),
                        "Error: expected redirect to '/' but got '" + location(response) + "'"),
                () -> verify(gw, never()).getFieldRajada()
        );
    }

    /** Path 5: isCli = false, gw.getGame() != null -> fills text field, clicks button, redirects with result. */
    @Test
    void run5() throws Exception {
        IGame game = mock(IGame.class);
        when(game.shotsResult()).thenReturn("HIT");
        JTextField field = new JTextField();
        JButton button = new JButton();
        AtomicInteger clicks = new AtomicInteger();
        button.addActionListener(e -> clicks.incrementAndGet());

        GameWindow gw = mock(GameWindow.class);
        when(gw.getGame()).thenReturn(game);
        when(gw.getShotsResult()).thenCallRealMethod();
        when(gw.getFieldRajada()).thenReturn(field);
        when(gw.getRajada()).thenReturn(button);
        server.run(port, gw, game, false);

        HttpResponse<String> response = post("tiro=C3");

        assertAll("GUI mode with a generated fleet",
                () -> assertEquals(302, response.statusCode(),
                        "Error: expected HTTP 302 but got " + response.statusCode()),
                () -> assertEquals("/?result=HIT", location(response),
                        "Error: expected redirect to '/?result=HIT' but got '" + location(response) + "'"),
                () -> assertEquals("C3 ", field.getText(),
                        "Error: expected field text 'C3 ' but got '" + field.getText() + "'"),
                () -> assertEquals(1, clicks.get(),
                        "Error: expected 1 button click but got " + clicks.get())
        );
    }

    /** Path 6: isCli = false, several "tiro" params -> all concatenated with trailing spaces. */
    @Test
    void run6() throws Exception {
        IGame game = mock(IGame.class);
        when(game.shotsResult()).thenReturn("MISS");
        JTextField field = new JTextField();
        GameWindow gw = mock(GameWindow.class);
        when(gw.getGame()).thenReturn(game);
        when(gw.getShotsResult()).thenCallRealMethod();
        when(gw.getFieldRajada()).thenReturn(field);
        when(gw.getRajada()).thenReturn(new JButton());
        server.run(port, gw, game, false);

        HttpResponse<String> response = post("tiro=A1&tiro=B2&tiro=C3");

        assertAll("GUI mode with multiple shots",
                () -> assertEquals(302, response.statusCode(),
                        "Error: expected HTTP 302 but got " + response.statusCode()),
                () -> assertEquals("A1 B2 C3 ", field.getText(),
                        "Error: expected field text 'A1 B2 C3 ' but got '" + field.getText() + "'"),
                () -> assertEquals("/?result=MISS", location(response),
                        "Error: expected redirect to '/?result=MISS' but got '" + location(response) + "'")
        );
    }

    // ----------------------------------------------------------------- stop()

    /** Path 1: app == null (run() never called) -> no exception. */
    @Test
    void stop1() {
        assertDoesNotThrow(() -> server.stop(),
                "Error: expected stop() on a never-started server not to throw");
    }

    /** Path 2: app != null -> server stops and no longer accepts connections. */
    @Test
    void stop2() throws Exception {
        server.run(port, mock(GameWindow.class), mock(IGame.class), true);
        assertEquals(302, post("tiro=A1").statusCode(),
                "Error: expected the running server to answer before stop()");

        server.stop();

        assertThrows(IOException.class, () -> post("tiro=A1"),
                "Error: expected a connection failure after stop() but the request succeeded");
    }
}