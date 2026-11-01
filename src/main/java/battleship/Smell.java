// 129869 - Diogo Gonçalves
package battleship;

/**
 * Versão corrigida do Long Method.
 * A repetição foi substituída por um ciclo.
 */
public class Smell {

    public int longMethod() {
        int total = 0;

        for (int i = 1; i <= 70; i++) {
            total += i;
        }

        return total;
    }
}
