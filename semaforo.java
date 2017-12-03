import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;

public class semaforo {
    static int id_proceso;
    String color;
    token t;
    static MulticastSocket mcSocketIn, mcSocketToken;
    static DatagramSocket mcSocketOut, mcSocketOut2;

    InetAddress ipMulti;
    int puertoMulti;

    static List<Integer> RN;

    public semaforo(int N, int id){
        id_proceso = id;
        RN = new ArrayList<>();
        for(int i=0; i<N; i++){
            RN.add(0);
        }
        this.color = "verde";
        try {
            //Se procede a pedir el token desde el RMI
            this.t = (token) Naming.lookup("//localhost:" + 8080 + "/token");

            //Se inicializa la ip y puertos multicast
            ipMulti = InetAddress.getByName("230.0.0.1");
            puertoMulti = 4545;

            //Se inicializa el socket multi cast para recibir mensajes
            mcSocketIn = new MulticastSocket(4545);
            mcSocketIn.joinGroup(ipMulti);

            //Se inicializa el socket multicast por el cual se esperará la señal de que se tiene el token
            mcSocketToken = new MulticastSocket(3434);
            mcSocketToken.joinGroup(InetAddress.getByName("230.0.0.2"));

            //Se inicializa el socket por el cual se mandarán los request
            mcSocketOut = new DatagramSocket();
            mcSocketOut2 = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Se procede a generar el thread que escucha las peticiones multicast
        Thread threadIn = new Thread(new inChannel());
        threadIn.start();

    }

    void request(int id, int seq){
        try {
            //Procedo a generar el mensaje a todos sobre el request
            String mensaje = "request-"+id + "-" + seq;

            //Se envia a los demas la petición
            DatagramPacket paqueteOut = new DatagramPacket(mensaje.getBytes(), mensaje.getBytes().length, ipMulti, puertoMulti);
            mcSocketOut.send(paqueteOut);

            //Si yo no tengo el token, entonces, debo de esperar, esto sirve para el primer proceso
            if (t.estado()!=id_proceso){
                waitToken();
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    void waitToken(){
        Boolean deboSeguir = true;
        while (deboSeguir){
            try{
                byte[] buf = new byte[256];
                DatagramPacket paqueteIn = new DatagramPacket(buf, buf.length);
                System.out.println("El proceso espera un mensaje ");
                mcSocketToken.receive(paqueteIn);

                System.out.println("Al proceso le llega respuesta");

                //Se empieza a procesar el mensaje
                String received = new String(paqueteIn.getData(), paqueteIn.getOffset(), paqueteIn.getLength());
                System.out.println("Posible permiso para obtener el token");
                System.out.println(received);

                //Caso en el que efectivamente me toca entrar a la zona crítica
                if (Integer.parseInt(received) == id_proceso){
                    System.out.println("El proceso "+id_proceso+" procede a tomar el token");
                    deboSeguir = false;
                }
                if (deboSeguir==true){
                    System.out.println("Se va a realizar denuevo el loop");
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
        //Se procede a ejecutar la toma del token
        takeToken(t);
    }

    void takeToken(token t){
        try{
            t.tomar(id_proceso);
        }catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    int freeToken(token t){
        int respuesta = -1;
        try{
            respuesta = t.liberar(RN);
            System.out.println("[Cliente "+id_proceso+"] La respuesta al liberar es: "+respuesta);
        }catch (RemoteException e) {
            e.printStackTrace();
        }
        /*caso en el que aun retengo el token*/
        if (respuesta == -1){
            System.out.println("[Liberar token] Caso en el que no hay nadie a quien avisar");
            return 0;

        }
        //Caso en el que le doy aviso al proceso que puede entrar
        else{
            try {
                //Procedo a generar el mensaje a todos sobre el request
                String mensaje = respuesta+"";
                System.out.println("[Liberar token] Voy a mandar el siguiente mensaje: "+mensaje);
                //Se envia a los demas la petición
                DatagramPacket paqueteOut = new DatagramPacket(mensaje.getBytes(), mensaje.getBytes().length, InetAddress.getByName("230.0.0.2"), 3434);
                mcSocketOut2.send(paqueteOut);

            }catch (IOException e){
                e.printStackTrace();
            }

        }
        return 1;
    }

    public static class inChannel implements Runnable{

        @Override
        public void run() {
            try {
                //Se crea un buffer
                byte[] buf = new byte[256];

                while(true){
                    //Se crea un paquete el cual leera el contenido del multicast
                    DatagramPacket paqueteIn = new DatagramPacket(buf, buf.length);
                    mcSocketIn.receive(paqueteIn);

                    /*SE PROCEDE A ESPERAR HASTA QUE LLEGUE ALGÚN MENSAJE*/

                    //Se empieza a procesar el mensaje
                    String received = new String(paqueteIn.getData(), paqueteIn.getOffset(), paqueteIn.getLength());
                    System.out.println(received);

                    //Procesamiento de peticiones según susuki kasami
                    //Solo se acepta si seq > RN[id]
                    String[] info = received.split("-");

                    if (info[0].equals("request")){
                        int id =  Integer.parseInt(info[1]);
                        int seq = Integer.parseInt(info[2]);

                        if (seq > RN.get(id)){
                            RN.set(id, seq);
                            System.out.println("[Semaforo "+id_proceso+"] Petición aceptada");
                            System.out.println("RN:"+RN);
                        }
                        else{
                            System.out.println("Petición rechazada");
                        }
                    }

                }

            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
