import java.rmi.Remote;
import java.rmi.RemoteException;

public interface app extends Remote{
    void request(int id, int seq) throws RemoteException;
    Token waitToken(int id) throws RemoteException;
    void takeToken(Token t) throws RemoteException;
    void kill() throws RemoteException;
}
