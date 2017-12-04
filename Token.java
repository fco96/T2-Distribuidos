import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//El token contiene 2 listas, 1 modela la lista LN del algoritmo S-K y Queue para la cola del siguiente proceso
//que le toca el token
public class Token implements Serializable{
    List<Integer> LN;
    List<Integer> Queue;

    public Token(int N) {
        LN = new ArrayList<>();
        Queue = new ArrayList<>();

        //Se inicializa la lista LN
        LN = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            LN.add(0);
        }

    }

    //Metodo para poder printear comodamente el Token
    @Override
    public String toString() {
        return "LN: "+LN+" Cola: "+Queue;
    }
}
