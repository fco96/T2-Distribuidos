import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;

public class Proceso {
    static int id, n, delay;
    static List<Integer> RN = new ArrayList<>();
    static Boolean bearer;
    static int sequenceNumber = 0;
    public static void main(String[] args){
        Token t = null;
        //Se parsean los elementos de entrada
        id = Integer.parseInt(args[0]);
        n = Integer.parseInt(args[1]);
        delay = Integer.parseInt(args[2]);
        if (args[3].equals("true")){
            bearer = true;
        }
        else{
            bearer = false;
        }

        System.out.println("Estado Inicial: id: "+id+", n: "+n+", delay: "+delay+", bearer: "+bearer);

        //Se inicializa la lista RN
        for(int i=0; i<n; i++){
            RN.add(0);
        }

        try{
            //Se procede a pedir el token desde el RMI
            app aplicacion = (app) Naming.lookup("//localhost:" + 8080 + "/app");

            //Se procede a generar el thread que escucha las peticiones multicast
            Thread threadIn = new Thread(new inChannel());
            threadIn.start();

            //Se procede a dar tiempo a los procesos para que todos esten Online
            Thread.sleep(1000*7);

            //Caso en el que tengo el token, entonces debo de mandar la petición e iniciar
            if (bearer){
                t = new Token(n);
                aplicacion.request(id, ++sequenceNumber);
            }

            //Caso en el que no parto con el token
            if (!bearer){
                aplicacion.request(id, ++sequenceNumber);
                t = aplicacion.waitToken(id);
            }

            /*Se comienzan a realizar cosas de la zona crítica*/


            System.out.println("[Proceso "+id+"] Saliendo de la zona crítica.");

            /*Empieza el proceso de modificar LN, la cola y ver a quién le voy a mandar el token*/

            //Después de que termina la zona crítica, hay que actualizar que se le realizó la petición al proceso que tenía el token
            //t.LN.set(id, RN.get(id));
            t.LN.set(id, sequenceNumber);

            //Ahora hay que recorrer LN y RN para así encolar a los procesos con peticiones pendientes que aun no son considerados
            for(int i=0; i<t.LN.size() ; i++){
                //Caso en que puede haber una petición pendiente
                if (RN.get(i) == t.LN.get(i) + 1){
                    //solo si no esta en la cola hay que añadirlo
                    if (!t.Queue.contains(i)){
                        t.Queue.add(i);
                    }

                }
            }

            //Finalmente ahora debo de ver quién esta en el frente de la cola, para así enviarle el token
            int siguiente;
            System.out.println("[Proceso "+id+"] Estado RN: "+RN+" "+t);

            Boolean termino = true;
            for(int i=0; i<t.LN.size(); i++){
                if (t.LN.get(i) == 0){
                    termino=false;
                }
            }

            if (termino){
                System.out.println("[Proceso "+id+"] Fin de la ejecución del algoritmo.");
            }

            else{
                //Si es que no hay nadie en la cola voy a retener el token, verificaré cada 1 segundo para mandarlo
                while (t.Queue.size() == 0){
                    Thread.sleep(1000*1);
                    //Voy a ver las solicitudes en curso
                    for(int i=0; i<t.LN.size() ; i++){
                        //Caso en que puede haber una petición pendiente
                        if (RN.get(i) == t.LN.get(i) + 1){
                            //solo si no esta en la cola hay que añadirlo
                            if (!t.Queue.contains(i)){
                                t.Queue.add(i);
                            }

                        }
                    }
                }

                siguiente = t.Queue.get(0);

                System.out.println("[Proceso "+id+"] Le enviaré el token a: "+siguiente);

                aplicacion.takeToken(t);
            }

            System.out.println("[Proceso "+id+"] Fin de la ejecución.");

        }catch (Exception e){
            e.printStackTrace();
        }






    }

    public static class inChannel implements Runnable{

        @Override
        public void run() {
            try {
                MulticastSocket mcSocketIn;
                InetAddress ipMulti;
                int puertoMulti;

                //Se inicializa la ip y puertos multicast
                ipMulti = InetAddress.getByName("230.0.0.1");
                puertoMulti = 4545;

                //Se inicializa el socket multi cast para recibir mensajes
                mcSocketIn = new MulticastSocket(4545);
                mcSocketIn.joinGroup(ipMulti);

                //Se crea un buffer
                byte[] buf = new byte[256];

                while(true){
                    //Se crea un paquete el cual leera el contenido del multicast
                    DatagramPacket paqueteIn = new DatagramPacket(buf, buf.length);
                    mcSocketIn.receive(paqueteIn);

                    /*SE PROCEDE A ESPERAR HASTA QUE LLEGUE ALGÚN MENSAJE*/

                    //Se empieza a procesar el mensaje
                    String received = new String(paqueteIn.getData(), paqueteIn.getOffset(), paqueteIn.getLength());

                    //Procesamiento de peticiones según susuki kasami
                    //Solo se acepta si seq > RN[id]
                    String[] info = received.split("-");

                    if (info[0].equals("request")){
                        int idIn =  Integer.parseInt(info[1]);
                        int seq = Integer.parseInt(info[2]);

                        if (seq > RN.get(idIn)){
                            RN.set(idIn, seq);
                            System.out.println("[Proceso "+id+"] Petición aceptada.");
                            System.out.println("RN:"+RN);
                        }
                        else{
                            System.out.println("[Proceso "+id+"] Petición rechazada.");
                        }
                    }

                }

            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}