package battleship;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Scanner;

public class GameWindow {

    private static final int BOARD_SIZE = 10;
    private static final char SHIP_DEAD = 'C';
    private static final char SHIP_MARKER = '#';
    private static final char SHOT_SHIP_MARKER = '*';
    private static final char SHOT_WATER_MARKER = 'o';
    private static final char SHIP_ADJACENT_MARKER = '-';
    private static final char EMPTY_MARKER = '.';

    private JFrame frame;
    private JPanel tabuleiro;
    private JPanel menu;
    private IFleet myFleet;
    private IGame game;
    private JTextField fieldRajada;
    private JButton rajada;

    public JButton getRajada() {
        return rajada;
    }

    public JTextField getFieldRajada() {
        return fieldRajada;
    }

    private char[][] map = new char[BOARD_SIZE][BOARD_SIZE];
    private JLabel[][] labelsMap = new JLabel[BOARD_SIZE][BOARD_SIZE];
    private boolean gerada = false;

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

    public void start(){
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addComponentsTabuleiro(){
        for (int i = 0; i < 100; i++) {
            JLabel label = new JLabel("");
            label.setOpaque(true);
            label.setBackground(new Color(31, 78, 107));
            tabuleiro.add(label);
            labelsMap[i/10][i%10] = label;
        }
    }

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

    private void addComponentsMenu(){

        // Estetica geraFrota
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
        JButton rajadaRandom = botaoNeon("Rajada Aleatoria", "#39FF14", "#FFFFFF");
        rajadaRandom.addActionListener(e->{
            if(game== null) return;
            game.randomEnemyFire();
            Game.printBoardShots(game.getAlienMoves(),map);
            updateBoard();
            checkwin();
        });

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

    public String shotsResult(){
        StringBuilder aux = new StringBuilder();
        String sep = "|";
        for (int i = 0; i < 3; i++) {
            IGame.ShotResult shot = game.getAlienMoves().getLast().getShotResults().get(i);
            IPosition pos = game.getAlienMoves().getLast().getShots().get(i);
            if (shot.ship() == null) {
                aux.append("null").append(",").append(pos.toString()).append(sep);
            }
            else {
                String out = shot.ship().toString().split("\\[")[1];
                aux.append(out.split(" ")[0]).append(",").append(pos.toString()).append(",").append(shot.sunk()).append(sep);
            }
        }
        return aux.toString();
    }

    public static void main(String[] args) {
        GameWindow g = new GameWindow();
        g.start();
    }
}
