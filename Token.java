import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public String toString() {
        return "LN: "+LN+" Cola: "+Queue;
    }
}
