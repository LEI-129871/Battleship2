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
			GameWindow gw = null;

            try {
                if (aux.contains("--gui")){
                    gw = new GameWindow();
                    gw.start();
                }
                if (aux.contains("--help") || aux.contains("-h")){
                    System.out.println("""
                        Uso: java -jar BattleshipGamePlayer-2.0.jar [OPÇÃO]...
                
                        Batalha naval dos Descobrimentos (BattleShip II).
                        Sem opções, arranca o menu de consola.
                
                        Opções:
                          --gui             abre a janela gráfica
                          --server          arranca o servidor HTTP que recebe rajadas
                          --port [1024-65535]  porta do servidor, 4444 default
                          -h, --help        mostra esta ajuda e termina
                
                        Exemplos:
                          java -jar BattleshipGamePlayer-2.0.jar
                          java -jar BattleshipGamePlayer-2.0.jar --gui
                          java -jar BattleshipGamePlayer-2.0.jar --gui --server
                    """);
					return;
                }
                if(aux.contains("--port")) {
	                port = Integer.parseInt(aux.get(aux.indexOf("--port") + 1));
					if(port > 65535 || port < 1024) {
						System.out.println("porta num range nao permitido!");
						throw new IllegalStateException();
					}
                }
                if (aux.contains("--server")){
                    if(!aux.contains("--gui")){
                        System.out.println("***  Battleship  ***");
                        Tasks.menu();
                    }
                    Server server = new Server();
					server.run(port,gw);
                }
            } catch (Exception e) {
				System.out.println("Erro inesperado corra com a flag --help ou -h para obter ajuda");
				return;
            }
        }
    }
}
