import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface token extends Remote {
    void tomar(int id) throws RemoteException;
    int estado() throws RemoteException;
    int liberar(List<Integer> RN) throws RemoteException;
}
