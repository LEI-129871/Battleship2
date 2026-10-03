/**
 * 
 */
package battleship;

import java.util.Arrays;
import java.util.List;

public class Main
{
	/**
	 * Main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("***  Battleship  ***");
			Tasks.menu();
		}
		else{
			int port = 4444;
			List<String> aux = Arrays.stream(args).toList();
            try {
                if (aux.contains("--gui")){
                    GameWindow gameWindow = new GameWindow();
                    gameWindow.start();
                }
                if (aux.contains("--help") || aux.contains("-h")){
                    System.out.println("""
                        Uso: java -jar BattleshipGamePlayer-2.0.jar [OPÇÃO]...
                
                        Batalha naval dos Descobrimentos (BattleShip II).
                        Sem opções, arranca o menu de consola.
                
                        Opções:
                          --gui         abre a janela gráfica
                          --server      arranca o servidor HTTP que recebe rajadas
                          --port X      porta do servidor, 4444 default
                          -h, --help    mostra esta ajuda e termina
                
                        Exemplos:
                          java -jar BattleshipGamePlayer-2.0.jar
                          java -jar BattleshipGamePlayer-2.0.jar --gui
                          java -jar BattleshipGamePlayer-2.0.jar --gui --server
                    """);
					return;
                }
                if(aux.contains("--port")) port = Integer.parseInt(aux.get(aux.indexOf("--port")+1));
                if (aux.contains("--server")){
                    //myServer server = new myServer();
                    if(!aux.contains("--gui")){
                        System.out.println("***  Battleship  ***");
                        Tasks.menu();
                    }
                }
            } catch (Exception e) {
				System.out.println("Erro inesperado corra com a flag --help ou -h para obter ajuda");
				return;
            }
        }
    }
}
