# Tarea 2, Sistemas distribuidos

Francisco Olivares 201473575-8 <br>
francisco.olivars.14@sansano.usm.cl

### Instrucciones de ejecución
1) Entre en la carpeta del proyecto y ejecute el siguiente comando
```
rmiregistry 8080 &
```

2) Escriba make, para que así se compilen las clases 
```
make
```

3) Ejecute una instancia del servidor rmi con el comando
```
make app
```
4) Abra los clientes que estime necesarios con el comando
```
make proceso id=<id del proceso> n=<numero total de procesos> delay=<retardo en ms del proceso> bearer=<true/false>
```
**Ejemplo** 
```
make proceso id=0 n=3 delay=5000 bearer=false
```

### Consideraciones y supuestos

* Todos los procesos están online cuando inicia el algoritmo, por esto se añadió un delay de 10 segundos, para que así se abran todas las intancias de procesos y después de eso empieza a operar el proceso
* Se modificó la firma original de la función ```waitToken()``` a ```waitToken(int id)```, esto se hizo con la finalidad de que el proceso que ejecute ```waitToken()``` se quede esperando (el token) y abra un socket Unicast en la dirección 5000 + id desde el lado del rmi
* Dentro del proyecto hay una carpeta llamada **logs**, en esta se guardan los logs de la ejecución de los procesos de la forma ``logP<id>``, se recomienda entre una ejecución y otra borrar los logs para tener una mejor noción de la ejecución actual. 

### Estrategia
* El arreglo **RN** es una lista que vive en los procesos
* El arreglo **LN** y la **Cola** viven en el Token.
* Los métodos ``request()``, ``waitToken``, ``takeToken`` y ``Kill`` fueron implementados en el objeto rmi **app**.
* Existe un único Token, este parte inicialmente en el proceso con el flag ``bearer=true`` , una vez este termine su zona crítica, hará todo el procedimiento de añadir nuevos procesos a la cola y esperará hasta que haya alguíen a quíen pasarle el token (alguien en la cola) y ejecutará el método ``takeToken``.
* Cada proceso tiene un Thread destinado a escuchar y procesar las request provenientes de los otros procesos, se aceptan o se rechazan según la regla del algoritmo.

Para más detalle se recomienda leer los comentarios del código.
