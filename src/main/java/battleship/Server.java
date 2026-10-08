package battleship;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.List;

/**
 * Representa o servidor web HTTP para o jogo Batalha Naval, utilizando a biblioteca Javalin.
 * <p>
 * Esta classe é responsável por disponibilizar os ficheiros estáticos da interface web
 * e processar os pedidos HTTP enviados a partir do browser para interagir com a janela principal do jogo.
 * </p>
 *
 * @author Diogo [129869]
 * @version 1.0
 */
public class Server {

    /**
     * Inicializa e arranca o servidor web Javalin na porta especificada, configurando
     * as rotas HTTP e a gestão de ficheiros estáticos.
     *
     * @param port O número da porta em que o servidor HTTP vai escutar (ex: 8080).
     * @param gw A instância de {@link GameWindow} associada à interface gráfica do jogo,
     *           utilizada para aplicar as ações recebidas via HTTP.
     */
    public void run(int port, GameWindow gw){
        var app = Javalin.create(config -> {
            // Configura a pasta de ficheiros estáticos (HTML/CSS/JS) no classpath
            config.staticFiles.add("/public", Location.CLASSPATH);

            /*
             * Rota POST "/rajada" - Processa o envio de uma rajada de tiros por parte do cliente web.
             * Extrai os parâmetros do formulário, atualiza a interface gráfica do jogo
             * e redireciona o cliente com o resultado dos disparos.
             */
            config.routes.post("/rajada", ctx -> {
                String tiros = "";
                for(String str : ctx.formParams("tiro")){
                    tiros = tiros + str + " ";
                }

                // Atualiza o campo de rajada na janela do jogo e simula o clique de disparo
                gw.getFieldRajada().setText(tiros);
                gw.getRajada().doClick();

                // Redireciona a página web com os resultados acumulados dos tiros
                ctx.redirect("/?result=" + gw.shotsResult());
            });
        }).start(port);
    }
}