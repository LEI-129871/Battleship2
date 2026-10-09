package battleship;

import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class Time extends Thread{

    private StopWatch sw = new StopWatch();
    private int sec;
    private JFrame j;

    public Time(JFrame j, int sec){
        sw.start();
        this.sec = sec;
        this.j = j;
    }

    public double getTemporizador() throws InterruptedException {
        sw.suspend();
        double time = sec - sw.getTime(TimeUnit.SECONDS);
        if(time <= 0){
            String[] opcoes = {"Fechar"};
            int escolha = JOptionPane.showOptionDialog(j,
                    "fosses mais rapido AHAHAHAH", "És dos lentos tu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, opcoes, opcoes[0]);
                    sw.stop();
                    System.exit(0);

        }
        sw.resume();
        return time;
    }

    public void reiniciar() {
        sw.reset();
        sw.start();
    }

    public void run(){
        while(true){
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                j.setTitle("BattleShip2 " + getTemporizador() + "s") ;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
