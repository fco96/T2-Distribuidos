public class cliente {
    public static void main(String[] args){
        int id, n, delay;
        Boolean bearer;
        int sequenceNumber = 0;

        System.out.println("El largo de args es: "+args.length);

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

        System.out.println("El id vale: "+id+", el número máximo es: "+n+", el delay: "+delay+", el bearer: "+bearer);

        semaforo s = new semaforo(n, id);

        //Como soy el primero me quedo con el semaforo
        if (bearer){
            s.takeToken(s.t);
        }

        //Dar tiempo para que todos los clientes esten Online al mismo tiempo
        try{
            Thread.sleep(1000*10);
        }catch (InterruptedException e){
            e.printStackTrace();
        }


        sequenceNumber++;
        s.request(id, sequenceNumber);

        /*SE QUEDA ESPERANO HASTA QUE ME DEN EL TOKEN*/

        s.freeToken(s.t);

        System.out.println("El proceso de id "+id+"hace su zona crítica");

        System.out.println("Se cierra el cliente");
    }
}
