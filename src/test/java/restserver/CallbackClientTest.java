package restserver;

import battleship.IPosition;
import battleship.Position;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CallbackClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    private HttpServer server;
    private String callbackUrl;
    private CallbackClient client;
    private List<IPosition> shots;

    @BeforeEach
    void setUp() {
        client = new CallbackClient();
        shots = new ArrayList<>(List.of(new Position(0, 0), new Position(1, 1), new Position(2, 2)));
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
        Thread.interrupted(); // limpa o estado de interrupção, se o teste o deixou ativo
    }

    // ---------- auxiliares ----------

    /** Arranca um servidor local que devolve sempre o estado e o corpo indicados. */
    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        callbackUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String closedPortUrl() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return "http://127.0.0.1:" + s.getLocalPort();
        }
    }

    private GameSession session() {
        return new GameSession("g1", "Alice", callbackUrl);
    }

    private static Dtos.ShotResult result(String outcome) {
        Dtos.ShotResult r = new Dtos.ShotResult();
        r.row = "A";
        r.column = 1;
        r.outcome = outcome;
        return r;
    }

    private String response(List<Dtos.ShotResult> results, int shipsRemaining, String status)
            throws JsonProcessingException {
        Dtos.ShotResponse r = new Dtos.ShotResponse();
        r.results = results;
        r.shipsRemaining = shipsRemaining;
        r.gameStatus = status;
        return mapper.writeValueAsString(r);
    }

    // ---------- testes ----------

    @Test
    @DisplayName("Resposta válida: envia o pedido certo, regista a jogada e o jogo continua")
    void testSucessoJogoContinua() throws Exception {
        startServer(200, response(List.of(result("MISS"), result("HIT"), result("SUNK")), 5, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots), "Erro: devia devolver true.");

        assertEquals("POST", lastMethod.get(), "Erro: o pedido devia ser POST.");
        assertEquals("/game/g1/shots", lastPath.get(), "Erro: caminho do pedido incorreto.");
        assertEquals("application/json", lastContentType.get(), "Erro: Content-Type incorreto.");

        Dtos.ShotRequest sent = mapper.readValue(lastBody.get(), Dtos.ShotRequest.class);
        assertEquals(3, sent.shots.size(), "Erro: deviam ser enviados 3 tiros.");
        assertEquals("A", sent.shots.get(0).row);
        assertEquals(1, sent.shots.get(0).column);
        assertEquals("B", sent.shots.get(1).row);
        assertEquals(2, sent.shots.get(1).column);
        assertEquals("C", sent.shots.get(2).row);
        assertEquals(3, sent.shots.get(2).column);

        var moves = session.getGame().getAlienMoves();
        assertEquals(1, moves.size(), "Erro: devia haver uma jogada registada.");
        assertEquals(shots, moves.get(0).getShots(), "Erro: os tiros registados são diferentes.");
        var results = moves.get(0).getShotResults();
        assertEquals(3, results.size());
        assertTrue(results.get(0).valid());
        assertFalse(results.get(0).repeated());
        assertFalse(results.get(0).sunk());
        assertNull(results.get(1).ship(), "Erro: o navio do adversário não é conhecido.");
        assertTrue(results.get(2).sunk());
        assertFalse(session.isGameOver(), "Erro: o jogo não devia ter acabado.");
        assertNull(session.getWinner());
    }

    @Test
    @DisplayName("Sem navios restantes: a IA ganha")
    void testIaGanhaSemNaviosRestantes() throws Exception {
        startServer(200, response(List.of(result("SUNK")), 0, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        assertTrue(session.isGameOver(), "Erro: o jogo devia ter acabado.");
        assertEquals("AI_WINS", session.getWinner());
    }

    @Test
    @DisplayName("Estado GAME_OVER com navios restantes: a IA ganha")
    void testIaGanhaPorGameOver() throws Exception {
        startServer(200, response(List.of(result("HIT")), 3, "GAME_OVER"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        assertTrue(session.isGameOver(), "Erro: o jogo devia ter acabado.");
        assertEquals("AI_WINS", session.getWinner());
    }

    @Test
    @DisplayName("Resposta sem lista de resultados: devolve true mas não regista a jogada")
    void testSemResultados() throws Exception {
        startServer(200, response(null, 4, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        assertTrue(session.getGame().getAlienMoves().isEmpty(), "Erro: não devia haver jogada registada.");
        assertFalse(session.isGameOver());
    }

    @Test
    @DisplayName("Menos resultados do que tiros: só regista os resultados recebidos")
    void testMenosResultadosQueTiros() throws Exception {
        startServer(200, response(List.of(result("MISS")), 4, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        assertEquals(1, session.getGame().getAlienMoves().get(0).getShotResults().size(),
                "Erro: devia haver 1 resultado registado.");
    }

    @Test
    @DisplayName("Mais resultados do que tiros: ignora os resultados a mais")
    void testMaisResultadosQueTiros() throws Exception {
        startServer(200, response(
                List.of(result("MISS"), result("MISS"), result("MISS"), result("MISS"), result("MISS")),
                4, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        assertEquals(3, session.getGame().getAlienMoves().get(0).getShotResults().size(),
                "Erro: devia haver 3 resultados registados, um por tiro.");
    }

    @Test
    @DisplayName("Os resultados INVALID, REPEATED e SUNK são convertidos nas flags certas")
    void testConversaoDosResultados() throws Exception {
        startServer(200, response(List.of(result("INVALID"), result("REPEATED"), result("SUNK")), 4, "ONGOING"));
        GameSession session = session();

        assertTrue(client.sendShots(session, shots));
        var results = session.getGame().getAlienMoves().get(0).getShotResults();

        assertFalse(results.get(0).valid(), "Erro: INVALID não é válido.");
        assertFalse(results.get(0).repeated());
        assertFalse(results.get(0).sunk());

        assertTrue(results.get(1).valid(), "Erro: REPEATED é válido.");
        assertTrue(results.get(1).repeated(), "Erro: REPEATED devia estar marcado como repetido.");
        assertFalse(results.get(1).sunk());

        assertTrue(results.get(2).valid());
        assertFalse(results.get(2).repeated());
        assertTrue(results.get(2).sunk(), "Erro: SUNK devia estar marcado como afundado.");
    }

    @Test
    @DisplayName("Resposta HTTP de erro (500): devolve false e não regista nada")
    void testRespostaHttpDeErro() throws Exception {
        startServer(500, "erro interno");
        GameSession session = session();

        assertFalse(client.sendShots(session, shots), "Erro: devia devolver false.");
        assertTrue(session.getGame().getAlienMoves().isEmpty());
        assertFalse(session.isGameOver());
    }

    @Test
    @DisplayName("Corpo da resposta que não é JSON: devolve false e não regista nada")
    void testRespostaComJsonInvalido() throws Exception {
        startServer(200, "isto não é JSON");
        GameSession session = session();

        assertFalse(client.sendShots(session, shots), "Erro: devia devolver false.");
        assertTrue(session.getGame().getAlienMoves().isEmpty());
    }

    @Test
    @DisplayName("Servidor inacessível: devolve false")
    void testServidorInacessivel() throws Exception {
        GameSession session = new GameSession("g1", "Alice", closedPortUrl());

        assertFalse(client.sendShots(session, shots), "Erro: devia devolver false.");
        assertTrue(session.getGame().getAlienMoves().isEmpty());
    }

    @Test
    @DisplayName("Thread interrompida durante o pedido: devolve false")
    void testThreadInterrompida() throws Exception {
        GameSession session = new GameSession("g1", "Alice", closedPortUrl());

        Thread.currentThread().interrupt();
        assertFalse(client.sendShots(session, shots), "Erro: devia devolver false.");
    }

    @Test
    @DisplayName("Falha ao serializar os tiros: devolve false")
    void testFalhaNaSerializacao() throws Exception {
        ObjectMapper falha = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("falha simulada") {
                };
            }
        };
        Field campo = CallbackClient.class.getDeclaredField("mapper");
        campo.setAccessible(true);
        campo.set(client, falha);

        GameSession session = new GameSession("g1", "Alice", "http://127.0.0.1:1");
        assertFalse(client.sendShots(session, shots), "Erro: devia devolver false.");
    }
}