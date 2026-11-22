package restserver;

import battleship.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    /** Estratégia da IA falsa: devolve sempre os mesmos tiros e conta as chamadas. */
    private static class FakeAi extends AiShotStrategy {
        int calls;
        final List<IPosition> shots = List.of(new Position(0, 0), new Position(1, 1), new Position(2, 2));

        @Override
        public List<IPosition> computeShots(IGame game) {
            calls++;
            return shots;
        }
    }

    /** Cliente de callback falso: não usa rede, regista as chamadas e devolve o resultado configurado. */
    private static class FakeCallback extends CallbackClient {
        int calls;
        boolean ok = true;
        List<IPosition> received;

        @Override
        public boolean sendShots(GameSession session, List<IPosition> aiShots) {
            calls++;
            received = aiShots;
            return ok;
        }
    }

    private GameRegistry registry;
    private FakeAi ai;
    private FakeCallback callback;
    private GameController controller;

    // estado do jogo simulado
    private final Deque<IGame.ShotResult> queue = new ArrayDeque<>();
    private final List<Boolean> repeatedFlags = new ArrayList<>();
    private int remaining;

    @BeforeEach
    void setUp() {
        registry = new GameRegistry();
        ai = new FakeAi();
        callback = new FakeCallback();
        controller = new GameController(registry, callback, ai);
        remaining = 5;
    }

    // ---------- auxiliares ----------

    private IGame fakeGame() {
        return (IGame) Proxy.newProxyInstance(IGame.class.getClassLoader(), new Class<?>[]{IGame.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "fireSingleShot" -> {
                        repeatedFlags.add((Boolean) args[1]);
                        yield queue.poll();
                    }
                    case "getRemainingShips" -> remaining;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private GameSession newSession() throws Exception {
        GameSession session = registry.createSession("Alice", "http://localhost:9090");
        Field game = GameSession.class.getDeclaredField("game");
        game.setAccessible(true);
        game.set(session, fakeGame());
        return session;
    }

    private static IShip anyShip() {
        return new Barge(Compass.NORTH, new Position(0, 0));
    }

    private static IGame.ShotResult res(boolean valid, boolean repeated, IShip ship, boolean sunk) {
        return new IGame.ShotResult(valid, repeated, ship, sunk);
    }

    private static Dtos.ShotPosition sp(String row, int column) {
        Dtos.ShotPosition p = new Dtos.ShotPosition();
        p.row = row;
        p.column = column;
        return p;
    }

    private static Dtos.ShotRequest request(Dtos.ShotPosition... positions) {
        Dtos.ShotRequest r = new Dtos.ShotRequest();
        r.shots = new ArrayList<>(Arrays.asList(positions));
        return r;
    }

    private static Dtos.ShotRequest validShots() {
        return request(sp("A", 1), sp("B", 2), sp("C", 3));
    }

    private static Dtos.RegistrationRequest registration(String name, String url) {
        Dtos.RegistrationRequest r = new Dtos.RegistrationRequest();
        r.playerName = name;
        r.callbackUrl = url;
        return r;
    }

    private static String errorOf(ResponseEntity<?> response) {
        return ((Dtos.ErrorResponse) response.getBody()).error;
    }

    private void assertBadRequest(Dtos.ShotRequest request, GameSession session) {
        ResponseEntity<?> r = controller.receiveShots(session.getGameId(), request);
        assertEquals(400, r.getStatusCode().value(), "Erro: devia devolver 400.");
    }

    // ---------- register ----------

    @Test
    @DisplayName("Registo válido: cria a sessão e devolve o identificador do jogo")
    void testRegistoValido() {
        ResponseEntity<?> r = controller.register(registration("Alice", "http://localhost:9090"));

        assertEquals(200, r.getStatusCode().value());
        Dtos.RegistrationResponse body = (Dtos.RegistrationResponse) r.getBody();
        assertEquals("REGISTERED", body.status);
        assertEquals(Game.NUMBER_SHOTS, body.shotsPerTurn);
        assertTrue(body.message.contains("Alice"), "Erro: a mensagem devia incluir o nome do jogador.");
        assertNotNull(registry.getSession(body.gameId), "Erro: a sessão devia estar registada.");
    }

    @Test
    @DisplayName("Registo sem nome (nulo ou em branco): devolve 400")
    void testRegistoSemNome() {
        ResponseEntity<?> nulo = controller.register(registration(null, "http://localhost:9090"));
        assertEquals(400, nulo.getStatusCode().value());
        assertEquals("playerName is required", errorOf(nulo));

        ResponseEntity<?> branco = controller.register(registration("  ", "http://localhost:9090"));
        assertEquals(400, branco.getStatusCode().value());
    }

    @Test
    @DisplayName("Registo sem callbackUrl (nulo ou em branco): devolve 400")
    void testRegistoSemCallback() {
        ResponseEntity<?> nulo = controller.register(registration("Alice", null));
        assertEquals(400, nulo.getStatusCode().value());
        assertEquals("callbackUrl is required", errorOf(nulo));

        ResponseEntity<?> branco = controller.register(registration("Alice", " "));
        assertEquals(400, branco.getStatusCode().value());
    }

    // ---------- receiveShots: validações ----------

    @Test
    @DisplayName("Jogo inexistente: devolve 404")
    void testJogoNaoEncontrado() {
        ResponseEntity<?> r = controller.receiveShots("nao-existe", validShots());
        assertEquals(404, r.getStatusCode().value());
        assertTrue(errorOf(r).contains("Game not found"));
    }

    @Test
    @DisplayName("Jogo já terminado: devolve 410")
    void testJogoTerminado() throws Exception {
        GameSession session = newSession();
        session.markAiWins();

        ResponseEntity<?> r = controller.receiveShots(session.getGameId(), validShots());
        assertEquals(410, r.getStatusCode().value());
        assertTrue(errorOf(r).contains("AI_WINS"), "Erro: devia indicar o vencedor.");
    }

    @Test
    @DisplayName("Número de tiros inválido (nulo ou diferente de 3): devolve 400")
    void testNumeroDeTirosInvalido() throws Exception {
        GameSession session = newSession();

        Dtos.ShotRequest semTiros = new Dtos.ShotRequest();
        semTiros.shots = null;
        assertBadRequest(semTiros, session);

        assertBadRequest(request(sp("A", 1), sp("B", 2)), session);
    }

    @Test
    @DisplayName("Posições inválidas (linha nula, em branco, coluna 0 ou 11): devolve 400")
    void testPosicaoInvalida() throws Exception {
        GameSession session = newSession();

        assertBadRequest(request(sp(null, 1), sp("B", 2), sp("C", 3)), session);
        assertBadRequest(request(sp("A", 1), sp(" ", 2), sp("C", 3)), session);
        assertBadRequest(request(sp("A", 1), sp("B", 2), sp("C", 0)), session);

        ResponseEntity<?> r = controller.receiveShots(session.getGameId(),
                request(sp("A", 1), sp("B", 2), sp("C", 11)));
        assertEquals(400, r.getStatusCode().value());
        assertTrue(errorOf(r).startsWith("Invalid position"), "Erro: mensagem inesperada.");
    }

    // ---------- receiveShots: resultados ----------

    @Test
    @DisplayName("Resultados INVALID, REPEATED e MISS são traduzidos corretamente")
    void testResultadosInvalidoRepetidoEAgua() throws Exception {
        GameSession session = newSession();
        queue.add(res(false, false, null, false));
        queue.add(res(true, true, null, false));
        queue.add(res(true, false, null, false));

        ResponseEntity<?> r = controller.receiveShots(session.getGameId(), validShots());
        assertEquals(200, r.getStatusCode().value());
        Dtos.ShotResponse body = (Dtos.ShotResponse) r.getBody();

        assertEquals("INVALID", body.results.get(0).outcome);
        assertEquals("REPEATED", body.results.get(1).outcome);
        assertEquals("MISS", body.results.get(2).outcome);
        assertNull(body.results.get(2).shipType, "Erro: uma falha não tem tipo de navio.");
        assertEquals("A", body.results.get(0).row);
        assertEquals(1, body.results.get(0).column);
    }

    @Test
    @DisplayName("Resultados SUNK e HIT incluem o tipo de navio")
    void testResultadosAfundadoEAcertado() throws Exception {
        GameSession session = newSession();
        IGame.ShotResult sunk = res(true, false, anyShip(), true);
        IGame.ShotResult hit = res(true, false, anyShip(), false);
        queue.add(sunk);
        queue.add(hit);
        queue.add(res(true, false, null, false));

        Dtos.ShotResponse body = (Dtos.ShotResponse) controller
                .receiveShots(session.getGameId(), validShots()).getBody();

        assertEquals("SUNK", body.results.get(0).outcome);
        assertEquals(sunk.shipCategory(), body.results.get(0).shipType);
        assertEquals("HIT", body.results.get(1).outcome);
        assertEquals(hit.shipCategory(), body.results.get(1).shipType);
    }

    @Test
    @DisplayName("Tiro repetido dentro da mesma jogada é sinalizado ao jogo")
    void testTirosRepetidosNaMesmaJogada() throws Exception {
        GameSession session = newSession();
        for (int i = 0; i < 3; i++) queue.add(res(true, false, null, false));

        controller.receiveShots(session.getGameId(), request(sp("A", 1), sp("A", 1), sp("B", 2)));

        assertEquals(List.of(false, true, false), repeatedFlags,
                "Erro: só o segundo tiro em A1 devia contar como repetido na jogada.");
    }

    @Test
    @DisplayName("A letra da linha em minúscula é convertida para maiúscula")
    void testLetraMinuscula() throws Exception {
        GameSession session = newSession();
        for (int i = 0; i < 3; i++) queue.add(res(true, false, null, false));

        Dtos.ShotResponse body = (Dtos.ShotResponse) controller.receiveShots(session.getGameId(),
                request(sp("a", 1), sp("b", 2), sp("c", 3))).getBody();

        assertEquals("A", body.results.get(0).row);
        assertEquals("B", body.results.get(1).row);
        assertEquals("C", body.results.get(2).row);
    }

    // ---------- receiveShots: fim de jogo e resposta da IA ----------

    @Test
    @DisplayName("O estudante afunda todos os navios: ganha e a IA não joga")
    void testEstudanteGanha() throws Exception {
        GameSession session = newSession();
        for (int i = 0; i < 3; i++) queue.add(res(true, false, anyShip(), true));
        remaining = 0;

        ResponseEntity<?> r = controller.receiveShots(session.getGameId(), validShots());
        Dtos.ShotResponse body = (Dtos.ShotResponse) r.getBody();

        assertEquals(200, r.getStatusCode().value());
        assertEquals("GAME_OVER", body.gameStatus);
        assertEquals("STUDENT_WINS", body.winner);
        assertEquals(0, body.shipsRemaining);
        assertTrue(session.isGameOver());
        assertEquals("STUDENT_WINS", session.getWinner());
        assertEquals(0, ai.calls, "Erro: a IA não devia jogar depois de perder.");
        assertEquals(0, callback.calls, "Erro: não devia haver callback depois de perder.");
    }

    @Test
    @DisplayName("O jogo continua: a IA calcula os tiros e envia-os pelo callback")
    void testJogoContinuaComCallback() throws Exception {
        GameSession session = newSession();
        for (int i = 0; i < 3; i++) queue.add(res(true, false, null, false));
        remaining = 5;

        Dtos.ShotResponse body = (Dtos.ShotResponse) controller
                .receiveShots(session.getGameId(), validShots()).getBody();

        assertEquals("ONGOING", body.gameStatus);
        assertNull(body.winner);
        assertEquals(5, body.shipsRemaining);
        assertEquals(1, ai.calls);
        assertEquals(1, callback.calls);
        assertEquals(ai.shots, callback.received, "Erro: o callback devia receber os tiros da IA.");
        assertFalse(session.isGameOver());
    }

    @Test
    @DisplayName("Falha no callback: o turno do estudante termina na mesma com 200")
    void testCallbackFalha() throws Exception {
        GameSession session = newSession();
        for (int i = 0; i < 3; i++) queue.add(res(true, false, null, false));
        callback.ok = false;

        ResponseEntity<?> r = controller.receiveShots(session.getGameId(), validShots());

        assertEquals(200, r.getStatusCode().value());
        assertEquals(1, callback.calls);
        assertEquals("ONGOING", ((Dtos.ShotResponse) r.getBody()).gameStatus);
    }
}