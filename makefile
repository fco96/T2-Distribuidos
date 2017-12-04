id=0
n=1
initialDelay=0
bearer=false

JAVAC=javac
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: clases

clases: $(classes)

proceso:
	java -Djava.security.policy=permisos Proceso $(id) $(n) $(initialDelay) $(bearer)

app:
	java -Djava.security.policy=permisos appImp

clean :
	rm -f *.class

%.class : %.java
	$(JAVAC) $<
