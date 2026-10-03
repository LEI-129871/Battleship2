package battleship;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.List;

public class Server {
    public void run(int port, GameWindow gw){
        var app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.routes.post("/rajada", ctx -> {
                String tiros = "";
                for(String str : ctx.formParams("tiro")){
                    tiros = tiros + str +  " ";
                }
                gw.getFieldRajada().setText(tiros);
                gw.getRajada().doClick();
                ctx.redirect("/?result="+gw.shotsResult());
            });
        }).start(port);
    }
}
