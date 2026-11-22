package restserver;

import battleship.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AiShotStrategyTest {

    private AiShotStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AiShotStrategy();
    }

    // ---------- auxiliares: jogo e jogadas simuladas ----------

    private static IGame game(List<IMove> moves) {
        return (IGame) Proxy.newProxyInstance(IGame.class.getClassLoader(), new Class<?>[]{IGame.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAlienMoves")) return moves;
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IMove move(List<IPosition> shots, List<IGame.ShotResult> results) {
        return (IMove) Proxy.newProxyInstance(IMove.class.getClassLoader(), new Class<?>[]{IMove.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getShots" -> shots;
                    case "getShotResults" -> results;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static List<IPosition> shots(int... rc) {
        List<IPosition> list = new ArrayList<>();
        for (int i = 0; i < rc.length; i += 2) list.add(new Position(rc[i], rc[i + 1]));
        return list;
    }

    private static IShip anyShip() {
        return new Barge(Compass.NORTH, new Position(0, 0));
    }

    /** Acerto num navio que ainda não afundou. */
    private static IGame.ShotResult hit() {
        return new IGame.ShotResult(true, false, anyShip(), false);
    }

    private static IGame.ShotResult miss() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private static void assertValidBurst(List<IPosition> result, int expectedSize) {
        assertEquals(expectedSize, result.size(), "Erro: número de tiros incorreto.");
        assertEquals(result.size(), new HashSet<>(result).size(), "Erro: há tiros repetidos na rajada.");
        for (IPosition p : result)
            assertTrue(p.isInside(), "Erro: tiro fora do tabuleiro: " + p);
    }

    // ---------- testes ----------

    @Test
    @DisplayName("Sem jogadas anteriores: 3 tiros distintos, no tabuleiro e no padrão xadrez")
    void testSemJogadasAnteriores() {
        List<IPosition> result = strategy.computeShots(game(List.of()));
        assertValidBurst(result, Game.NUMBER_SHOTS);
        for (IPosition p : result)
            assertEquals(0, (p.getRow() + p.getColumn()) % 2, "Erro: o tiro devia estar numa casa do padrão xadrez.");
    }

    @Test
    @DisplayName("Com acertos sem afundar: os tiros vão para casas vizinhas dos acertos")
    void testAlvoVizinhosDeAcertos() {
        IMove m = move(shots(5, 5, 7, 7), List.of(hit(), hit()));
        List<IPosition> result = strategy.computeShots(game(List.of(m)));
        assertValidBurst(result, Game.NUMBER_SHOTS);

        Set<IPosition> neighbours = new HashSet<>();
        neighbours.addAll(new Position(5, 5).adjacentPositions());
        neighbours.addAll(new Position(7, 7).adjacentPositions());
        assertTrue(neighbours.containsAll(result), "Erro: todos os tiros deviam ser vizinhos de um acerto.");
    }

    @Test
    @DisplayName("Acertos no canto: não repete tiros já feitos nem vizinhas já escolhidas")
    void testAlvoSemRepeticoes() {
        IMove m = move(shots(0, 0, 1, 1), List.of(hit(), hit()));
        List<IPosition> result = strategy.computeShots(game(List.of(m)));
        assertValidBurst(result, Game.NUMBER_SHOTS);
        assertFalse(result.contains(new Position(0, 0)), "Erro: repetiu o tiro em (0,0).");
        assertFalse(result.contains(new Position(1, 1)), "Erro: repetiu o tiro em (1,1).");
        assertTrue(result.contains(new Position(0, 1)), "Erro: devia atirar em (0,1).");
        assertTrue(result.contains(new Position(1, 0)), "Erro: devia atirar em (1,0).");
    }

    @Test
    @DisplayName("Poucos vizinhos livres: completa a rajada com tiros de caça (padrão xadrez)")
    void testAlvoEcacaCombinados() {
        IMove m = move(shots(0, 0, 0, 1, 1, 0), List.of(hit(), miss(), miss()));
        List<IPosition> result = strategy.computeShots(game(List.of(m)));
        assertValidBurst(result, Game.NUMBER_SHOTS);
        assertTrue(result.contains(new Position(1, 1)), "Erro: o único vizinho livre de (0,0) é (1,1).");
        for (IPosition p : result)
            if (!p.equals(new Position(1, 1)))
                assertEquals(0, (p.getRow() + p.getColumn()) % 2, "Erro: os restantes tiros são de caça.");
    }

    @Test
    @DisplayName("Resultados inválidos, repetidos, falhados, afundados ou em falta não geram alvos")
    void testResultadosQueNaoSaoAlvos() {
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(false, false, null, false),     // inválido
                new IGame.ShotResult(true, true, anyShip(), false),  // repetido
                miss(),                                              // água
                new IGame.ShotResult(true, false, anyShip(), true)); // afundou
        IMove m1 = move(shots(1, 1, 1, 3, 2, 2, 3, 3, 4, 4), results);   // 5 tiros, 4 resultados
        IMove m2 = move(shots(6, 6), List.of());                         // sem resultados

        List<IPosition> result = strategy.computeShots(game(List.of(m1, m2)));
        assertValidBurst(result, Game.NUMBER_SHOTS);

        Set<IPosition> already = new HashSet<>(shots(1, 1, 1, 3, 2, 2, 3, 3, 4, 4, 6, 6));
        for (IPosition p : result) {
            assertFalse(already.contains(p), "Erro: repetiu um tiro anterior: " + p);
            assertEquals(0, (p.getRow() + p.getColumn()) % 2, "Erro: sem alvos, só devia haver tiros de caça.");
        }
    }

    @Test
    @DisplayName("Padrão xadrez esgotado: o plano de recurso escolhe casas ainda não atingidas")
    void testPlanoDeRecurso() {
        List<IPosition> all = new ArrayList<>();
        for (int r = 0; r < Game.BOARD_SIZE; r++)
            for (int c = 0; c < Game.BOARD_SIZE; c++)
                if ((r + c) % 2 == 0) all.add(new Position(r, c));

        // o primeiro tiro (0,0) foi um acerto; as restantes casas pares já foram atingidas
        IMove m = move(all, List.of(hit()));
        List<IPosition> result = strategy.computeShots(game(List.of(m)));

        assertValidBurst(result, Game.NUMBER_SHOTS);
        assertEquals(Set.of(new Position(0, 1), new Position(1, 0), new Position(0, 3)), new HashSet<>(result),
                "Erro: esperavam-se os 2 vizinhos de (0,0) e a primeira casa livre.");
    }

    @Test
    @DisplayName("Tabuleiro todo atingido: não há tiros possíveis")
    void testTabuleiroCompleto() {
        List<IPosition> all = new ArrayList<>();
        for (int r = 0; r < Game.BOARD_SIZE; r++)
            for (int c = 0; c < Game.BOARD_SIZE; c++)
                all.add(new Position(r, c));

        List<IPosition> result = strategy.computeShots(game(List.of(move(all, List.of()))));
        assertTrue(result.isEmpty(), "Erro: sem casas livres a rajada devia estar vazia.");
    }
}