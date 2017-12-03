id=0
n=1
initialDelay=0
bearer=false

JAVAC=javac
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: clases

clases: $(classes)

cliente:
	java -Djava.security.policy=permisos cliente $(id) $(n) $(initialDelay) $(bearer)

clean :
	rm -f *.class

%.class : %.java
	$(JAVAC) $<
