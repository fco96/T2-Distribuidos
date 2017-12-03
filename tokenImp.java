import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class tokenImp extends UnicastRemoteObject implements token {
    int pertenencia;
    List<Integer> LN;
    List<Integer> Queue;

    @Override
    public int estado() throws RemoteException {
        return pertenencia;
    }

    @Override
    public int liberar(List<Integer> RN) throws RemoteException {
        /*Aquí hay que hacer lo relacionado con antes de liberar el toquen, ver la cola, etc*/

        //Después de que termina la zona crítica, hay que actualizar que se le realizó la petición al proceso que tenía el token
        LN.set(pertenencia, RN.get(pertenencia));

        //Ahora hay que recorrer la cola para re-asignar el token

        for(int i=0; i<LN.size() ; i++){
            //Caso en que puede haber una petición pendiente
            if (RN.get(i) == LN.get(i) + 1){
                //solo si no esta en la cola hay que añadirlo
                if (!Queue.contains(i)){
                    Queue.add(i);
                }

            }
        }
        int siguiente;
        //Ahora se debe proceder a darle el token a la siguiente persona disponible
        try{
            siguiente = Queue.get(0);
        }catch (IndexOutOfBoundsException e){
            siguiente = -1;
        }

        if (siguiente==-1){
            System.out.println("No hay alguien a quíen entregarle el token");
        }

        //Se procede a avisarle que puede ocupar el toquen
        else{
            Queue.remove(0);
        }
        System.out.println("El siguiente a ejecutarse es: "+siguiente);
        return siguiente;
    }

    @Override
    public void tomar(int id) throws RemoteException {
        pertenencia = id;
    }

    public tokenImp(int N) throws RemoteException {
        super();
        this.pertenencia = -1;
        //Se inicializa la lista LN
        LN = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            LN.add(0);
        }

        //Se inicializa la cola
        Queue = new ArrayList<>();

    }

    public static void main(String[] args){
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            //Se debe de inicializar el número de procesos
            tokenImp t = new tokenImp(Integer.parseInt(args[0]));
            Naming.rebind("rmi://localhost:" + 8080 + "/token", t);
            System.out.println("Se ha bindeado el token exitosamente!.");
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
