import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Proceso {
    static int id, n, delay;
    static List<Integer> RN = new ArrayList<>();
    static Boolean bearer;
    static int sequenceNumber = 0;
    static Log myLog = null;
    public static void main(String[] args){



        Token t = null;
        String color = "Verde";
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


        //Se inicializa la lista RN
        for(int i=0; i<n; i++){
            RN.add(0);
        }

        //Se inicializa el log
        try {
            myLog = new Log("./logs/logP"+id);
        }catch (Exception e){
            e.printStackTrace();
        }

        myLog.logger.info("Estado Inicial: id: "+id+", n: "+n+", delay: "+delay+", bearer: "+bearer);
        myLog.logger.info("[Proceso "+id+"] Color del semaforo: "+color);

        myLog.logger.setLevel(Level.INFO);

        try{
            //Se procede a pedir el token desde el RMI
            app aplicacion = (app) Naming.lookup("//localhost:" + 8080 + "/app");

            //Se procede a generar el thread que escucha las peticiones multicast
            Thread threadIn = new Thread(new inChannel());
            threadIn.start();

            //Se procede a dar tiempo a los procesos para que todos esten Online
            Thread.sleep(1000*10);

            /*Desde aquí en adelante comienza el intento de entrar en la zona crítica*/

            Thread.sleep(delay);

            //Caso en el que tengo el token, entonces debo de mandar la petición e iniciar
            if (bearer){
                t = new Token(n);
                aplicacion.request(id, ++sequenceNumber);
            }

            //Caso en el que no parto con el token
            if (!bearer){
                aplicacion.request(id, ++sequenceNumber);
                color = "Amarillo";
                myLog.logger.info("[Proceso "+id+"] Color del semaforo: "+color);
                myLog.logger.info("[Proceso "+id+"] Se espera el token");
                t = aplicacion.waitToken(id);
                myLog.logger.info("[Proceso "+id+"] Token recibido");
            }

            //Le doy 1 segundo para que sincronize mensajes que vengan en curso antes de entrar a la zona crítica
            Thread.sleep(1000*1);
            color = "Rojo";
            myLog.logger.info("[Proceso "+id+"] Color del semaforo: "+color);
            myLog.logger.info("[Proceso "+id+"] Entrando en la zona crítica.");

            /*Se comienzan a realizar cosas de la zona crítica*/

            myLog.logger.info("[Proceso "+id+"] Saliendo de la zona crítica.");

            color = "Verde";
            myLog.logger.info("[Proceso "+id+"] Color del semaforo: "+color);

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
            myLog.logger.info("[Proceso "+id+"] Estado RN: "+RN+" "+t);

            //Antes de verificar cómo esta la cola, voy a ver si ya terminó el algoritmo, es decir todos pasaron
            //1 vez satisfactoriamente por la ruta crítica
            Boolean termino = true;
            for(int i=0; i<t.LN.size(); i++){
                if (t.LN.get(i) == 0){
                    termino=false;
                }
            }

            //En caso de que yo soy el último proceso debo de terminar el algoritmo
            if (termino){
                myLog.logger.info("[Proceso "+id+"] Fin de la ejecución del algoritmo.");
                aplicacion.kill();
            }

            //Si es que no soy el último debo pasar el token al siguiente que lo requiera
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

                myLog.logger.info("[Proceso "+id+"] Le enviaré el token a: "+siguiente);

                aplicacion.takeToken(t);
                t = null;
            }
            //Como se va a salir mato el thread que esta esuchando en el multiccast y después salgo con exit
            threadIn.stop();
            myLog.logger.info("[Proceso "+id+"] Fin de la ejecución.");
            System.exit(0);

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
                            myLog.logger.info("[Proceso "+id+"] Petición aceptada, RN:"+RN);
                        }
                        else{
                            myLog.logger.info("[Proceso "+id+"] Petición rechazada.");
                        }
                    }

                }

            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
