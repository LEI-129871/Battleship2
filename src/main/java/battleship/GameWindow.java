package battleship;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Scanner;

/**
 * Representa a interface gráfica principal (GUI) do jogo Batalha Naval, desenvolvida em Swing.
 * <p>
 * Esta classe é responsável pela renderização do tabuleiro tático 10x10, gestão dos elementos visuais
 * (com estilo escuro e botões neon via FlatLaf), interação com a frota e processamento
 * dos disparos (rajadas) efetuados tanto na GUI como recebidos a partir do servidor web.
 * </p>
 *
 * @author Diogo [129869]
 * @version 1.0
 */
public class GameWindow {

    /** Dimensão da grelha do tabuleiro (10x10). */
    private static final int BOARD_SIZE = 10;

    /** Marcador visual de navio totalmente destruído. */
    private static final char SHIP_DEAD = 'C';

    /** Marcador visual de posição ocupada por um navio. */
    private static final char SHIP_MARKER = '#';

    /** Marcador visual de tiro certeiro num navio. */
    private static final char SHOT_SHIP_MARKER = '*';

    /** Marcador visual de tiro na água. */
    private static final char SHOT_WATER_MARKER = 'o';

    /** Marcador visual de posição adjacente a um navio afundado. */
    private static final char SHIP_ADJACENT_MARKER = '-';

    /** Marcador visual de célula vazia/não atingida. */
    private static final char EMPTY_MARKER = '.';

    /** Janela principal da aplicação. */
    private JFrame frame;

    /** Painel correspondente à grelha do tabuleiro de jogo. */
    private JPanel tabuleiro;

    /** Painel lateral contendo o menu de controlos e ações. */
    private JPanel menu;

    /** A frota de navios ativa do jogador. */
    private IFleet myFleet;

    /** Instância do motor de jogo que gere o estado da partida. */
    private IGame game;

    /** Getter do Game **/
    public IGame getGame() {
        return game;
    }

    /** Campo de texto para inserção das coordenadas dos tiros da rajada. */
    private JTextField fieldRajada;

    /** Botão para acionar a submissão da rajada de tiros. */
    private JButton rajada;

    /**
     * Obtém a referência para o botão de disparo de rajada.
     *
     * @return O componente {@link JButton} associado ao disparo.
     */
    public JButton getRajada() {
        return rajada;
    }

    /**
     * Obtém a referência para o campo de texto de inserção da rajada.
     *
     * @return O componente {@link JTextField} do campo de rajada.
     */
    public JTextField getFieldRajada() {
        return fieldRajada;
    }

    /** Matriz bidimensional que guarda o estado lógico de cada célula do tabuleiro. */
    private char[][] map = new char[BOARD_SIZE][BOARD_SIZE];

    /** Matriz de rótulos visuais (labels) para a representação gráfica do tabuleiro. */
    private JLabel[][] labelsMap = new JLabel[BOARD_SIZE][BOARD_SIZE];

    /** Indica se a frota atual já foi gerada e colocada no tabuleiro. */
    private boolean gerada = false;

    /**
     * Constrói a janela do jogo, inicializa os LookAndFeel personalizados (FlatLaf),
     * define as configurações de temas, dimensões e monta os componentes do tabuleiro e menu.
     */
    public GameWindow(){
        FlatLightLaf.setup();
        FlatJetBrainsMonoFont.install();
        UIManager.put("OptionPane.background", Color.decode("#0F0F0F"));
        UIManager.put("Panel.background", Color.decode("#0F2A38"));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("OptionPane.messageFont", new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 14));
        UIManager.put("Button.arc", 30);
        frame = new JFrame("BattleShip 2");
        frame.setSize(900,500);
        frame.setLayout(new BorderLayout());
        tabuleiro = new JPanel();
        menu = new JPanel();
        menu.setBackground(new Color(15,15,15));
        menu.setLayout(new MigLayout("wrap 1", "[grow, fill]",""));
        tabuleiro.setPreferredSize(new Dimension(500,500));
        menu.setPreferredSize(new Dimension(400,500));
        tabuleiro.setLayout(new MigLayout("wrap 10, gap 2, insets 2","[grow, fill, sg]","[grow, fill, sg]"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addComponentsTabuleiro();
        addComponentsMenu();

        frame.add(tabuleiro, BorderLayout.CENTER);
        frame.add(menu,BorderLayout.EAST);
    }

    /**
     * Centra a janela no ecrã e torna-a visível, dando início à interface do jogo.
     */
    public void start(){
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Cria e inicializa as 100 células (JLabels) que constituem a grelha visual do tabuleiro 10x10.
     */
    private void addComponentsTabuleiro(){
        for (int i = 0; i < 100; i++) {
            JLabel label = new JLabel("");
            label.setOpaque(true);
            label.setBackground(new Color(31, 78, 107));
            tabuleiro.add(label);
            labelsMap[i/10][i%10] = label;
        }
    }

    /**
     * Método auxiliar para instanciar e estilizar um {@link JButton} com estética Neon personalizada.
     *
     * @param texto O texto a ser exibido no botão.
     * @param corBorda A cor da borda do botão em formato Hexadecimal (ex: "#BC13FE").
     * @param corTexto A cor do texto em formato Hexadecimal.
     * @return O botão configurado com os estilos visuais aplicados.
     */
    private JButton botaoNeon(String texto, String corBorda, String corTexto) {
        JButton b = new JButton(texto);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 15));
        b.putClientProperty(FlatClientProperties.STYLE,
                "arc: 30;" +
                        "borderWidth: 2;" +
                        "background: #282828;" +
                        "hoverBackground: #333333;" +
                        "pressedBackground: #1E1E1E;" +
                        "foreground: " + corTexto + ";" +
                        "borderColor: " + corBorda + ";" +
                        "hoverBorderColor: " + corBorda + ";" +
                        "pressedBorderColor: " + corBorda + ";" +
                        "focusedBorderColor: " + corBorda + ";");
        return b;
    }

    /**
     * Atualiza a representação gráfica do tabuleiro no ecrã com base na matriz lógica
     * e no estado atual dos navios da frota (tiros na água, acertos e navios afundados).
     */
    private void updateBoard(){
        for (IShip ship : myFleet.getShips()) {
            if (!ship.stillFloating()) {
                for (IPosition ship_pos : ship.getPositions())
                    map[ship_pos.getRow()][ship_pos.getColumn()] = SHIP_DEAD;
                for (IPosition adjacent_pos : ship.getAdjacentPositions())
                    map[adjacent_pos.getRow()][adjacent_pos.getColumn()] = SHIP_ADJACENT_MARKER;
            }
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                switch (map[i][j]){
                    case SHOT_SHIP_MARKER -> {
                        labelsMap[i][j].setBackground(Color.decode("#6B3F1D"));
                        labelsMap[i][j].setForeground(Color.decode("#FF073A"));
                        labelsMap[i][j].setText("X");
                        labelsMap[i][j].setHorizontalAlignment(0);
                        labelsMap[i][j].setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 20));
                    }
                    case SHIP_MARKER -> labelsMap[i][j].setBackground(Color.decode("#A0693A"));
                    case SHIP_DEAD -> {
                        labelsMap[i][j].setBackground(Color.decode("#3E2412"));
                        labelsMap[i][j].setForeground(Color.decode("#FF073A"));
                        labelsMap[i][j].setText("X");
                        labelsMap[i][j].setHorizontalAlignment(0);
                        labelsMap[i][j].setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 20));
                    }
                    case SHOT_WATER_MARKER -> {
                        labelsMap[i][j].setBackground(Color.decode("#1F4E6B"));
                        labelsMap[i][j].setForeground(Color.decode("#BFE3F5"));
                        labelsMap[i][j].setText("•");
                        labelsMap[i][j].setHorizontalAlignment(0);
                        labelsMap[i][j].setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 20));
                    }
                    case SHIP_ADJACENT_MARKER -> {
                        labelsMap[i][j].setBackground(Color.decode("#173B52"));
                        labelsMap[i][j].setForeground(Color.decode("#BFE3F5"));
                        labelsMap[i][j].setText("•");
                        labelsMap[i][j].setHorizontalAlignment(0);
                        labelsMap[i][j].setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 20));
                    }

                    default -> {
                        labelsMap[i][j].setBackground(new Color(31, 78, 107));
                        labelsMap[i][j].setText("");
                    }
                }
            }
        }
    }

    /**
     * Verifica se a partida terminou (todos os navios afundados). Em caso afirmativo,
     * exibe uma caixa de diálogo informando o fim do jogo e permite reiniciar a partida ou sair.
     */
    private void checkwin(){
        if(game.getRemainingShips() > 0) return;

        String[] opcoes = {"Novo Jogo", "Fechar"};
        int escolha = JOptionPane.showOptionDialog(frame,
                "Fim do Jogo em " + game.getAlienMoves().size() + " Rajadas", "Fim",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);

        if (escolha == 0){
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    map[i][j] = EMPTY_MARKER;
                }
            }
            gerada = false;
            myFleet = new Fleet();
            game = null;
            updateBoard();
        }
        else System.exit(0);
    }

    /**
     * Cria e adiciona todos os componentes ao painel lateral do menu,
     * incluindo botões de controlo, campo de texto e respetivos listeners de eventos.
     */
    private void addComponentsMenu(){

        // Botão para gerar frota aleatória
        JButton geraFrota = botaoNeon("Gerar Frota","#BC13FE", "#FFFFFF");
        geraFrota.addActionListener(e->{
            if(gerada) return;
            myFleet = Fleet.createRandom();
            game = new Game(myFleet);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    map[i][j] = EMPTY_MARKER;
                }
            }
            for (IShip ship : myFleet.getShips()) {
                if (ship.stillFloating()) {
                    for (IPosition ship_pos : ship.getPositions())
                        map[ship_pos.getRow()][ship_pos.getColumn()] = SHIP_MARKER;
                }
            }
            updateBoard();
            gerada = true;

        });

        // Campo de texto para introdução da rajada
        fieldRajada = new JTextField();
        fieldRajada.setFont(new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, 15));
        fieldRajada.setHorizontalAlignment(JTextField.CENTER);
        fieldRajada.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ex: A1 A2 A3");
        fieldRajada.putClientProperty(FlatClientProperties.STYLE,
                "arc: 30;" +
                        "borderWidth: 2;" +
                        "background: #191919;" +
                        "foreground: #FFFFFF;" +
                        "caretColor: #00F0FF;" +
                        "borderColor: #00F0FF;" +
                        "focusedBorderColor: #00F0FF;" +
                        "margin: 4,12,4,12;");

        // Botão para submeter a rajada
        rajada = botaoNeon("Rajada","#FF5C00", "#FFFFFF");
        rajada.addActionListener(e->{
            if(game== null || fieldRajada.getText().isEmpty()) return;
            Scanner in = new Scanner(fieldRajada.getText());
            try {
                game.readEnemyFire(in);
            }
            catch (IllegalArgumentException i){
                JOptionPane.showMessageDialog(frame, i.getMessage(),
                        "ERRO na Rajada", JOptionPane.INFORMATION_MESSAGE);
            }Game.printBoardShots(game.getAlienMoves(),map);
            in.close();

            Game.printBoardShots(game.getAlienMoves(),map);

            updateBoard();
            checkwin();
        });

        // Botão para disparar uma rajada aleatória
        JButton rajadaRandom = botaoNeon("Rajada Aleatoria", "#39FF14", "#FFFFFF");
        rajadaRandom.addActionListener(e->{
            if(game== null) return;
            game.randomEnemyFire();
            Game.printBoardShots(game.getAlienMoves(),map);
            updateBoard();
            checkwin();
        });

        // Botão para desistir da partida
        JButton desistir  = botaoNeon("Desistir","#FF073A", "#FF073A");
        desistir.addActionListener(e->{
            String[] opcoes = {"Reiniciar", "Fechar"};
            int escolha = JOptionPane.showOptionDialog(frame,
                    "Queres mesmo desistir?", "Desistir",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, opcoes, opcoes[0]);

            if (escolha == 0){
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        map[i][j] = EMPTY_MARKER;
                    }
                }
                gerada = false;
                myFleet = new Fleet();
                game = null;
                updateBoard();
            }
            else System.exit(0);
        });

        menu.add(geraFrota, "h 50!, gapbottom 47, gaptop 30");
        menu.add(fieldRajada, "h 75!");
        menu.add(rajada,"h 50!");
        menu.add(rajadaRandom,"h 50!");
        menu.add(desistir, "h 50!,gaptop 47");
    }


    /**
     * Ponto de entrada principal para execução autónoma da interface gráfica.
     *
     * @param args Argumentos da linha de comandos (não utilizados).
     */
    public static void main(String[] args) {
        GameWindow g = new GameWindow();
        g.start();
    }
}