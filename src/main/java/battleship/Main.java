/**
 * 
 */
package battleship;

import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

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
				if (aux.contains("--gui")){
					gw = new GameWindow();
					gw.start();
				}
                if(aux.contains("--port")) {
	                port = Integer.parseInt(aux.get(aux.indexOf("--port") + 1));
					if(port > 65535 || port < 1024) {
						System.err.println("porta num range nao permitido!");
						throw new IllegalStateException();
					}
                }
                if (aux.contains("--server")){
					final GameWindow GameWindowFinal = gw;
					final int serverPort = port;
                    Server server = new Server();
                    if(!aux.contains("--gui")){
                        System.out.println("***  Battleship  ***");
						Thread srvr = new Thread(() -> server.run(serverPort,GameWindowFinal,Tasks.game,true));
						srvr.start();
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
                        Tasks.menu();
						server.stop();
                    }
					else {
						server.run(port, gw, Tasks.game, false);
                    }
                }
            } catch (Exception e) {
				System.out.println("Erro inesperado corra com a flag --help ou -h para obter ajuda");
				return;
            }
        }
    }
}
