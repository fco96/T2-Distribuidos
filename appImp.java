import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class appImp extends UnicastRemoteObject implements app {
    //Socket que será ocupado para enviar paquetes al grupo multiCast
    DatagramSocket mcSocketOut;

    //Variables para indicar cuál es la ip y puerto del multiCast
    //En este caso se ocupará 230.0.0.1:4545
    InetAddress ipMulti;
    int puertoMulti;

    //Funcion que es invocada por un proceso y da cuenta que quiere hacer su sección crítica
    //Esta le envía a TODOS los procesos de la red un mensaje de la forma "request-<mi id>-<mi número de secuencia>"
    @Override
    public void request(int id, int seq) throws RemoteException {

        try {
            //Se procede a generar el mensaje
            String mensaje = "request-"+id + "-" + seq;

            //Se envia a los demas la petición
            DatagramPacket paqueteOut = new DatagramPacket(mensaje.getBytes(), mensaje.getBytes().length, ipMulti, puertoMulti);
            mcSocketOut.send(paqueteOut);

        }catch (IOException e){
            e.printStackTrace();
        }


    }

    //Función que hace que los procesos que quieren hacer su sección crítica y no tienen el token deban de esperar
    //Esta devuelve el token al proceso que le corresponda según el algoritmo de S-K (primero en la cola)
    @Override
    public Token waitToken(int id) throws RemoteException {
        try{
            byte[] buf = new byte[1000];
            //Cada proceso que espere va a abrir un socket en un puerto de la forma 500<id>, entonces así
            //se logra que se despierte el proceso indicado cuando le llegue un paquete en su socket
            DatagramSocket ucSocketIn = new DatagramSocket( 5000 + id );
            DatagramPacket paqueteIn = new DatagramPacket( buf, buf.length );

            /*El proceso se va a quedar esperando hasta que llegue el paquete con el token*/

            ucSocketIn.receive( paqueteIn );

            System.out.println("[App] WaitToken: Se ha recibido el token");
            //Empieza el proceso de "desempaquetar" el objeto que llegó en el paquete
            ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(buf));
            Token t = (Token) objIn.readObject();
            objIn.close();

            return t;

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //Función que sirve para quitarle el token a un proceso y hacerselo llegar a otro (primero de la cola)
    @Override
    public void takeToken(Token t) throws RemoteException {
        try{

            int siguiente = t.Queue.get(0);
            t.Queue.remove(0);

            //Se escribe el objeto
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(buffer);
            objOut.writeObject(t);
            objOut.close();

            //Se procede a armar el paquete y enviar el objeto
            byte[] bufMsg = buffer.toByteArray();
            DatagramSocket ucSocketOut = new DatagramSocket();
            DatagramPacket paqueteOut = new DatagramPacket( bufMsg, bufMsg.length, InetAddress.getByName( "127.0.0.1" ), siguiente+5000 );
            ucSocketOut.send( paqueteOut );
            System.out.println( "[App] Se ha enviado el token al proceso "+siguiente);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    //Función que sirve para terminar el servidor rmi
    @Override
    public void kill() throws RemoteException {
        try{
            Naming.unbind("//localhost:" + 8080 + "/app");
            UnicastRemoteObject.unexportObject(this, true);
        }catch (Exception e){
            e.printStackTrace();
        }
        //System.exit(0);

    }

    public appImp()throws RemoteException {
        super();
        try{
            //Se inicializa la ip y puertos multicast
            ipMulti = InetAddress.getByName("230.0.0.1");
            puertoMulti = 4545;

            //Se inicializa el socket por el cual se enviaran mensajes multicast
            mcSocketOut = new DatagramSocket();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void main(String[] args){
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            appImp aplicacion = new appImp();
            Naming.rebind("rmi://localhost:" + 8080 + "/app", aplicacion);
            System.out.println("Se ha bindeado la aplicación exitosamente!.");
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }


}